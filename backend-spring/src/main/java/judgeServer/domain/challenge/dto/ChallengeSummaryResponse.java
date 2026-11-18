package judgeServer.domain.challenge.dto;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.enums.IsolationType;

public record ChallengeSummaryResponse(
        Long id,
        String code,
        String title,
        ChallengeCategory category,
        /** 지금 풀면 받는 점수. 해결자가 늘면 줄어든다. */
        Integer points,
        Integer maxPoint,
        Integer minPoint,
        Integer decreasePoint,
        IsolationType isolationType,
        /** 첨부파일(문제 파일)이 붙어 있는지. 실제 S3 키는 내보내지 않는다. */
        boolean hasAttachment,
        Boolean isPublic,
        /** 현재 대회에서 이 문제를 푼 팀 수 (문제 관리 화면의 "해결 팀") */
        long solveCount
) {
    public static ChallengeSummaryResponse from(Challenge challenge, long solveCount) {
        return new ChallengeSummaryResponse(
                challenge.getId(),
                challenge.getCode(),
                challenge.getTitle(),
                challenge.getCategory(),
                challenge.currentPoints(solveCount),
                challenge.getMaxPoint(),
                challenge.getMinPoint(),
                challenge.getDecreasePoint(),
                challenge.getIsolationType(),
                challenge.getAttachmentKey() != null && !challenge.getAttachmentKey().isBlank(),
                challenge.getIsPublic(),
                solveCount
        );
    }
}
