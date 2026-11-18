package judgeServer.domain.challenge.dto;

import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.enums.IsolationType;
import judgeServer.domain.contest.service.ContestService;

import java.time.OffsetDateTime;

/**
 * 우리 팀이 푼 문제 하나 (CTF 사이트 게임 모드의 "해결한 문제" 카드, 문제 상세의 "해결함" 배너).
 *
 * @param awardedPoints 이 풀이로 받은 점수. 순위 비공개 중에는 참가자에게 null (점수를 더하면 팀 점수가 드러나므로)
 * @param solvedBy      푼 팀원의 닉네임
 */
public record SolvedChallengeResponse(String code, String title, ChallengeCategory category, IsolationType isolationType,
                                      boolean hasAttachment, Integer awardedPoints, OffsetDateTime solvedAt,
                                      String solvedBy) {

    public static SolvedChallengeResponse from(SolvedChallengeRow row, boolean hidePoints) {
        return new SolvedChallengeResponse(row.code(), row.title(), row.category(), row.isolationType(),
                row.attachmentKey() != null && !row.attachmentKey().isBlank(),
                hidePoints ? null : row.awardedPoints(),
                ContestService.toKst(row.solvedAt()), row.solvedBy());
    }
}
