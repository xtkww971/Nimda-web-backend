package judgeServer.domain.solve.service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.FlagType;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.contest.enums.ContestPhase;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/** 큐에서 꺼낸 제출을 채점한다. 트랜잭션을 걸면 안 된다 ({@link SolveRecorder} 참고). */
@Service
@RequiredArgsConstructor
public class FlagJudgeService {

    private final ChallengeRepository challengeRepository;
    private final SolveRecorder solveRecorder;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;

    /**
     * @param submittedAt 참가자가 제출한 시각(KST). 큐에서 늦게 꺼내도 마감 판정은 이 시각으로 한다.
     */
    public SubmissionVerdict judge(Long challengeId, Long userId, boolean canAccessDrafts, String submittedHash,
                                   LocalDateTime submittedAt) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        // 비공개 문제는 있는지조차 알리지 않는다.
        if (challenge == null || (!Boolean.TRUE.equals(challenge.getIsPublic()) && !canAccessDrafts)) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.NOT_FOUND);
        }
        // 대회 시간 밖의 제출은 서버가 거부한다 (프론트 비활성화만으로는 부족). 관리자·DEV는 검수를 위해 채점만 하고 기록하지 않는다.
        ContestPhase phase = contestService.phaseAt(submittedAt);
        if (!phase.acceptsSubmissions() && !canAccessDrafts) {
            return SubmissionVerdict.of(phase == ContestPhase.UPCOMING
                    ? SubmissionVerdict.Status.CONTEST_NOT_STARTED
                    : SubmissionVerdict.Status.CONTEST_ENDED);
        }
        // 팀전이라 팀이 없는 참가자는 제출할 수 없다. 관리자·DEV는 검수를 위해 통과시키되 팀이 없으면 기록하지 않는다.
        Long teamId = teamResolver.findTeamId(userId).orElse(null);
        if (teamId == null && !canAccessDrafts) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.NO_TEAM);
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
        // 팀이 없는 관리자·DEV의 정답도 기록하지 않는다 (점수를 받을 팀이 없다). 대회 시간 밖의 정답도 마찬가지다.
        if (!Boolean.TRUE.equals(challenge.getIsPublic()) || teamId == null || !phase.acceptsSubmissions()) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.CORRECT);
        }
        return solveRecorder.record(challengeId, userId, teamId, contestService.currentContestId().orElse(null));
    }
}
