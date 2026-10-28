package judgeServer.domain.challenge.instance.Service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.IsolationType;
import judgeServer.domain.challenge.instance.Config.InstanceProperties;
import judgeServer.domain.challenge.instance.ObjectStatus.InstanceStatus;
import judgeServer.domain.challenge.instance.ObjectStatus.Status;
import judgeServer.domain.challenge.instance.Repository.InstanceResultStore;
import judgeServer.domain.challenge.instance.Proxy.SubdomainInstanceProxyFilter;
import judgeServer.domain.challenge.instance.Repository.InstanceStatusStore;
import judgeServer.domain.challenge.mq.message.CtfResultMessage;
import judgeServer.domain.challenge.mq.message.RequestStatus;
import judgeServer.config.CtfTtlProperties;
import judgeServer.domain.challenge.mq.producer.RedisCtfRequestProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import redis.util.RedisUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceService {

    private final RedisCtfRequestProducer producer;
    private final InstanceStatusStore instanceStatusStore;
    private final InstanceResultStore resultStore;
    private final InstanceProperties instanceProperties;
    private final CtfTtlProperties ttlProperties;
    private final RedisUtil redisUtil;

    /**
     * 인스턴스를 요청한다. 같은 사용자가 같은 문제로 살아 있는 요청을 갖고 있으면 그 uuid를 다시 쓴다.
     *
     * <p>Judge는 컨테이너 이름(ctf-{code}-{uuid})이 같아야 같은 인스턴스로 본다. 요청마다 새 uuid를 만들면
     * 버튼을 누를 때마다 컨테이너가 하나씩(1시간씩) 늘어난다.
     *
     * <ul>
     *   <li>없음 / FAILED / 소유자 매핑 만료 → 새 uuid로 발행</li>
     *   <li>PENDING / READY → 같은 uuid, 재발행 안 함 (같은 이름으로 두 번 빌드되지 않게)</li>
     *   <li>EXPIRED(결과 만료) → 같은 uuid로 재발행. 떠 있으면 Judge가 그대로 돌려주고, 없으면 다시 만든다</li>
     * </ul>
     */
    public String createInstance(Challenge challenge, Long userId) {
        if (challenge.getIsolationType() != IsolationType.PER_USER
                && challenge.getIsolationType() != IsolationType.SHARED) {
            // NONE은 다운로드만 하는 문제라 인스턴스가 없다.
            return null;
        }

        Optional<String> mapped = resultStore.findUuid(userId, challenge.getId());
        if (mapped.isPresent()) {
            String uuid = mapped.get();
            if (userId.equals(producer.findUserId(uuid))) {
                Status state = state(uuid);
                if (state == Status.PENDING || state == Status.READY) {
                    return uuid;
                }
                if (state == Status.EXPIRED) {
                    publish(challenge, userId, uuid);
                    return uuid;
                }
            }
            // FAILED이거나 소유자 매핑이 만료된 uuid → 버리고 아래에서 새 uuid로 다시 시도한다.
            resultStore.deleteUserChallengeMapping(userId, challenge.getId());
        }

        String uuid = UUID.randomUUID().toString();
        // 동시에 두 번 눌렸으면 먼저 기록된 쪽을 쓴다.
        if (!resultStore.saveUserChallengeMappingIfAbsent(userId, challenge.getId(), uuid, ttlProperties.getOwner())) {
            return resultStore.findUuid(userId, challenge.getId()).orElse(uuid);
        }
        publish(challenge, userId, uuid);
        return uuid;
    }

    /**
     * 이 문제로 내가 띄운 인스턴스의 uuid. 매핑이 있어도 그 uuid의 소유자 매핑이 나와 다르거나 만료됐으면
     * 쓸 수 없는 것으로 본다 — 프록시가 소유권을 확인하지 못해 어차피 403이 난다.
     */
    public Optional<String> findMyUuid(Long userId, Long challengeId) {
        return resultStore.findUuid(userId, challengeId)
                .filter(uuid -> userId.equals(producer.findUserId(uuid)));
    }

    /** uuid의 현재 상태. 결과가 있으면 결과로, 없으면 생성 중 표시로 판단한다. */
    public Status state(String uuid) {
        return stateOf(uuid, resultStore.find(uuid).orElse(null));
    }

    /** 이 uuid를 요청한 사용자가 맞는지. 소유자 매핑이 만료됐으면 확인할 근거가 없으므로 false. */
    public boolean isOwner(String uuid, Long userId) {
        return userId != null && userId.equals(producer.findUserId(uuid));
    }

    /**
     * 참가자에게 보여줄 인스턴스 상태. host/port(내부 컨테이너 이름·EXPOSE 포트)는 싣지 않는다 —
     * 참가자는 accessUrl로만 접속한다.
     *
     * @param scheme     사용자가 플랫폼에 접속한 스킴 (accessUrl용)
     * @param serverPort 사용자가 플랫폼에 접속한 포트 (accessUrl용)
     */
    public Map<String, Object> view(String uuid, String scheme, int serverPort) {
        CtfResultMessage result = resultStore.find(uuid).orElse(null);
        Status state = stateOf(uuid, result);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("uuid", uuid);
        view.put("status", state.name());
        if (state == Status.READY) {
            view.put("accessUrl", accessUrl(result, scheme, serverPort));
            view.put("expiresAt", result.getExpiresAt());
        } else if (state == Status.FAILED) {
            // 생성 실패는 Judge가 준 사유를 그대로 전한다 (빌드 실패, 이미지 없음 등).
            view.put("message", result.getMessage());
        }
        return view;
    }

    private Status stateOf(String uuid, CtfResultMessage result) {
        if (result != null) {
            return result.getStatus() == RequestStatus.READY ? Status.READY : Status.FAILED;
        }
        return instanceStatusStore.getStatus(uuid).isPresent() ? Status.PENDING : Status.EXPIRED;
    }

    // 생성 중 표시를 남기고 발행한다. 표시는 결과가 올 때까지 "같은 이름으로 또 빌드하지 않기" 위한 것이다.
    private void publish(Challenge challenge, Long userId, String uuid) {
        instanceStatusStore.saveStatus(uuid, InstanceStatus.builder()
                .status(Status.PENDING)
                .build());
        resultStore.saveUserChallengeMapping(userId, challenge.getId(), uuid, ttlProperties.getOwner());
        // redis stream에 message 발행. 무엇을 할지는 조율자가 문제 유형을 보고 정한다.
        producer.request(challenge, userId, uuid);
    }

    /**
     * 접속 주소. baseDomain이 설정돼 있고 인스턴스가 READY일 때만 만들어진다.
     * 이 호스트로 들어온 요청은 {@link SubdomainInstanceProxyFilter}가 실제 컨테이너로 넘긴다.
     *
     * @param scheme      사용자가 플랫폼에 접속한 스킴 (http/https)
     * @param serverPort  사용자가 플랫폼에 접속한 포트
     */
    public String accessUrl(CtfResultMessage result, String scheme, int serverPort) {
        String baseDomain = instanceProperties.getBaseDomain();
        if (result == null || result.getStatus() != RequestStatus.READY
                || baseDomain == null || baseDomain.isBlank()) {
            return null;
        }

        boolean defaultPort = ("http".equals(scheme) && serverPort == 80)
                || ("https".equals(scheme) && serverPort == 443);

        // 주소에 uuid가 들어간다. 이 값을 모르면 남의 인스턴스 주소를 만들 수 없다 —
        // uuid를 쓰기로 한 이유가 이것이다.
        return scheme + "://" + instanceProperties.getSubdomainPrefix() + result.getUuid()
                + "." + baseDomain + (defaultPort ? "" : ":" + serverPort);
    }
}
