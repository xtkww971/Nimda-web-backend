package com.nimda.ctfgame.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.ctfgame.service.GameSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임(game.nimda.kr)이 부르는 게임 설정.
 * <pre>
 *   GET /api/ctf/game/settings  -> { stage2OpenMinutes, stage2OpenAt, stage2Open, serverTime }
 * </pre>
 * 권한은 SecurityConfig: ROLE_CTF_USER·CTF_MASTER·ADMIN·DEV (게임 입장 역할과 같다).
 * nimda.ctf-game.enabled=false 면 만들어지지 않아 404 가 된다.
 */
@RestController
@RequestMapping(GameSettingsController.SETTINGS_URL)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nimda.ctf-game", name = "enabled", havingValue = "true")
public class GameSettingsController {

    public static final String SETTINGS_URL = "/api/ctf/game/settings";

    private final GameSettingsService service;

    @GetMapping
    public ResponseEntity<?> get() {
        return ApiResponse.ok(service.forPlayers()).toResponse();
    }
}
