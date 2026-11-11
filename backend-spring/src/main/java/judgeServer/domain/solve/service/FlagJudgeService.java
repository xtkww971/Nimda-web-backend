package judgeServer.domain.solve.service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.FlagType;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 큐에서 꺼낸 제출을 채점한다. 트랜잭션을 걸면 안 된다 ({@link SolveRecorder} 참고). */
@Service
@RequiredArgsConstructor
public class FlagJudgeService {

    private final ChallengeRepository challengeRepository;
    private final SolveRecorder solveRecorder;

    public SubmissionVerdict judge(Long challengeId, Long userId, boolean canAccessDrafts, String submittedHash) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        // 비공개 문제는 있는지조차 알리지 않는다.
        if (challenge == null || (!Boolean.TRUE.equals(challenge.getIsPublic()) && !canAccessDrafts)) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.NOT_FOUND);
        }
        if (challenge.getFlagType() == FlagType.DYNAMIC) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.UNSUPPORTED);
        }
        if (challenge.getFlagHash() == null) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.ERROR);
        }

        boolean correct = MessageDigest.isEqual(
                challenge.getFlagHash().getBytes(StandardCharsets.UTF_8),
                submittedHash.getBytes(StandardCharsets.UTF_8));
        if (!correct) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.WRONG);
        }

        // 초안은 출제자가 플래그를 검수하는 단계다. 기록하면 공개 후 점수가 미리 깎인 채로 시작한다.
        if (!Boolean.TRUE.equals(challenge.getIsPublic())) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.CORRECT);
        }
        return solveRecorder.record(challengeId, userId);
    }
}
