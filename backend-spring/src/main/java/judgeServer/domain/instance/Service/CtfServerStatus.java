package judgeServer.domain.instance.Service;

import judgeServer.config.CtfQueueProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.util.RedisUtil;

/**
 * CTF 서버가 인스턴스 요청을 받을 수 있는지.
 *
 * <p>웹은 스트림에 요청을 넣기만 하고 누가 읽는지 모른다. 그래서 provisioner가 agent까지 살아 있을 때
 * heartbeat 키를 TTL 30초로 계속 갱신하고, 여기서는 그 키가 있는지만 본다 (CTF 레포 ADR-0022).
 * 값(쓴 시각)은 해석하지 않는다. 만료는 redis TTL이 대신 판정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CtfServerStatus {

    private final RedisUtil redisUtil;
    private final CtfQueueProperties queueProperties;

    public boolean isAvailable() {
        try {
            return redisUtil.getData(queueProperties.getHeartbeatKey()) != null;
        } catch (RuntimeException e) {
            // 웹 redis에 못 붙으면 요청을 넣을 곳도 없다. 꺼진 것으로 본다.
            log.warn("CTF 서버 heartbeat 조회 실패", e);
            return false;
        }
    }
}
