package com.nimda.cite.domain.comment.controller;

import com.nimda.cite.domain.comment.dto.*;
import com.nimda.cite.domain.comment.service.CommentService;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.s3.S3Service;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(UrlNames.commentUrl)
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private final S3Service s3Service;

    private void resolveCommentProfileImages(List<CommentResponse> comments) {
        if (s3Service == null || comments == null) return;
        for (CommentResponse c : comments) {
            String img = c.getAuthorProfileImage();
            if (img != null && !img.isBlank() && !img.startsWith("http")) {
                c.setAuthorProfileImage(s3Service.createPresignedGetUrl(img, 60));
            }
            resolveCommentProfileImages(c.getChildren());
        }
    }

    private void resolveCommentProfileImage(CommentResponse c) {
        if (s3Service == null || c == null) return;
        String img = c.getAuthorProfileImage();
        if (img != null && !img.isBlank() && !img.startsWith("http")) {
            c.setAuthorProfileImage(s3Service.createPresignedGetUrl(img, 60));
        }
        resolveCommentProfileImages(c.getChildren());
    }

    /**
     * 특정 게시글 댓글 생성
     * POST /api/board/{boardId}/comments
     */
    @PostMapping(UrlNames.createCommentUrl)
    public ResponseEntity<ApiResponse<CommentDetailResponseDTO>> createComment(
            @PathVariable Long boardId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();
        CommentResponse response = commentService.createComment(boardId, request, userId);
        resolveCommentProfileImage(response);
        return ApiResponse.ok("댓글이 성공적으로 작성되었습니다.",
                CommentDetailResponseDTO.of(response)).toResponse(HttpStatus.CREATED);
    }


    /**
     * 특정 게시글 댓글 조회
     * GET /api/board/{boardId}/comments
     */
    @GetMapping(UrlNames.getCommentsUrl)
    public ResponseEntity<ApiResponse<CommentListResponseDTO>> getComments(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        // 댓글 조회
        List<CommentResponse> comments = commentService.getComments(boardId, userId, isAdmin);
        resolveCommentProfileImages(comments);
        return ApiResponse.ok("댓글을 성공적으로 조회했습니다.",
                CommentListResponseDTO.of(comments)).toResponse();
    }

    /**
     * 마이페이지 작성 댓글 조회
     * GET /api/my-page/comments
     */
    @GetMapping(UrlNames.getMyCommentsUrl)
    public ResponseEntity<ApiResponse<MyCommentListResponseDTO>> getMyComments(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();
        return ApiResponse.ok("댓글을 성공적으로 조회했습니다.",
                MyCommentListResponseDTO.of(commentService.getMyComments(userId))).toResponse();
    }


    /**
     * 댓글 수정
     * PATCH /api/comments/{commentId}
     */
    @PatchMapping(UrlNames.updateCommentUrl)
    public ResponseEntity<ApiResponse<CommentDetailResponseDTO>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();
        CommentResponse response = commentService.updateComment(commentId, request, userId);
        resolveCommentProfileImage(response);
        return ApiResponse.ok("댓글을 성공적으로 수정했습니다.",
                CommentDetailResponseDTO.of(response)).toResponse();
    }

    /**
     * 댓글 숨김
     * PATCH /api/comments/{commentId}/status
     */
    @PatchMapping(UrlNames.hideCommentUrl)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentDetailResponseDTO>> hideComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CommentResponse response = commentService.updateCommentStatus(commentId, request);
        resolveCommentProfileImage(response);
        return ApiResponse.ok("댓글을 성공적으로 숨겼습니다.",
                CommentDetailResponseDTO.of(response)).toResponse();
    }



    /**
     * 댓글 삭제 (소프트 삭제)
     * DELETE /api/comments/{commentId}
     */
    @DeleteMapping(UrlNames.deleteCommentUrl)
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();
        commentService.deleteComment(commentId, userId);
        return ApiResponse.<Void>ok("댓글이 성공적으로 삭제되었습니다.").toResponse();
    }

    /**
     * 내가 작성한 댓글 개수 조회
     * GET /api/comments/my/count
     */
    @GetMapping(UrlNames.getMyCommentCountUrl)
    public ResponseEntity<ApiResponse<CommentCountResponseDTO>> getMyCommentCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();

        // Service에서 해당 유저의 댓글 개수 조회
        long commentCount = commentService.countByUserId(userId);

        return ApiResponse.ok("댓글 개수를 성공적으로 조회했습니다.",
                CommentCountResponseDTO.of(commentCount)).toResponse();
    }

    /**
     * 마이페이지 선택 댓글 삭제
     * DELETE /api/my-page/comments
     */
    @DeleteMapping(UrlNames.deleteMyCommentsUrl)
    public ResponseEntity<ApiResponse<Void>> deleteMyComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MyCommentsDeleteRequest request
    ) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();
        commentService.deleteMyComments(request.getCommentIds(), userId);
        return ApiResponse.<Void>ok("선택한 댓글이 성공적으로 삭제되었습니다.").toResponse();
    }

    /**
     * 특정 유저의 작성 댓글 목록 조회 (공개 프로필용, 인증 불필요)
     * GET /api/comments/user/{nickname}
     */
    @GetMapping(UrlNames.getCommentsByNicknameUrl)
    public ResponseEntity<ApiResponse<MyCommentListResponseDTO>> getCommentsByNickname(@PathVariable String nickname) {
        List<MyCommentResponse> comments = commentService.getCommentsByNickname(nickname);
        return ApiResponse.ok("댓글 목록을 조회했습니다.",
                MyCommentListResponseDTO.of(comments)).toResponse();
    }
}
