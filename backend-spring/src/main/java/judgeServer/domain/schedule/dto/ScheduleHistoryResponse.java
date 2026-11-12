package judgeServer.domain.schedule.dto;

import java.time.LocalDateTime;

/**
 * 일정 변경 이력 한 줄 (CHANGE LOG).
 *
 * @param field CREATED, STARTS_AT, ENDS_AT, CLEANUP_INSTANCES, CHALLENGES_VISIBLE, FREEZE_SCOREBOARD, FREEZE_MINUTES
 */
public record ScheduleHistoryResponse(LocalDateTime changedAt, String changedBy, String field,
                                      String oldValue, String newValue) {
}
