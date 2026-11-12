package redis.mq.consumer;

import judgeServer.config.CtfQueueProperties;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.solve.service.FlagJudgeService;
import judgeServer.domain.solve.service.FlagSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;
import redis.mq.stream.RedisStreamErrors;
import redis.mq.stream.StreamFields;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 플래그 제출 스트림을 소비해 채점한다. 백엔드 인스턴스마다 컨슈머 하나가 붙고, 한 대 안에서는 순서대로 처리한다.
 * 여러 대 사이의 순서는 {@link judgeServer.domain.solve.service.SolveRecorder}의 행 잠금이 맞춘다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlagSubmissionConsumer
        implements StreamListener<String, MapRecord<String, String, String>>, InitializingBean, DisposableBean {

    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate redisTemplate;
    private final CtfQueueProperties props;
    private final FlagJudgeService judgeService;
    private final FlagSubmissionService submissionService;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    @Override
    public void afterPropertiesSet() {
        ensureGroup();

        container = StreamMessageListenerContainer.create(connectionFactory,
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build());

        // Redis가 잠깐 끊겨도 구독이 영구히 멈추지 않도록 오류로 취소하지 않는다.
        container.register(
                StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create(props.getFlagStreamKey(), ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(props.getFlagConsumerGroup(), "backend-" + UUID.randomUUID()))
                        .autoAcknowledge(false)
                        .cancelOnError(error -> false)
                        .errorHandler(error -> log.warn("플래그 제출 스트림 읽기 실패: {}", error.getMessage()))
                        .build(),
                this);
        container.start();

        log.info("플래그 제출 소비 시작: stream={}, group={}", props.getFlagStreamKey(), props.getFlagConsumerGroup());
    }

    private void ensureGroup() {
        try {
            redisTemplate.execute((RedisCallback<String>) connection -> connection.streamCommands().xGroupCreate(
                    props.getFlagStreamKey().getBytes(StandardCharsets.UTF_8),
                    props.getFlagConsumerGroup(), ReadOffset.from("0"), true));
        } catch (Exception e) {
            if (!RedisStreamErrors.isBusyGroup(e)) {
                log.warn("플래그 제출 컨슈머 그룹 생성 실패: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            handle(message.getValue());
        } catch (Exception e) {
            log.error("플래그 제출 처리 실패 (id={}): {}", message.getId(), e.getMessage(), e);
        } finally {
            redisTemplate.opsForStream().acknowledge(props.getFlagStreamKey(), props.getFlagConsumerGroup(), message.getId());
        }
    }

    private void handle(Map<String, String> fields) {
        String submissionId = StreamFields.required(fields, "submissionId", "플래그 제출");
        Long challengeId = Long.valueOf(StreamFields.required(fields, "challengeId", "플래그 제출"));
        Long userId = Long.valueOf(StreamFields.required(fields, "userId", "플래그 제출"));
        boolean canAccessDrafts = Boolean.parseBoolean(fields.get("canAccessDrafts"));
        String flagHash = StreamFields.required(fields, "flagHash", "플래그 제출");
        LocalDateTime submittedAt = parseSubmittedAt(fields.get("submittedAt"));

        SubmissionVerdict verdict;
        try {
            verdict = judgeService.judge(challengeId, userId, canAccessDrafts, flagHash, submittedAt);
        } catch (Exception e) {
            log.error("플래그 채점 실패: submissionId={}, challengeId={}: {}", submissionId, challengeId, e.getMessage(), e);
            verdict = SubmissionVerdict.of(SubmissionVerdict.Status.ERROR);
        }
        submissionService.saveVerdict(submissionId, userId, verdict);
    }

    // 배포 직후 이전 버전이 넣은 메시지에는 제출 시각이 없다. 그때는 지금 시각으로 판정한다.
    private static LocalDateTime parseSubmittedAt(String epochMillis) {
        if (epochMillis == null || epochMillis.isBlank()) {
            return LocalDateTime.now(ContestService.KST);
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(epochMillis)), ContestService.KST);
        } catch (NumberFormatException e) {
            return LocalDateTime.now(ContestService.KST);
        }
    }

    @Override
    public void destroy() {
        if (container != null) {
            container.stop();
        }
    }
}
