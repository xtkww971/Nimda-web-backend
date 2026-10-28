package judgeServer.domain.challenge.mq.consumer;

import judgeServer.config.CtfQueueProperties;
import judgeServer.config.CtfTtlProperties;
import judgeServer.domain.challenge.download.ChallengeDownloadUrlCache;
import judgeServer.domain.challenge.instance.Repository.InstanceResultStore;
import judgeServer.domain.challenge.mq.message.CtfResultMessage;
import judgeServer.domain.challenge.mq.message.RequestStatus;
import judgeServer.domain.challenge.mq.producer.RedisCtfRequestProducer;
import judgeServer.domain.challenge.mq.stream.StreamResultConsumer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 조율자(Go)가 결과 스트림에 발행한 것을 소비해서 uuid 키로 Redis에 잠깐 저장한다.
 * 요청했던 사용자가 그 uuid로 결과를 조회(폴링)하거나, 서브도메인 프록시가 접속 대상을
 * 찾을 때 쓴다.
 *
 * <p>예전에는 인스턴스 결과와 다운로드 결과를 각각 소비하는 컨슈머가 따로 있었다.
 * 결과 스트림이 하나로 합쳐지면서 이 하나만 남았고, 무엇이 왔는지는 채워진 필드로 구분한다.
 */
@Component
public class CtfResultConsumer extends StreamResultConsumer<CtfResultMessage> {

    /** 사용자가 결과를 조회할 때 쓰는 키 접두사: ctf:result:{uuid} */
    public static final String RESULT_KEY_PREFIX = "ctf:result:";

    private final CtfQueueProperties props;
    private final CtfTtlProperties ttlProperties;
    private final ChallengeDownloadUrlCache urlCache;
    private final RedisCtfRequestProducer producer;
    private final InstanceResultStore instanceResultStore;

    public CtfResultConsumer(RedisConnectionFactory connectionFactory,
                             StringRedisTemplate redisTemplate,
                             CtfQueueProperties props,
                             CtfTtlProperties ttlProperties,
                             ChallengeDownloadUrlCache urlCache,
                             RedisCtfRequestProducer producer,
                             InstanceResultStore instanceResultStore) {
        super(connectionFactory, redisTemplate);
        this.props = props;
        this.ttlProperties = ttlProperties;
        this.urlCache = urlCache;
        this.producer = producer;
        this.instanceResultStore = instanceResultStore;
    }

    @Override
    protected String streamKey() {
        return props.getResultStreamKey();
    }

    @Override
    protected String consumerGroup() {
        return props.getResultConsumerGroup();
    }

    @Override
    protected String resultKeyPrefix() {
        return RESULT_KEY_PREFIX;
    }

    @Override
    protected Duration resultTtl() {
        return ttlProperties.getResult();
    }

    @Override
    protected CtfResultMessage parse(Map<String, String> fields) {
        return CtfResultMessage.fromStreamFields(fields);
    }

    @Override
    protected String requestIdOf(CtfResultMessage result) {
        return result.getUuid();
    }

    /**
     * 첨부파일 링크는 문제 단위로 캐시해 재사용한다. 그런데 결과에는 challengeCode가 없다
     * (식별자가 uuid 하나로 줄었다). 그래서 발행할 때 남겨둔 uuid → challengeCode 매핑으로
     * 되짚는다. 매핑이 만료됐으면 캐시만 건너뛴다 — 결과 자체는 이미 저장됐다.
     */
    @Override
    protected void afterStored(CtfResultMessage result, Map<String, String> fields) {
        if (result.getStatus() != RequestStatus.READY) {
            return;
        }
        if (!result.isDownload()) {
            extendToInstanceExpiry(result);
            return;
        }
        String challengeCode = producer.findChallengeCode(result.getUuid());
        if (challengeCode == null) {
            log.warn("다운로드 결과의 문제를 찾지 못해 링크 캐시를 건너뜁니다: uuid={}", result.getUuid());
            return;
        }
        urlCache.put(challengeCode, result.getDownloadUrl(), result.getExpiresAt());
    }

    /**
     * 인스턴스 결과는 컨테이너가 끝나는 시각(expiresAt)까지 남긴다.
     *
     * <p>기본 보관 시간(결과 10분, 소유자 30분)은 인스턴스 수명(1시간)보다 짧다. 그대로 두면 컨테이너는
     * 살아 있는데 10분 뒤 프록시가 결과를 못 찾아 503, 30분 뒤엔 소유권을 확인 못 해 403을 낸다.
     * 결과·소유자·사용자 매핑을 같은 시각에 끝내야 "결과는 있는데 주인을 모르는" 상태가 안 생긴다.
     *
     * <p>expiresAt이 없거나 이미 지났으면 기본 보관 시간을 그대로 둔다(Judge의 Existing 응답 등).
     */
    private void extendToInstanceExpiry(CtfResultMessage result) {
        Instant until = parseExpiresAt(result.getExpiresAt());
        if (until == null || !until.isAfter(Instant.now())) {
            return;
        }
        String uuid = result.getUuid();
        redisTemplate.expireAt(RESULT_KEY_PREFIX + uuid, until);
        producer.expireOwnerAt(uuid, until);

        Long userId = producer.findUserId(uuid);
        Long challengeId = producer.findChallengeId(uuid);
        if (userId != null && challengeId != null) {
            instanceResultStore.expireUserChallengeMappingAt(userId, challengeId, until);
        }
    }

    private Instant parseExpiresAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            log.warn("인스턴스 만료 시각을 읽지 못해 기본 보관 시간을 씁니다: expiresAt={}", raw);
            return null;
        }
    }

    @Override
    protected String logSummary(CtfResultMessage result) {
        // 서명된 URL은 그 자체가 접근 권한이라 로그에 남기지 않는다.
        return String.format("uuid=%s, status=%s, host=%s, port=%s, download=%s",
                result.getUuid(), result.getStatus(), result.getHost(), result.getPort(),
                result.isDownload());
    }

    @Override
    protected String resultName() {
        return "CTF 결과";
    }
}
