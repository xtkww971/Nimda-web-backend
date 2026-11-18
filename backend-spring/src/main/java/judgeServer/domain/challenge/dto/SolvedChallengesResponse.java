package judgeServer.domain.challenge.dto;

import java.util.List;

/**
 * 우리 팀이 푼 문제 + 찾을 수 있는 문제 수 (대시보드 "팀이 푼 문제 3 / 12").
 * total 은 개수만 준다 (참가자에게 전체 목록·코드는 주지 않는다).
 */
public record SolvedChallengesResponse(long total, List<SolvedChallengeResponse> solved) {
}
