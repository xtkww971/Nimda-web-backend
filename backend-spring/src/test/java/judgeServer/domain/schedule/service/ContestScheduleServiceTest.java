package judgeServer.domain.schedule.service;

import judgeServer.domain.schedule.dto.AdminScheduleResponse;
import judgeServer.domain.schedule.dto.ScheduleUpdateRequest;
import judgeServer.domain.schedule.entity.ContestSchedule;
import judgeServer.domain.schedule.entity.ContestScheduleHistory;
import judgeServer.domain.schedule.enums.ContestPhase;
import judgeServer.domain.schedule.repository.ContestScheduleHistoryRepository;
import judgeServer.domain.schedule.repository.ContestScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContestScheduleServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 3, 21, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 3, 22, 18, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 21, 10, 35);

    private final ContestScheduleRepository scheduleRepository = mock(ContestScheduleRepository.class);
    private final ContestScheduleHistoryRepository historyRepository = mock(ContestScheduleHistoryRepository.class);
    private final ContestScheduleService service = new ContestScheduleService(scheduleRepository, historyRepository);

    @BeforeEach
    void fixClock() {
        service.setClock(Clock.fixed(NOW.atZone(ContestScheduleService.KST).toInstant(), ContestScheduleService.KST));
    }

    @Test
    void phaseBoundaries() {
        ContestSchedule s = schedule(START, END);
        assertEquals(ContestPhase.UPCOMING, s.phaseAt(START.minusNanos(1)));
        assertEquals(ContestPhase.RUNNING, s.phaseAt(START));
        assertEquals(ContestPhase.RUNNING, s.phaseAt(END.minusNanos(1)));
        assertEquals(ContestPhase.ENDED, s.phaseAt(END));
    }

    @Test
    void noScheduleMeansNotScheduled() {
        when(scheduleRepository.findById(ContestSchedule.SINGLETON_ID)).thenReturn(Optional.empty());

        assertEquals(ContestPhase.NOT_SCHEDULED, service.phaseAt(NOW));
        assertEquals(ContestPhase.NOT_SCHEDULED, service.getSchedule().status());
        assertNotNull(service.getSchedule().serverTime());
    }

    @Test
    void endBeforeStartIsRejected() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.update(request(END, START), 1L));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void firstSaveCreatesScheduleWithDefaultsAndCreatedHistory() {
        when(scheduleRepository.findByIdForUpdate(ContestSchedule.SINGLETON_ID)).thenReturn(Optional.empty());

        service.update(request(START, END), 1L);

        ArgumentCaptor<ContestSchedule> saved = ArgumentCaptor.forClass(ContestSchedule.class);
        verify(scheduleRepository).save(saved.capture());
        assertTrue(saved.getValue().getCleanupInstancesOnEnd());
        assertTrue(saved.getValue().getChallengesVisibleAfterEnd());
        assertFalse(saved.getValue().getFreezeScoreboard());
        assertEquals(30, saved.getValue().getFreezeMinutesBeforeEnd());

        List<ContestScheduleHistory> history = capturedHistory();
        assertEquals(1, history.size());
        assertEquals("CREATED", history.get(0).getField());
        assertEquals("2026-03-21 10:00 ~ 2026-03-22 18:00", history.get(0).getNewValue());
    }

    @Test
    void extendingEndRecordsOnlyChangedFields() {
        ContestSchedule existing = schedule(START, LocalDateTime.of(2026, 3, 22, 17, 0));
        when(scheduleRepository.findByIdForUpdate(ContestSchedule.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(scheduleRepository.findById(ContestSchedule.SINGLETON_ID)).thenReturn(Optional.of(existing));

        AdminScheduleResponse response = service.update(request(START, END), 1L);

        List<ContestScheduleHistory> history = capturedHistory();
        assertEquals(1, history.size());
        assertEquals("ENDS_AT", history.get(0).getField());
        assertEquals("2026-03-22 17:00", history.get(0).getOldValue());
        assertEquals("2026-03-22 18:00", history.get(0).getNewValue());
        assertEquals(END, existing.getEndsAt());
        assertEquals(ContestPhase.RUNNING, response.status());
    }

    @SuppressWarnings("unchecked")
    private List<ContestScheduleHistory> capturedHistory() {
        ArgumentCaptor<List<ContestScheduleHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(historyRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private static ContestSchedule schedule(LocalDateTime start, LocalDateTime end) {
        return ContestSchedule.builder()
                .id(ContestSchedule.SINGLETON_ID)
                .startsAt(start)
                .endsAt(end)
                .cleanupInstancesOnEnd(true)
                .challengesVisibleAfterEnd(true)
                .freezeScoreboard(false)
                .freezeMinutesBeforeEnd(30)
                .updatedAt(start)
                .build();
    }

    private static ScheduleUpdateRequest request(LocalDateTime start, LocalDateTime end) {
        ScheduleUpdateRequest request = new ScheduleUpdateRequest();
        ReflectionTestUtils.setField(request, "startsAt", start);
        ReflectionTestUtils.setField(request, "endsAt", end);
        return request;
    }
}
