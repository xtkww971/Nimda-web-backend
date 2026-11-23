package com.nimda.ctfgame.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import com.nimda.ctfgame.dto.GameSettingsRequest;
import com.nimda.ctfgame.service.GameSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 설정 관리 (운영진).
 * <pre>
 *   GET /api/ctf/game/admin/settings  -> 지금 설정 + 바꾼 때
 *   PUT /api/ctf/game/admin/settings  { "stage2OpenMinutes": 90 }   (null 이면 시간 개방 끄기)
 * </pre>
 * 권한은 SecurityConfig: ROLE_ADMIN·ROLE_DEV (게임 물건 관리와 같다).
 */
@RestController
@RequestMapping(AdminGameSettingsController.ADMIN_SETTINGS_URL)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nimda.ctf-game", name = "enabled", havingValue = "true")
public class AdminGameSettingsController {

    public static final String ADMIN_SETTINGS_URL = "/api/ctf/game/admin/settings";

    private final GameSettingsService service;

    @GetMapping
    public ResponseEntity<?> get() {
        return ApiResponse.ok(service.forAdmin()).toResponse();
    }

    @PutMapping
    public ResponseEntity<?> update(@Valid @RequestBody GameSettingsRequest request,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok("게임 설정을 바꿨습니다.", service.update(request, user.getUser().getId())).toResponse();
    }
}
