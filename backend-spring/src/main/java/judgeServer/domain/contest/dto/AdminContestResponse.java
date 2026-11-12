package judgeServer.domain.contest.dto;

import judgeServer.domain.contest.enums.ContestPhase;

import java.time.OffsetDateTime;

/** 운영자 대회 목록 한 줄. finalizedAt이 있으면 이력에 오른 대회다. */
public record AdminContestResponse(Long id, String name, OffsetDateTime startsAt, OffsetDateTime endsAt,
                                   ContestPhase status, OffsetDateTime finalizedAt, boolean current) {
}
