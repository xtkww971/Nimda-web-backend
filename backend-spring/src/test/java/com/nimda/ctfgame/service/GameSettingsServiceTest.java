package com.nimda.ctfgame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimda.ctfgame.dto.GameSettingsRequest;
import com.nimda.ctfgame.dto.GameSettingsResponse;
import com.nimda.ctfgame.entity.GameSettings;
import com.nimda.ctfgame.repository.GameSettingsRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import judgeServer.domain.contest.entity.Contest;
import judgeServer.domain.contest.service.ContestService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Stage2 시간 개방: 대회 시작 + N분을 서버 시계로 판정하고, 설정이 없거나 대회가 없으면 열지 않는다. */
class GameSettingsServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 5, 13, 0);

    private final GameSettingsRepository repository = mock(GameSettingsRepository.class);
    private final ContestService contests = mock(ContestService.class);
    private final GameSettingsService service = new GameSettingsService(repository, contests);

    private void at(LocalDateTime now) {
        when(contests.now()).thenReturn(now);
    }

    private void contestStartingAt(LocalDateTime start) {
        Contest contest = mock(Contest.class);
        when(contest.getStartsAt()).thenReturn(start);
        when(contests.current()).thenReturn(Optional.of(contest));
    }

    private void saved(Integer minutes) {
        when(repository.findById(GameSettings.ID)).thenReturn(Optional.of(
                GameSettings.builder().id(GameSettings.ID).stage2OpenMinutes(minutes).updatedAt(START.minusDays(1)).updatedBy(1L).build()));
    }

    @Test
    void opensStage2AfterTheMinutesFromTheContestStart() {
        saved(90);
        contestStartingAt(START);

        at(START.plusMinutes(89).plusSeconds(59));
        GameSettingsResponse before = service.forPlayers();
        assertEquals(90, before.stage2OpenMinutes());
        assertEquals("2026-12-05T14:30+09:00", before.stage2OpenAt().toString(), "시작 13:00 + 90분, KST");
        assertFalse(before.stage2Open(), "1초 전에는 아직");

        at(START.plusMinutes(90));
        assertTrue(service.forPlayers().stage2Open(), "그 시각부터 열린다");
    }

    @Test
    void noTimedOpeningWhenTheSettingIsEmpty() {
        saved(null);
        contestStartingAt(START);
        at(START.plusHours(10));

        GameSettingsResponse response = service.forPlayers();
        assertNull(response.stage2OpenMinutes());
        assertNull(response.stage2OpenAt());
        assertFalse(response.stage2Open());
    }

    @Test
    void noTimedOpeningWithoutAContest() {
        saved(90);
        when(contests.current()).thenReturn(Optional.empty());
        at(START.plusHours(10));

        GameSettingsResponse response = service.forPlayers();
        assertEquals(90, response.stage2OpenMinutes(), "설정 값은 그대로 보여 준다");
        assertNull(response.stage2OpenAt(), "대회가 없으면 열 시각이 없다");
        assertFalse(response.stage2Open());
    }

    @Test
    void missingRowAnswersWithDefaults() {
        when(repository.findById(GameSettings.ID)).thenReturn(Optional.empty());
        contestStartingAt(START);
        at(START.plusHours(1));

        GameSettingsResponse response = service.forPlayers();
        assertNull(response.stage2OpenMinutes());
        assertFalse(response.stage2Open());
    }

    @Test
    void playersDoNotSeeWhenItWasChangedButAdminsDo() {
        saved(90);
        contestStartingAt(START);
        at(START);

        assertNull(service.forPlayers().updatedAt());
        assertEquals("2026-12-04T13:00+09:00", service.forAdmin().updatedAt().toString());
    }

    @Test
    void adminCanSetAndClearTheMinutes() {
        saved(null);
        contestStartingAt(START);
        at(START.plusMinutes(30));
        when(repository.save(any(GameSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        GameSettingsResponse set = service.update(new GameSettingsRequest(20), 7L);
        assertEquals(20, set.stage2OpenMinutes());
        assertTrue(set.stage2Open(), "시작 30분이 지났으니 20분 개방은 이미 열림");
        ArgumentCaptor<GameSettings> captor = ArgumentCaptor.forClass(GameSettings.class);
        verify(repository).save(captor.capture());
        assertEquals(7L, captor.getValue().getUpdatedBy());
        assertEquals(START.plusMinutes(30), captor.getValue().getUpdatedAt());

        GameSettingsResponse cleared = service.update(new GameSettingsRequest(null), 7L);
        assertNull(cleared.stage2OpenMinutes(), "null 이면 시간 개방을 끈다");
        assertFalse(cleared.stage2Open());
    }
}
