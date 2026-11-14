package judgeServer.domain.instance.service;

import judgeServer.config.CtfQueueProperties;
import org.junit.jupiter.api.Test;
import redis.util.RedisUtil;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CtfServerStatusTest {

    // Mockito inline mock은 JDK 버전에 따라 클래스 계측에 실패해서, 상속한 stub을 쓴다.
    static class StubRedis extends RedisUtil {
        final Map<String, String> data = new HashMap<>();
        RuntimeException failure;

        StubRedis() {
            super(null);
        }

        @Override
        public String getData(String key) {
            if (failure != null) throw failure;
            return data.get(key);
        }
    }

    private final StubRedis redis = new StubRedis();
    private final CtfQueueProperties props = new CtfQueueProperties();
    private final CtfServerStatus status = new CtfServerStatus(redis, props);

    @Test
    void heartbeat키가_있으면_사용가능() {
        redis.data.put("ctf:server:heartbeat", "2026-11-11T03:00:00Z");
        assertThat(status.isAvailable()).isTrue();
    }

    @Test
    void heartbeat키가_없으면_사용불가() {
        assertThat(status.isAvailable()).isFalse();
    }

    @Test
    void redis_조회가_실패하면_사용불가() {
        redis.failure = new RuntimeException("connection refused");
        assertThat(status.isAvailable()).isFalse();
    }

    @Test
    void 설정한_키를_본다() {
        props.setHeartbeatKey("custom:heartbeat");
        redis.data.put("custom:heartbeat", "x");
        assertThat(status.isAvailable()).isTrue();
    }
}
