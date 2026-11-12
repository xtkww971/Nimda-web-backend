package judgeServer.domain.scoreboard.dto;

import java.time.LocalDateTime;

/** 팀 점수판 조회 결과 한 줄 (순위 계산 전). */
public record TeamScoreboardRow(Long teamId, String teamName, Long memberCount, Long totalScore,
                                Integer solveCount, LocalDateTime lastSolvedAt) {
}
