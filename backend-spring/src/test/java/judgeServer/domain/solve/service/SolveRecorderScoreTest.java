package judgeServer.domain.solve.service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import judgeServer.domain.solve.entity.Solve;
import judgeServer.domain.solve.repository.SolveRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class SolveRecorderScoreTest {

    private final ChallengeRepository challengeRepository = mock(ChallengeRepository.class);
    private final SolveRepository solveRepository = mock(SolveRepository.class);
    private final CtfTeamScoreRepository teamScoreRepository = mock(CtfTeamScoreRepository.class);
    private final SolveRecorder recorder = new SolveRecorder(challengeRepository, solveRepository, teamScoreRepository);

    private final Challenge challenge = Challenge.builder().id(1L).maxPoint(100).minPoint(50).decreasePoint(10).build();

    @Test
    void correctSolveAddsAwardedPointsToTeamScore() {
        LocalDateTime solvedAt = LocalDateTime.of(2026, 11, 12, 10, 0);
        when(challengeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(challenge));
        when(solveRepository.existsByChallengeIdAndTeamId(1L, 3L)).thenReturn(false);
        when(solveRepository.countInContest(1L, 9L)).thenReturn(2L);
        when(solveRepository.save(any(Solve.class))).thenAnswer(inv -> {
            Solve s = inv.getArgument(0);
            assertEquals(3L, s.getTeamId());
            assertEquals(9L, s.getContestId());
            return Solve.builder().challengeId(s.getChallengeId()).userId(s.getUserId()).teamId(s.getTeamId())
                    .awardedPoints(s.getAwardedPoints()).solvedAt(solvedAt).build();
        });

        SubmissionVerdict verdict = recorder.record(1L, 7L, 3L, 9L);

        assertEquals(SubmissionVerdict.Status.CORRECT, verdict.status());
        assertEquals(80, verdict.awardedPoints());
        verify(teamScoreRepository).addSolve(3L, 80, solvedAt);
    }

    @Test
    void teammateAlreadySolvedGivesNoPoints() {
        when(challengeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(challenge));
        when(solveRepository.existsByChallengeIdAndTeamId(1L, 3L)).thenReturn(true);

        // 같은 팀(3)의 다른 멤버(8)가 이미 푼 문제를 다시 맞힌 경우
        SubmissionVerdict verdict = recorder.record(1L, 8L, 3L, 9L);

        assertEquals(SubmissionVerdict.Status.ALREADY_SOLVED, verdict.status());
        verify(teamScoreRepository, never()).addSolve(anyLong(), anyLong(), any());
    }
}
