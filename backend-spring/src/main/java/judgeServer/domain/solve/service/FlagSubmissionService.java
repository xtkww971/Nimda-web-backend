package judgeServer.domain.solve.service;

import judgeServer.config.CtfQueueProperties;
import judgeServer.config.CtfTtlProperties;
import judgeServer.domain.challenge.util.FlagHasher;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 플래그 제출을 Redis Stream에 넣고 채점 결과를 기다린다.
 *
 * <p>스트림에는 평문 대신 해시만 넣는다. 정답 플래그가 Redis에 남지 않게 하기 위함이다.
 */
@Service
@RequiredArgsConstructor
public class FlagSubmissionService {

    public static final String RESULT_KEY_PREFIX = "ctf:flag:result:";
    private static final String RATE_KEY_PREFIX = "ctf:flag:rate:";

    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final long STREAM_MAX_LENGTH = 10_000;

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(3);
    private static final long POLL_INTERVAL_MS = 50;

    /** 제출 조회 결과. 아직 채점 전이면 verdict가 비어 있다. */
    public record Submission(String submissionId, Optional<SubmissionVerdict> verdict) {
    }

    private final StringRedisTemplate redisTemplate;
    private final CtfQueueProperties queueProperties;
    private final CtfTtlProperties ttlProperties;

    /**
     * 제출을 큐에 넣고 잠깐 기다린다.
     *
     * <p>이 경로에서는 DB를 조회하면 안 된다. Open-in-view가 켜져 있어 한 번이라도 조회하면 응답을
     * 보낼 때까지 커넥션을 쥐고 있게 되고, 제출이 몰리면 채점기가 커넥션을 얻지 못해 서로 기다린다.
     * 문제 존재·권한 확인까지 채점기에서 한다.
     */
    public Submission submit(Long challengeId, Long userId, boolean canAccessDrafts, String flag) {
        checkRate(userId, challengeId);

        String submissionId = UUID.randomUUID().toString();
        String streamKey = queueProperties.getFlagStreamKey();
        redisTemplate.opsForStream().add(streamKey, Map.of(
                "submissionId", submissionId,
                "challengeId", String.valueOf(challengeId),
                "userId", String.valueOf(userId),
                "canAccessDrafts", String.valueOf(canAccessDrafts),
                "flagHash", FlagHasher.hash(flag),
                // 마감 판정 기준. 큐가 밀려 종료 직후에 채점돼도 종료 전에 낸 제출은 인정한다.
                "submittedAt", String.valueOf(System.currentTimeMillis())));
        redisTemplate.opsForStream().trim(streamKey, STREAM_MAX_LENGTH, true);

        return new Submission(submissionId, awaitVerdict(submissionId, userId));
    }

    /** 채점 결과를 조회한다. 아직이거나 보관 시간이 지났으면 비어 있다. */
    public Submission find(String submissionId, Long userId) {
        return new Submission(submissionId, readVerdict(submissionId, userId));
    }

    public void saveVerdict(String submissionId, Long userId, SubmissionVerdict verdict) {
        String key = RESULT_KEY_PREFIX + submissionId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                "userId", String.valueOf(userId),
                "status", verdict.status().name(),
                "awardedPoints", String.valueOf(verdict.awardedPoints())));
        redisTemplate.expire(key, ttlProperties.getResult());
    }

    // 문제 플래그를 무차별 대입하지 못하도록 사용자·문제 단위로 시도 횟수를 제한한다.
    private void checkRate(Long userId, Long challengeId) {
        String key = RATE_KEY_PREFIX + userId + ":" + challengeId;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, RATE_WINDOW);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS_PER_WINDOW) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "제출이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private Optional<SubmissionVerdict> awaitVerdict(String submissionId, Long userId) {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        while (true) {
            Optional<SubmissionVerdict> verdict = readVerdict(submissionId, userId);
            if (verdict.isPresent() || System.nanoTime() >= deadline) {
                return verdict;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    private Optional<SubmissionVerdict> readVerdict(String submissionId, Long userId) {
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(RESULT_KEY_PREFIX + submissionId);
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        if (!String.valueOf(userId).equals(fields.get("userId"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 제출이 아닙니다.");
        }
        return Optional.of(new SubmissionVerdict(
                SubmissionVerdict.Status.valueOf((String) fields.get("status")),
                Integer.parseInt((String) fields.get("awardedPoints"))));
    }
}
