package judgeServer.domain.contest.service;

import judgeServer.domain.contest.dto.ContestSaveRequest;
import judgeServer.domain.contest.entity.Contest;
import judgeServer.domain.contest.entity.ContestChangeLog;
import judgeServer.domain.contest.enums.ContestPhase;
import judgeServer.domain.contest.repository.ContestChangeLogRepository;
import judgeServer.domain.contest.repository.ContestRepository;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.solve.repository.SolveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ContestServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 3, 21, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 3, 22, 18, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 21, 10, 35);

    private final ContestRepository contestRepository = mock(ContestRepository.class);
    private final ContestChangeLogRepository changeLogRepository = mock(ContestChangeLogRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final SolveRepository solveRepository = mock(SolveRepository.class);
    private final RecordingSnapshotService snapshotService = new RecordingSnapshotService();
    private final ContestService service = new ContestService(contestRepository, changeLogRepository, snapshotService,
            groupRepository, solveRepository);

    @BeforeEach
    void fixClock() {
        at(NOW);
        when(contestRepository.save(any(Contest.class))).thenAnswer(inv -> {
            Contest c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 1L);
            return c;
        });
    }

    @Test
    void phaseBoundaries() {
        Contest c = contest(1L, START, END, null);
        assertEquals(ContestPhase.UPCOMING, c.phaseAt(START.minusNanos(1)));
        assertEquals(ContestPhase.RUNNING, c.phaseAt(START));
        assertEquals(ContestPhase.RUNNING, c.phaseAt(END.minusNanos(1)));
        assertEquals(ContestPhase.ENDED, c.phaseAt(END));
    }

    @Test
    void noContestMeansNotScheduled() {
        assertEquals(ContestPhase.NOT_SCHEDULED, service.phaseAt(NOW));
        assertEquals(ContestPhase.NOT_SCHEDULED, service.getSchedule().status());
        assertNotNull(service.getSchedule().serverTime());
    }

    @Test
    void endBeforeStartIsRejected() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(request(null, END, START), 1L));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(contestRepository, never()).save(any());
    }

    @Test
    void firstContestAdoptsOrphanTeamsAndUsesDefaults() {
        service.create(request(null, START, END), 1L);

        ArgumentCaptor<Contest> saved = ArgumentCaptor.forClass(Contest.class);
        verify(contestRepository).save(saved.capture());
        assertEquals("NIMDACON 2026", saved.getValue().getName());
        assertTrue(saved.getValue().getCleanupInstancesOnEnd());
        assertTrue(saved.getValue().getChallengesVisibleAfterEnd());
        assertFalse(saved.getValue().getFreezeScoreboard());
        assertEquals(30, saved.getValue().getFreezeMinutesBeforeEnd());

        // 대회 없이 만든 팀과 그 팀 정답 기록을 첫 대회로 편입한다
        verify(groupRepository).assignOrphansToContest(1L);
        verify(solveRepository).assignOrphansToContest(1L);

        ArgumentCaptor<ContestChangeLog> log = ArgumentCaptor.forClass(ContestChangeLog.class);
        verify(changeLogRepository).save(log.capture());
        assertEquals("CREATED", log.getValue().getField());
        assertEquals("2026-03-21 10:00 ~ 2026-03-22 18:00", log.getValue().getNewValue());
    }

    @Test
    void cannotCreateWhileLatestContestIsNotFinalized() {
        currentIs(contest(1L, START, END, null));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(request("NIMDACON 2027", START.plusYears(1), END.plusYears(1)), 1L));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void secondContestDoesNotAdoptOrphans() {
        currentIs(contest(1L, START, END, END.plusMinutes(1)));

        service.create(request("NIMDACON 2027", START.plusYears(1), END.plusYears(1)), 1L);

        verify(groupRepository, never()).assignOrphansToContest(anyLong());
        verify(solveRepository, never()).assignOrphansToContest(anyLong());
    }

    @Test
    void extendingEndRecordsOnlyChangedFields() {
        Contest existing = contest(1L, START, LocalDateTime.of(2026, 3, 22, 17, 0), null);
        currentIs(existing);

        service.updateCurrent(request(null, START, END), 1L);

        List<ContestChangeLog> logs = savedLogs();
        assertEquals(1, logs.size());
        assertEquals("ENDS_AT", logs.get(0).getField());
        assertEquals("2026-03-22 17:00", logs.get(0).getOldValue());
        assertEquals("2026-03-22 18:00", logs.get(0).getNewValue());
        assertEquals(END, existing.getEndsAt());
        assertEquals("NIMDACON 2026", existing.getName());
    }

    @Test
    void reopeningFinalizedContestClearsSnapshot() {
        at(END.plusHours(1));
        Contest finalized = contest(1L, START, END, END.plusMinutes(1));
        currentIs(finalized);

        service.updateCurrent(request(null, START, END.plusDays(1)), 1L);

        assertFalse(finalized.isFinalized());
        assertEquals(List.of("clear:1"), snapshotService.calls);
        assertTrue(savedLogs().stream().anyMatch(l -> "REOPENED".equals(l.getField())));
    }

    @Test
    void autoFinalizeWaitsForGracePeriod() {
        Contest ended = contest(1L, START, END, null);
        currentIs(ended);

        at(END.plusSeconds(30));
        assertFalse(service.finalizeIfDue());
        assertTrue(snapshotService.calls.isEmpty());

        at(END.plus(ContestService.FINALIZE_GRACE));
        assertTrue(service.finalizeIfDue());
        assertEquals(List.of("write:1"), snapshotService.calls);
        assertTrue(ended.isFinalized());

        // 이미 확정된 대회는 다시 확정하지 않는다 (백엔드 여러 대)
        assertFalse(service.finalizeIfDue());
        assertEquals(1, snapshotService.calls.size());
    }

    @Test
    void manualFinalizeRejectsRunningContest() {
        currentIs(contest(1L, START, END, null));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.finalizeContest(1L, 1L));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertTrue(snapshotService.calls.isEmpty());
    }

    private void at(LocalDateTime time) {
        service.setClock(Clock.fixed(time.atZone(ContestService.KST).toInstant(), ContestService.KST));
    }

    private void currentIs(Contest c) {
        when(contestRepository.findFirstByOrderByStartsAtDescIdDesc()).thenReturn(Optional.of(c));
        when(contestRepository.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
    }

    @SuppressWarnings("unchecked")
    private List<ContestChangeLog> savedLogs() {
        ArgumentCaptor<List<ContestChangeLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(changeLogRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private static Contest contest(Long id, LocalDateTime start, LocalDateTime end, LocalDateTime finalizedAt) {
        return Contest.builder()
                .id(id)
                .name("NIMDACON 2026")
                .startsAt(start)
                .endsAt(end)
                .cleanupInstancesOnEnd(true)
                .challengesVisibleAfterEnd(true)
                .freezeScoreboard(false)
                .freezeMinutesBeforeEnd(30)
                .finalizedAt(finalizedAt)
                .createdAt(start)
                .updatedAt(start)
                .build();
    }

    private static ContestSaveRequest request(String name, LocalDateTime start, LocalDateTime end) {
        ContestSaveRequest request = new ContestSaveRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "startsAt", start);
        ReflectionTestUtils.setField(request, "endsAt", end);
        return request;
    }

    /** Java 24에서는 Mockito가 구체 클래스를 mock하지 못해 하위 클래스로 호출만 기록한다. */
    private static class RecordingSnapshotService extends ContestSnapshotService {
        final List<String> calls = new ArrayList<>();

        RecordingSnapshotService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public void clear(Long contestId) {
            calls.add("clear:" + contestId);
        }

        @Override
        public void write(Long contestId) {
            calls.add("write:" + contestId);
        }
    }
}
