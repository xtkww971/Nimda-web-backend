package judgeServer.domain.contest.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 대회 이력 상세 (디자인: 10 CTF 대회 이력 상세). 모두 종료 시점 스냅샷이다 (라이브 데이터 아님).
 *
 * @param finalizedAt 결과를 고정한 시각 ("최종 결과 · 03.23 18:00 기준으로 고정")
 * @param myTeam      내가 참가하지 않은 대회면 null ("이 대회에 참가하지 않았습니다.")
 */
public record ContestDetailResponse(Long id, String name, OffsetDateTime startsAt, OffsetDateTime endsAt,
                                    OffsetDateTime finalizedAt, Stats stats, List<Standing> standings,
                                    List<ChallengeResult> challenges, MyTeam myTeam) {

    public record Stats(long teamCount, long participantCount, long challengeCount, long totalSolves) {
    }

    /** @param rank 점수가 0인 팀은 null */
    public record Standing(Integer rank, Long teamId, String teamName, int memberCount, int solveCount,
                           long totalScore, LocalDateTime lastSolvedAt) {
    }

    /** @param finalPoint 종료 시점 배점 ("최대 → 종료 시점") */
    public record ChallengeResult(Long challengeId, String code, String title, String category, int maxPoint,
                                  int finalPoint, int solvedTeams, String firstBloodTeamName,
                                  LocalDateTime firstBloodAt) {
    }

    /** @param totalTeams "#4 / 12"의 12 */
    public record MyTeam(Long teamId, String teamName, Integer rank, long totalTeams, long totalScore,
                         int solveCount, List<Member> members) {
    }

    /** @param role LEADER / MEMBER */
    public record Member(Long userId, String nickname, String role, int solveCount, long points) {
    }
}
