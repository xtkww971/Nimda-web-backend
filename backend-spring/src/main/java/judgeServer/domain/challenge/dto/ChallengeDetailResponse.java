package judgeServer.domain.challenge.dto;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.enums.FlagType;
import judgeServer.domain.challenge.enums.IsolationType;

public record ChallengeDetailResponse(
        Long id,
        String code,
        String title,
        String description,
        ChallengeCategory category,
        /** 지금 풀면 받는 점수. 해결자가 늘면 줄어든다. */
        Integer points,
        Integer maxPoint,
        Integer minPoint,
        Integer decreasePoint,
        FlagType flagType,
        IsolationType isolationType,
        boolean hasAttachment,
        Boolean isPublic
) {
    public static ChallengeDetailResponse from(Challenge challenge, long solveCount) {
        return new ChallengeDetailResponse(
                challenge.getId(),
                challenge.getCode(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getCategory(),
                challenge.currentPoints(solveCount),
                challenge.getMaxPoint(),
                challenge.getMinPoint(),
                challenge.getDecreasePoint(),
                challenge.getFlagType(),
                challenge.getIsolationType(),
                challenge.getAttachmentKey() != null && !challenge.getAttachmentKey().isBlank(),
                challenge.getIsPublic()
        );
    }
}
