package judgeServer.domain.scoreboard.dto;

import judgeServer.domain.groupMember.enums.MemberRole;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 팀 점수 (디자인: 상단 "내 팀" 카드, MY TEAM · CONTRIBUTION, MY TEAM · RECENT SOLVES, 대시보드 팀 점수/순위).
 *
 * @param rank       팀 순위(1부터). 아직 점수가 없으면 null
 * @param totalTeams 전체 팀 수 ("#4 / 12"의 12)
 */
public record MyTeamScoreResponse(Long teamId, String teamName, Long rank, long totalTeams, long totalScore,
                                  int solveCount, int memberCount, LocalDateTime lastSolvedAt,
                                  List<Contribution> members, List<RecentSolveResponse> recentSolves) {

    /** 멤버 한 명의 기여. 아직 못 푼 멤버도 0으로 나온다. */
    public record Contribution(Long userId, String nickname, MemberRole role, long solveCount, long points) {
    }
}
