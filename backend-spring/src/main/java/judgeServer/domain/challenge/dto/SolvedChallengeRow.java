package judgeServer.domain.challenge.dto;

import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.enums.IsolationType;

import java.time.LocalDateTime;

/** 정답 기록 + 문제 + 푼 사람을 한 번에 읽은 행 (SolveRepository 의 JPQL 생성자 식). 응답은 {@link SolvedChallengeResponse}. */
public record SolvedChallengeRow(Long challengeId, String code, String title, ChallengeCategory category,
                                 IsolationType isolationType, String attachmentKey, Integer awardedPoints,
                                 LocalDateTime solvedAt, String solvedBy) {
}
