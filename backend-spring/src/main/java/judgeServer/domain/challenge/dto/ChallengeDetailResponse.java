package judgeServer.domain.challenge.dto;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.enums.FlagType;
import judgeServer.domain.challenge.enums.IsolationType;

/**
 * 문제 상세.
 *
 * @param solvedByMyTeam 우리 팀(팀이 없으면 본인)이 현재 대회에서 풀었는지. CTF 사이트는 false 면 "아직 풀지 않은 문제" 화면을 보여 준다
 * @param mySolve        풀었으면 그 기록 (푼 사람·시각·점수, 순위 비공개 중에는 점수 null). 안 풀었으면 null
 */
public record ChallengeDetailResponse(
        Long id,
        String code,
        String title,
        String description,
        ChallengeCategory category,
        /** 지금 풀면 받는 점수. 해결자가 늘면 줄어든다. 순위 비공개 중 참가자에게는 null (max·decrease 와 함께 보면 해결 팀 수가 드러나므로) */
        Integer points,
        Integer maxPoint,
        Integer minPoint,
        Integer decreasePoint,
        FlagType flagType,
        IsolationType isolationType,
        boolean hasAttachment,
        Boolean isPublic,
        boolean solvedByMyTeam,
        SolvedChallengeResponse mySolve
) {
    public static ChallengeDetailResponse from(Challenge challenge, long solveCount, SolvedChallengeResponse mySolve) {
        return from(challenge, solveCount, mySolve, false);
    }

    /** hidePoints: 순위 비공개 중 참가자 → 지금 점수(points)를 빼고 준다 */
    public static ChallengeDetailResponse from(Challenge challenge, long solveCount, SolvedChallengeResponse mySolve,
                                               boolean hidePoints) {
        return new ChallengeDetailResponse(
                challenge.getId(),
                challenge.getCode(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getCategory(),
                hidePoints ? null : challenge.currentPoints(solveCount),
                challenge.getMaxPoint(),
                challenge.getMinPoint(),
                challenge.getDecreasePoint(),
                challenge.getFlagType(),
                challenge.getIsolationType(),
                challenge.getAttachmentKey() != null && !challenge.getAttachmentKey().isBlank(),
                challenge.getIsPublic(),
                mySolve != null,
                mySolve
        );
    }
}
