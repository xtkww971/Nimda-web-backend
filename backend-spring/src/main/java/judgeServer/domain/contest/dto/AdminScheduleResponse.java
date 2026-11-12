package judgeServer.domain.contest.dto;

import judgeServer.domain.contest.enums.ContestPhase;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 운영 설정 화면 (현재 대회). 대회가 없으면 시각·설정은 null이고 status는 NOT_SCHEDULED다.
 *
 * @param finalizedAt           최종 결과가 확정돼 이력에 오른 시각. null이면 아직
 * @param closeSubmissionsOnEnd 종료 후 플래그 제출 마감. 끌 수 없어 항상 true
 */
public record AdminScheduleResponse(Long contestId, String name, OffsetDateTime startsAt, OffsetDateTime endsAt,
                                    ContestPhase status, OffsetDateTime serverTime, OffsetDateTime finalizedAt,
                                    boolean closeSubmissionsOnEnd,
                                    Boolean cleanupInstancesOnEnd,
                                    Boolean challengesVisibleAfterEnd,
                                    Boolean freezeScoreboard,
                                    Integer freezeMinutesBeforeEnd,
                                    List<ChangeLogResponse> history) {
}
