package judgeServer.domain.scoreboard.dto;

import java.util.List;

/**
 * 팀 점수판 페이지. page는 0부터 센다.
 *
 * @param rankedTeams 점수가 있어 순위에 오른 팀 수 (페이지 계산 기준)
 * @param totalTeams  전체 팀 수. 점수 0인 팀은 순위에 나오지 않으므로 rankedTeams보다 클 수 있다
 */
public record TeamScoreboardPageResponse(List<TeamScoreboardEntryResponse> entries, int page, int size,
                                         long rankedTeams, int totalPages, long totalTeams) {
}
