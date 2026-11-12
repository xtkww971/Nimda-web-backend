package judgeServer.domain.contest.dto;

import judgeServer.domain.contest.enums.ContestPhase;

import java.time.OffsetDateTime;

/**
 * 참가자용 현재 대회 일정. 남은 시간은 참가자 PC 시계가 아니라 serverTime 기준으로 계산한다.
 * 대회가 없으면 contestId·name·startsAt·endsAt은 null, status는 NOT_SCHEDULED다.
 */
public record ScheduleResponse(Long contestId, String name, OffsetDateTime startsAt, OffsetDateTime endsAt,
                               ContestPhase status, OffsetDateTime serverTime) {
}
