package judgeServer.domain.scoreboard.dto;

import java.time.LocalDateTime;

/** 팀 점수판 한 줄. teamId는 내 팀 행 강조에 쓴다. */
public record TeamScoreboardEntryResponse(long rank, Long teamId, String teamName, long memberCount,
                                          long totalScore, int solveCount, LocalDateTime lastSolvedAt) {

    public static TeamScoreboardEntryResponse of(long rank, TeamScoreboardRow row) {
        return new TeamScoreboardEntryResponse(rank, row.teamId(), row.teamName(), row.memberCount(),
                row.totalScore(), row.solveCount(), row.lastSolvedAt());
    }
}
