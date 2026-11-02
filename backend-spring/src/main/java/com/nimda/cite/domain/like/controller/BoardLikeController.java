package com.nimda.cite.domain.like.controller;

import com.nimda.cite.domain.board.dto.BoardResponseDTO;
import com.nimda.cite.domain.board.entity.Board;
import com.nimda.cite.domain.comment.enums.STATUS;
import com.nimda.cite.domain.comment.repository.CommentRepository;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.domain.like.dto.BoardLikeCountResponseDTO;
import com.nimda.cite.domain.like.dto.BoardLikeListResponseDTO;
import com.nimda.cite.domain.like.dto.BoardLikeResponse;
import com.nimda.cite.domain.like.service.BoardLikeService;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.service.UserService;
import com.nimda.cite.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 매핑 이름 바꿔야 함

@RestController
@RequestMapping(UrlNames.boardLikeUrl)
@RequiredArgsConstructor
public class BoardLikeController {

    private final BoardLikeService boardLikeService;
    private final CommentRepository commentRepository;
    private final UserService userService;

    /**
     * 타인의 좋아요한 게시글 목록 조회 (공개 프로필용, 인증 불필요)
     * GET /api/like/board/user/{nickname}/liked
     */
    @GetMapping(UrlNames.getLikedBoardsByNicknameUrl)
    public ResponseEntity<?> getLikedBoardsByNickname(@PathVariable String nickname) {
        User user = userService.getUserByNickname(nickname);
        List<Board> boards = boardLikeService.getTotalLikeBoards(user.getId());
        List<BoardResponseDTO> dtos = boards.stream()
                .map(b -> BoardResponseDTO.from(b,
                        boardLikeService.getLikeCount(b.getId()),
                        false,
                        commentRepository.countByBoardIdAndStatusNot(b.getId(), STATUS.DELETED)))
                .toList();
        return ApiResponse.ok(BoardLikeListResponseDTO.of(dtos)).toResponse();
    }

    // 게시글 내에서 표기할 좋아요 개수와 좋아요 여부
    @GetMapping(UrlNames.getLikeStatusUrl)
    public ResponseEntity<?> getLikeStatus(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        long count = boardLikeService.getLikeCount(boardId);
        boolean isLiked = boardLikeService.isUserLiked(userId, boardId);
        BoardLikeResponse dto = BoardLikeResponse.builder().likeCount(count).isLiked(isLiked).build();
        return ApiResponse.ok(dto).toResponse();
    }

    @GetMapping(UrlNames.getBoardLikeCountUrl)
    public ResponseEntity<?> getLikeCount(@PathVariable Long boardId) {
        long likeCount = boardLikeService.getLikeCount(boardId);
        return ApiResponse.ok(BoardLikeResponse.builder().likeCount(likeCount).build()).toResponse();
    }

    @PostMapping(UrlNames.toggleBoardLikeUrl)
    public ResponseEntity<?> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId) {

        Long userId = userDetails.getUser().getId();
        String message = boardLikeService.toggleLike(userId, boardId);
        long likeCount = boardLikeService.getLikeCount(boardId);
        boolean isLiked = !message.contains("취소");
        return ApiResponse.ok(BoardLikeResponse.of(message, likeCount, isLiked)).toResponse();
    }

    @GetMapping(UrlNames.getTotalLikesReceivedUrl)
    public ResponseEntity<?> getTotalLikesReceived(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        long totalReceived = boardLikeService.getTotalLikesReceived(userId);
        BoardLikeResponse dto = BoardLikeResponse.builder().likeCount(totalReceived).build();
        return ApiResponse.ok(dto).toResponse();
    }

    @GetMapping(UrlNames.getLikeBoardsUrl)
    public ResponseEntity<?> getLikeBoards(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<Board> boards = boardLikeService.getTotalLikeBoards(userId);
        List<BoardResponseDTO> dtos = boards.stream()
                .map(b -> BoardResponseDTO.from(b,
                        boardLikeService.getLikeCount(b.getId()),
                        true,
                        commentRepository.countByBoardIdAndStatusNot(b.getId(), STATUS.DELETED)))
                .toList();
        return ApiResponse.ok(BoardLikeListResponseDTO.of(dtos)).toResponse();
    }

    @GetMapping(UrlNames.getPushedLikesCountUrl)
    public ResponseEntity<?> getPushedLikesCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();

        long count = boardLikeService.countTotalLikeBoards(userId);

        // 프론트엔드 boardLike.ts는 'result.data?.likeCount'로 읽는다.
        return ApiResponse.ok(BoardLikeCountResponseDTO.of(count)).toResponse();
    }
}
