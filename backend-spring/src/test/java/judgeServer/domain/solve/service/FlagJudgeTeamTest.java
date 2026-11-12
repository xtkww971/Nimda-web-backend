package judgeServer.domain.solve.service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Java 24에서는 Mockito가 구체 클래스를 mock하지 못해 SolveRecorder·CtfTeamResolver는 하위 클래스로 대신한다. */
class FlagJudgeTeamTest {

    private static final String HASH = "a".repeat(64);

    private final ChallengeRepository challengeRepository = mock(ChallengeRepository.class);
    private final RecordingSolveRecorder solveRecorder = new RecordingSolveRecorder();

    private final Challenge challenge = Challenge.builder().id(1L).flagHash(HASH).isPublic(true).build();

    @Test
    void participantWithoutTeamCannotSubmit() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(null).judge(1L, 7L, false, HASH);

        assertEquals(SubmissionVerdict.Status.NO_TEAM, verdict.status());
        assertTrue(solveRecorder.calls.isEmpty());
    }

    @Test
    void staffWithoutTeamGetsVerdictButNoRecord() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(null).judge(1L, 7L, true, HASH);

        assertEquals(SubmissionVerdict.Status.CORRECT, verdict.status());
        assertTrue(solveRecorder.calls.isEmpty());
    }

    @Test
    void teamMemberCorrectFlagIsRecordedForTeam() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(3L).judge(1L, 7L, false, HASH);

        assertEquals(100, verdict.awardedPoints());
        assertEquals(List.of(List.of(1L, 7L, 3L)), solveRecorder.calls);
    }

    private FlagJudgeService judge(Long teamId) {
        CtfTeamResolver resolver = new CtfTeamResolver(null) {
            @Override
            public Optional<Long> findTeamId(Long userId) {
                return Optional.ofNullable(teamId);
            }
        };
        return new FlagJudgeService(challengeRepository, solveRecorder, resolver);
    }

    private static class RecordingSolveRecorder extends SolveRecorder {
        final List<List<Long>> calls = new ArrayList<>();

        RecordingSolveRecorder() {
            super(null, null, null);
        }

        @Override
        public SubmissionVerdict record(Long challengeId, Long userId, Long teamId) {
            calls.add(List.of(challengeId, userId, teamId));
            return new SubmissionVerdict(SubmissionVerdict.Status.CORRECT, 100);
        }
    }
}
