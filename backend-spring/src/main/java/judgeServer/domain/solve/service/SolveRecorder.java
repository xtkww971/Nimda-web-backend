package judgeServer.domain.solve.service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import judgeServer.domain.solve.entity.Solve;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정답을 기록하고 점수를 정한다.
 *
 * <p>백엔드가 여러 대면 같은 문제의 정답이 동시에 처리될 수 있어 문제 행을 잠가 순서를 세운다.
 * MySQL 기본 격리 수준(REPEATABLE READ)은 트랜잭션의 첫 일반 조회 시점으로 스냅샷을 고정하므로,
 * 잠금보다 먼저 일반 조회를 하면 잠금을 얻은 뒤에도 옛 해결자 수를 읽게 된다. 그래서 이 트랜잭션은
 * 반드시 잠금 조회로 시작하고, 정답 비교는 트랜잭션 밖({@link FlagJudgeService})에서 한다.
 */
@Service
@RequiredArgsConstructor
public class SolveRecorder {

    private final ChallengeRepository challengeRepository;
    private final SolveRepository solveRepository;
    private final CtfTeamScoreRepository teamScoreRepository;

    /** 팀 단위로 기록한다. 같은 팀원이 이미 맞힌 문제면 ALREADY_SOLVED이고 점수는 다시 주지 않는다. */
    @Transactional
    public SubmissionVerdict record(Long challengeId, Long userId, Long teamId, Long contestId) {
        Challenge challenge = challengeRepository.findByIdForUpdate(challengeId).orElse(null);
        if (challenge == null) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.ERROR);
        }
        if (solveRepository.existsByChallengeIdAndTeamId(challengeId, teamId)) {
            return SubmissionVerdict.of(SubmissionVerdict.Status.ALREADY_SOLVED);
        }

        int awarded = challenge.currentPoints(solveRepository.countInContest(challengeId, contestId));
        Solve solve = solveRepository.save(Solve.builder()
                .challengeId(challengeId)
                .userId(userId)
                .teamId(teamId)
                .contestId(contestId)
                .awardedPoints(awarded)
                .build());
        // 같은 트랜잭션에서 팀 점수판에 더한다. 정답 기록이 롤백되면 점수도 같이 롤백된다.
        teamScoreRepository.addSolve(teamId, awarded, solve.getSolvedAt());
        return new SubmissionVerdict(SubmissionVerdict.Status.CORRECT, awarded);
    }
}
