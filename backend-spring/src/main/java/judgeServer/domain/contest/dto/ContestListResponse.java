package judgeServer.domain.contest.dto;

import judgeServer.domain.contest.enums.ContestPhase;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 대회 이력 목록 (디자인: 09 CTF 대회 이력).
 *
 * @param current 아직 결과가 확정되지 않은 현재 대회. 이력에 넣지 않고 배너로 스코어보드에 연결한다. 없으면 null
 * @param ended   결과가 확정된 대회
 */
public record ContestListResponse(CurrentContest current, List<EndedContest> ended) {

    public record CurrentContest(Long id, String name, OffsetDateTime startsAt, OffsetDateTime endsAt,
                                 ContestPhase status, long teamCount) {
    }

    /**
     * @param winner   우승 팀. 점수를 얻은 팀이 없으면 null
     * @param myResult 내가 참가했으면 내 팀 결과, 아니면 null ("참가하지 않음")
     */
    public record EndedContest(Long id, String name, OffsetDateTime startsAt, OffsetDateTime endsAt,
                               long teamCount, long participantCount, long challengeCount,
                               Winner winner, MyResult myResult) {
    }

    public record Winner(Long teamId, String teamName, long totalScore) {
    }

    /** @param rank 점수가 0이었으면 null */
    public record MyResult(Integer rank, Long teamId, String teamName, long totalScore) {
    }
}
