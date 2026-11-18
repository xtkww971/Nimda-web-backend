package com.nimda.ctfgame.controller;

import com.nimda.ctfgame.service.GameItemService;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.ctfgame.dto.GameItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 물건 관리 (맵 배치·연결 문제·이야기).
 * <pre>
 *   GET    /api/ctf/game/admin/items        -> 전부 (숨긴 것, 이야기 포함)
 *   POST   /api/ctf/game/admin/items        -> 등록
 *   PUT    /api/ctf/game/admin/items/{id}   -> 수정 (key 는 그대로)
 *   DELETE /api/ctf/game/admin/items/{id}   -> 삭제
 * </pre>
 * 권한은 SecurityConfig: ROLE_ADMIN·ROLE_DEV (CTF 문제 관리와 같다).
 */
@RestController
@RequestMapping(AdminGameItemController.ADMIN_ITEMS_URL)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nimda.ctf-game", name = "enabled", havingValue = "true")
public class AdminGameItemController {

    public static final String ADMIN_ITEMS_URL = "/api/ctf/game/admin/items";

    private final GameItemService service;

    @GetMapping
    public ResponseEntity<?> list() {
        return ApiResponse.ok(service.listAll()).toResponse();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody GameItemRequest request) {
        return ApiResponse.ok("게임 물건을 만들었습니다.", service.create(request)).toResponse(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody GameItemRequest request) {
        return ApiResponse.ok("게임 물건을 고쳤습니다.", service.update(id, request)).toResponse();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("게임 물건을 지웠습니다.").toResponse();
    }
}
