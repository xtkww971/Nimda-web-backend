package judgeServer.domain.challenge.mq.producer;

import judgeServer.config.CtfQueueProperties;
import judgeServer.config.CtfTtlProperties;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.mq.message.CtfRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCtfRequestProducer implements CtfRequestProducer {

    /** uuid → {userId, challengeCode} 매핑 키 접두사. */
    public static final String OWNER_KEY_PREFIX = "ctf:request:owner:";

    private final StringRedisTemplate redisTemplate;
    private final CtfQueueProperties queueProperties;
    private final CtfTtlProperties ttlProperties;

    @Override
    public String request(Challenge challenge, Long userId, String uuid) {
        // 메시지를 먼저 만든다. 허용되지 않는 조합이면 여기서 예외가 나고, 매핑을 남기지 않는다.
        Map<String, String> fields = CtfRequestMessage.of(challenge, uuid).toStreamFields();

        // 매핑을 먼저 남긴다. 발행 뒤에 남기면 그 사이에 결과가 도착해 매핑을 못 찾을 수 있다.
        Map<String, String> owner = new LinkedHashMap<>();
        owner.put("userId", String.valueOf(userId));
        owner.put("challengeCode", challenge.getCode());
        // 결과를 받았을 때 userId:challengeId → uuid 매핑을 인스턴스 수명까지 늘리려면 challengeId가 필요하다.
        owner.put("challengeId", String.valueOf(challenge.getId()));
        // 같은 uuid라도 WEB은 프록시로, PWN은 공개 host:port로 붙는다. 결과에는 category가 없어서 여기 남긴다.
        owner.put("challengeCategory", challenge.getCategory().name());
        String ownerKey = OWNER_KEY_PREFIX + uuid;
        redisTemplate.opsForHash().putAll(ownerKey, owner);
        redisTemplate.expire(ownerKey, ttlProperties.getOwner());

        /*      CtfRequestMessage = {
                uuid
                challengeCode
                challengeCategory
                isolationType
                flagType
                }
         */
         RecordId recordId = redisTemplate.opsForStream().add(queueProperties.getStreamKey(), fields);

        log.info("CTF 요청 발행: uuid={}, challengeCode={}, category={}, stream={}, recordId={}",
                uuid, challenge.getCode(), challenge.getCategory(),
                queueProperties.getStreamKey(), recordId);

        return uuid;
    }

    /** 이 uuid를 요청한 사용자. 매핑이 만료됐으면 null. */
    public Long findUserId(String uuid) {
        Object raw = redisTemplate.opsForHash().get(OWNER_KEY_PREFIX + uuid, "userId");
        if (raw == null) {
            return null;
        }
        try {
            return Long.valueOf(raw.toString());
        } catch (NumberFormatException e) {
            log.warn("요청 매핑의 userId를 읽지 못했습니다: uuid={}, raw={}", uuid, raw);
            return null;
        }
    }

    /** 이 uuid가 어느 문제의 요청이었는지. 매핑이 만료됐으면 null. */
    public String findChallengeCode(String uuid) {
        Object raw = redisTemplate.opsForHash().get(OWNER_KEY_PREFIX + uuid, "challengeCode");
        return raw == null ? null : raw.toString();
    }

    /** 이 uuid가 어느 문제(id)의 요청이었는지. 매핑이 만료됐거나 예전 형식(challengeId 없음)이면 null. */
    public Long findChallengeId(String uuid) {
        Object raw = redisTemplate.opsForHash().get(OWNER_KEY_PREFIX + uuid, "challengeId");
        if (raw == null) {
            return null;
        }
        try {
            return Long.valueOf(raw.toString());
        } catch (NumberFormatException e) {
            log.warn("요청 매핑의 challengeId를 읽지 못했습니다: uuid={}, raw={}", uuid, raw);
            return null;
        }
    }

    /** 이 uuid가 어느 유형(category) 문제의 요청이었는지. 매핑이 만료됐거나 예전 형식이면 null. */
    public ChallengeCategory findChallengeCategory(String uuid) {
        Object raw = redisTemplate.opsForHash().get(OWNER_KEY_PREFIX + uuid, "challengeCategory");
        if (raw == null) {
            return null;
        }
        try {
            return ChallengeCategory.valueOf(raw.toString());
        } catch (IllegalArgumentException e) {
            log.warn("요청 매핑의 challengeCategory를 읽지 못했습니다: uuid={}, raw={}", uuid, raw);
            return null;
        }
    }

    /**
     * 소유자 매핑을 인스턴스가 끝나는 시각까지 남긴다. 기본 보관 시간(CTF_TTL_OWNER)은 인스턴스 수명보다
     * 짧아서, 그대로 두면 컨테이너는 살아 있는데 소유권을 확인할 수 없어 프록시가 403을 낸다.
     */
    public void expireOwnerAt(String uuid, Instant until) {
        redisTemplate.expireAt(OWNER_KEY_PREFIX + uuid, until);
    }
}
