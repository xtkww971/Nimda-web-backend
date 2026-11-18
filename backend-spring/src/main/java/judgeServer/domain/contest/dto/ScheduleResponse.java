package judgeServer.domain.contest.dto;

import judgeServer.domain.contest.enums.ContestPhase;

import java.time.OffsetDateTime;

/**
 * 참가자용 현재 대회 일정. 남은 시간은 참가자 PC 시계가 아니라 serverTime 기준으로 계산한다.
 * 대회가 없으면 contestId·name·startsAt·endsAt은 null, status는 NOT_SCHEDULED다.
 *
 * @param scoreboardHiddenFrom 순위 비공개가 시작되는 시각 (종료 N분 전). 대회 설정에서 순위 비공개를 껐거나 대회가 없으면 null.
 *                             비공개는 이 시각부터 endsAt 까지이고, 종료와 함께 공개된다 (화면의 "곧 순위가 가려집니다" 카운트다운)
 * @param scoreboardHidden     지금 참가자에게 순위가 가려져 있는지
 */
public record ScheduleResponse(Long contestId, String name, OffsetDateTime startsAt, OffsetDateTime endsAt,
                               ContestPhase status, OffsetDateTime serverTime,
                               OffsetDateTime scoreboardHiddenFrom, boolean scoreboardHidden) {
}
