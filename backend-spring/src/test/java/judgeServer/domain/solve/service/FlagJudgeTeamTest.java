package judgeServer.domain.solve.service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.contest.enums.ContestPhase;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Java 24에서는 Mockito가 구체 클래스를 mock하지 못해 SolveRecorder·CtfTeamResolver·ContestService는 하위 클래스로 대신한다. */
class FlagJudgeTeamTest {

    private static final String HASH = "a".repeat(64);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 21, 12, 0);

    private final ChallengeRepository challengeRepository = mock(ChallengeRepository.class);
    private final RecordingSolveRecorder solveRecorder = new RecordingSolveRecorder();

    private final Challenge challenge = Challenge.builder().id(1L).flagHash(HASH).isPublic(true).build();

    @Test
    void participantWithoutTeamCannotSubmit() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(null, ContestPhase.RUNNING).judge(1L, 7L, false, HASH, NOW);

        assertEquals(SubmissionVerdict.Status.NO_TEAM, verdict.status());
        assertTrue(solveRecorder.calls.isEmpty());
    }

    @Test
    void staffWithoutTeamGetsVerdictButNoRecord() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(null, ContestPhase.RUNNING).judge(1L, 7L, true, HASH, NOW);

        assertEquals(SubmissionVerdict.Status.CORRECT, verdict.status());
        assertTrue(solveRecorder.calls.isEmpty());
    }

    @Test
    void teamMemberCorrectFlagIsRecordedForTeam() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(3L, ContestPhase.RUNNING).judge(1L, 7L, false, HASH, NOW);

        assertEquals(100, verdict.awardedPoints());
        assertEquals(List.of(List.of(1L, 7L, 3L, 9L)), solveRecorder.calls);
    }

    @Test
    void submissionAfterContestEndIsRejected() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(3L, ContestPhase.ENDED).judge(1L, 7L, false, HASH, NOW);

        assertEquals(SubmissionVerdict.Status.CONTEST_ENDED, verdict.status());
        assertTrue(solveRecorder.calls.isEmpty());
    }

    @Test
    void submissionBeforeContestStartIsRejected() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(3L, ContestPhase.UPCOMING).judge(1L, 7L, false, HASH, NOW);

        assertEquals(SubmissionVerdict.Status.CONTEST_NOT_STARTED, verdict.status());
    }

    @Test
    void staffAfterContestEndGetsVerdictButNoRecord() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        SubmissionVerdict verdict = judge(3L, ContestPhase.ENDED).judge(1L, 7L, true, HASH, NOW);

        assertEquals(SubmissionVerdict.Status.CORRECT, verdict.status());
        assertTrue(solveRecorder.calls.isEmpty());
    }

    @Test
    void unscheduledContestAcceptsSubmissions() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        judge(3L, ContestPhase.NOT_SCHEDULED).judge(1L, 7L, false, HASH, NOW);

        assertEquals(1, solveRecorder.calls.size());
    }

    private FlagJudgeService judge(Long teamId, ContestPhase phase) {
        CtfTeamResolver resolver = new CtfTeamResolver(null, null) {
            @Override
            public Optional<Long> findTeamId(Long userId) {
                return Optional.ofNullable(teamId);
            }
        };
        ContestService contest = new ContestService(null, null, null, null, null) {
            @Override
            public ContestPhase phaseAt(LocalDateTime time) {
                return phase;
            }

            @Override
            public Optional<Long> currentContestId() {
                return Optional.of(9L);
            }
        };
        return new FlagJudgeService(challengeRepository, solveRecorder, resolver, contest);
    }

    private static class RecordingSolveRecorder extends SolveRecorder {
        final List<List<Long>> calls = new ArrayList<>();

        RecordingSolveRecorder() {
            super(null, null, null);
        }

        @Override
        public SubmissionVerdict record(Long challengeId, Long userId, Long teamId, Long contestId) {
            calls.add(List.of(challengeId, userId, teamId, contestId));
            return new SubmissionVerdict(SubmissionVerdict.Status.CORRECT, 100);
        }
    }
}
