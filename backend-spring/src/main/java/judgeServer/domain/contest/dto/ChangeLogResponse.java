package judgeServer.domain.contest.dto;

import java.time.LocalDateTime;

/**
 * 일정 변경 이력 한 줄 (CHANGE LOG).
 *
 * @param changedBy 바꾼 운영자 닉네임. 자동 확정(FINALIZED)이면 null
 * @param field     CREATED, NAME, STARTS_AT, ENDS_AT, CLEANUP_INSTANCES, CHALLENGES_VISIBLE, FREEZE_SCOREBOARD,
 *                  FREEZE_MINUTES, REOPENED, FINALIZED
 */
public record ChangeLogResponse(LocalDateTime changedAt, String changedBy, String field,
                                String oldValue, String newValue) {
}
