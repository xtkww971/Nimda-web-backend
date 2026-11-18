package com.nimda.ctfgame.controller;

import com.nimda.ctfgame.service.GameItemService;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.group.service.CtfTeamResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임(game.nimda.kr)이 부르는 물건 목록.
 * <pre>
 *   GET /api/ctf/game/items  -> 보이는 물건, 위치, 연결 문제 코드, 팀 소유 여부, (가졌으면) 이야기
 * </pre>
 * 권한은 SecurityConfig: ROLE_CTF_USER·CTF_MASTER·ADMIN·DEV (게임 입장 역할과 같다).
 * nimda.ctf-game.enabled=false 면 이 컨트롤러가 만들어지지 않아 404 가 된다.
 */
@RestController
@RequestMapping(GameItemController.ITEMS_URL)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nimda.ctf-game", name = "enabled", havingValue = "true")
public class GameItemController {

    public static final String ITEMS_URL = "/api/ctf/game/items";

    private final GameItemService service;

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok(service.listFor(user.getUser().getId(), CtfTeamResolver.isStaff(user))).toResponse();
    }
}
