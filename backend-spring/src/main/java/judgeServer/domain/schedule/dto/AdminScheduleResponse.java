package judgeServer.domain.schedule.dto;

import judgeServer.domain.schedule.enums.ContestPhase;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 운영 설정 화면. 일정이 없으면 시각·설정은 null이고 status는 NOT_SCHEDULED다.
 *
 * @param closeSubmissionsOnEnd 종료 후 플래그 제출 마감. 끌 수 없어 항상 true
 */
public record AdminScheduleResponse(OffsetDateTime startsAt, OffsetDateTime endsAt, ContestPhase status,
                                    OffsetDateTime serverTime,
                                    boolean closeSubmissionsOnEnd,
                                    Boolean cleanupInstancesOnEnd,
                                    Boolean challengesVisibleAfterEnd,
                                    Boolean freezeScoreboard,
                                    Integer freezeMinutesBeforeEnd,
                                    List<ScheduleHistoryResponse> history) {
}
