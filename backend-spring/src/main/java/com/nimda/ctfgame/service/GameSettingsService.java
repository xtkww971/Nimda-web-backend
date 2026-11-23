package com.nimda.ctfgame.service;

import com.nimda.ctfgame.dto.GameSettingsRequest;
import com.nimda.ctfgame.dto.GameSettingsResponse;
import com.nimda.ctfgame.entity.GameSettings;
import com.nimda.ctfgame.repository.GameSettingsRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import judgeServer.domain.contest.entity.Contest;
import judgeServer.domain.contest.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 설정. 지금은 Stage2 시간 개방 (기획안 8장: 시작 N분 뒤 기념품을 다 모으지 못한 팀에게도 Stage2 접근 허용).
 * 시각 판정은 대회와 같은 서버 시계(KST, ContestService.now)로 한다. 대회 일정은 읽기만 한다 (judgeServer 는 고치지 않는다).
 * 시간 개방은 문만 연다: 아이템·점수·해결 처리를 주지 않는다 (그건 게임 화면과 정답 기록이 정한다).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nimda.ctf-game", name = "enabled", havingValue = "true")
public class GameSettingsService {

    private final GameSettingsRepository repository;
    private final ContestService contestService;

    /** 참가자(게임)용: 바꾼 때·바꾼 사람은 주지 않는다 */
    @Transactional(readOnly = true)
    public GameSettingsResponse forPlayers() {
        return view(load(), false);
    }

    /** 운영진용: 바꾼 때까지 */
    @Transactional(readOnly = true)
    public GameSettingsResponse forAdmin() {
        return view(load(), true);
    }

    @Transactional
    public GameSettingsResponse update(GameSettingsRequest request, Long adminUserId) {
        GameSettings settings = repository.findById(GameSettings.ID)
                .orElseGet(() -> GameSettings.defaults(contestService.now()));
        settings.changeStage2OpenMinutes(request.stage2OpenMinutes(), contestService.now(), adminUserId);
        return view(repository.save(settings), true);
    }

    private GameSettings load() {
        // 마이그레이션이 한 줄을 넣어 두지만, 없더라도 기본값(시간 개방 없음)으로 답한다
        return repository.findById(GameSettings.ID).orElseGet(() -> GameSettings.defaults(contestService.now()));
    }

    private GameSettingsResponse view(GameSettings settings, boolean admin) {
        LocalDateTime now = contestService.now();
        Integer minutes = settings.getStage2OpenMinutes();
        LocalDateTime openAt = minutes == null ? null
                : contestService.current().map(Contest::getStartsAt).map(start -> start.plusMinutes(minutes)).orElse(null);
        boolean open = openAt != null && !now.isBefore(openAt);
        return new GameSettingsResponse(minutes, kst(openAt), open, kst(now), admin ? kst(settings.getUpdatedAt()) : null);
    }

    private static OffsetDateTime kst(LocalDateTime time) {
        return time == null ? null : time.atZone(ContestService.KST).toOffsetDateTime();
    }
}
