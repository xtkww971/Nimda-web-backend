package com.nimda.cite.domain.board.controller;

import com.nimda.cite.domain.board.dto.BoardTagStatsResponseDTO;
import com.nimda.cite.domain.board.dto.BoardTagStatusUpdateResponseDTO;
import com.nimda.cite.domain.board.service.BoardService;
import com.nimda.cite.common.util.UrlNames;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자용 게시글 관리 컨트롤러
 * - 기존 응답 모양(최상위 success/message 필드)을 유지하기 위해 ApiResponse로 감싸지 않는다.
 */
@RestController
@RequestMapping(UrlNames.adminBoardUrl)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBoardController {

    private final BoardService boardService;

    /**
     * 카테고리의 태그별 게시글 통계 조회
     */
    @GetMapping(UrlNames.getTagStatsUrl)
    public ResponseEntity<BoardTagStatsResponseDTO> getTagStats(@RequestParam Long categoryId) {
        return ResponseEntity.ok(boardService.getTagStats(categoryId));
    }

    /**
     * 태그 기반 게시글 비활성화 (ACTIVE → HIDDEN)
     */
    @PutMapping(UrlNames.deactivateByTagUrl)
    public ResponseEntity<BoardTagStatusUpdateResponseDTO> deactivateByTag(@RequestParam Long categoryId, @RequestParam Long tagId) {
        int count = boardService.hideBoardsByTag(categoryId, tagId);

        return ResponseEntity.ok(BoardTagStatusUpdateResponseDTO.of(
                "tagId=" + tagId + " 태그의 게시글 " + count + "건이 비활성화되었습니다.", count));
    }

    /**
     * 태그 기반 게시글 활성화 (HIDDEN → ACTIVE)
     */
    @PutMapping(UrlNames.activateByTagUrl)
    public ResponseEntity<BoardTagStatusUpdateResponseDTO> activateByTag(@RequestParam Long categoryId, @RequestParam Long tagId) {
        int count = boardService.activateBoardsByTag(categoryId, tagId);

        return ResponseEntity.ok(BoardTagStatusUpdateResponseDTO.of(
                "tagId=" + tagId + " 태그의 게시글 " + count + "건이 활성화되었습니다.", count));
    }
}
