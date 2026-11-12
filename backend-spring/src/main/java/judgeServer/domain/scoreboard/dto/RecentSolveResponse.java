package judgeServer.domain.scoreboard.dto;

import java.time.LocalDateTime;

/** 내 팀 최근 해결 한 줄 (디자인: MY TEAM · RECENT SOLVES). */
public record RecentSolveResponse(LocalDateTime solvedAt, String nickname, String challengeCode,
                                  String challengeTitle, Integer points) {
}
