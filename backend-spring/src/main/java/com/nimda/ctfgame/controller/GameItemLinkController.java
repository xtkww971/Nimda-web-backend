package com.nimda.ctfgame.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.ctfgame.dto.GameItemLinkRequest;
import com.nimda.ctfgame.service.GameItemLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문제 쪽에서 게임 물건 연결 (CTF 사이트 문제 관리 화면). 명세: docs/api/CTF 게임 물건 연결 API.md
 * <pre>
 *   GET    /api/ctf/game/admin/links                          -> 문제별 연결된 물건 + 장식 물건
 *   GET    /api/ctf/game/admin/challenges/{code}/items        -> 한 문제에 연결된 물건
 *   PUT    /api/ctf/game/admin/challenges/{code}/items/{key}  -> key 의 물건을 이 문제에 연결 (없으면 만든다: 201)
 *   DELETE /api/ctf/game/admin/challenges/{code}/items/{key}  -> 연결 해제 (물건은 남는다)
 * </pre>
 * 권한은 SecurityConfig 의 /api/ctf/game/admin/** (ADMIN·DEV).
 */
@RestController
@RequestMapping("/api/ctf/game/admin")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nimda.ctf-game", name = "enabled", havingValue = "true")
public class GameItemLinkController {

    private final GameItemLinkService service;

    @GetMapping("/links")
    public ResponseEntity<?> links() {
        return ApiResponse.ok(service.links()).toResponse();
    }

    @GetMapping("/challenges/{code}/items")
    public ResponseEntity<?> itemsOf(@PathVariable String code) {
        return ApiResponse.ok(service.itemsOf(code)).toResponse();
    }

    @PutMapping("/challenges/{code}/items/{key}")
    public ResponseEntity<?> link(@PathVariable String code, @PathVariable String key,
                                  @Valid @RequestBody GameItemLinkRequest request) {
        GameItemLinkService.LinkResult result = service.link(code, key, request);
        return result.created()
                ? ApiResponse.ok("게임 물건을 만들어 연결했습니다.", result.item()).toResponse(HttpStatus.CREATED)
                : ApiResponse.ok("게임 물건을 연결했습니다.", result.item()).toResponse();
    }

    @DeleteMapping("/challenges/{code}/items/{key}")
    public ResponseEntity<?> unlink(@PathVariable String code, @PathVariable String key) {
        return ApiResponse.ok("연결을 해제했습니다.", service.unlink(code, key)).toResponse();
    }
}
