package judgeServer.domain.schedule.dto;

import judgeServer.domain.schedule.enums.ContestPhase;

import java.time.OffsetDateTime;

/**
 * 참가자용 대회 일정. 남은 시간은 참가자 PC 시계가 아니라 serverTime 기준으로 계산한다.
 * 일정이 없으면 startsAt·endsAt은 null, status는 NOT_SCHEDULED다.
 */
public record ScheduleResponse(OffsetDateTime startsAt, OffsetDateTime endsAt, ContestPhase status,
                               OffsetDateTime serverTime) {
}
