package com.nimda.cite.domain.board.controller;

import com.nimda.cite.domain.attachment.service.AttachmentService;
import com.nimda.cite.domain.board.dto.BoardDeleteRequest;
import com.nimda.cite.domain.board.dto.BoardDeletedResponseDTO;
import com.nimda.cite.domain.board.dto.BoardDetailResponseDTO;
import com.nimda.cite.domain.board.dto.BoardListResponseDTO;
import com.nimda.cite.domain.board.dto.BoardPostCountResponseDTO;
import com.nimda.cite.domain.board.dto.BoardResponseDTO;
import com.nimda.cite.domain.board.dto.BoardWriteRequestDTO;
import com.nimda.cite.domain.board.dto.CategoryResponseDTO;
import com.nimda.cite.domain.board.entity.Board;
import com.nimda.cite.domain.board.entity.Category;
import com.nimda.cite.domain.board.enums.BoardStatus;
import com.nimda.cite.domain.board.repository.CategoryRepository;
import com.nimda.cite.domain.board.service.BoardService;
import com.nimda.cite.domain.comment.enums.STATUS;
import com.nimda.cite.domain.comment.repository.CommentRepository;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.s3.S3Service;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.domain.like.service.BoardLikeService;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.service.UserService;
import com.nimda.cite.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(UrlNames.boardUrl)
@RequiredArgsConstructor
public class BoardController {

    /** sort 파라미터로 허용된 Board 엔티티 필드명 화이트리스트 */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "postView", "pinned"
    );

    /** 클라이언트 sort 파라미터를 화이트리스트 기준으로 정제하고 페이지 크기를 제한 */
    private Pageable sanitizedPageable(Pageable pageable, int maxSize) {
        List<Sort.Order> safeOrders = pageable.getSort().stream()
                .filter(o -> ALLOWED_SORT_FIELDS.contains(o.getProperty()))
                .collect(Collectors.toList());
        Sort safeSort = safeOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(safeOrders);
        int size = Math.min(pageable.getPageSize(), maxSize);
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }

    private final BoardService boardService;

    private final CategoryRepository categoryRepository;

    private final UserService userService;

    private final BoardLikeService boardLikeService;

    private final AttachmentService attachmentService;

    private final CommentRepository commentRepository;

    private final S3Service s3Service;

    /** author.profileImage가 S3 키인 경우 Presigned URL로 변환 */
    private void resolveProfileImage(BoardResponseDTO dto) {
        if (s3Service == null || dto == null || dto.getAuthor() == null) return;
        String img = dto.getAuthor().getProfileImage();
        if (img != null && !img.isBlank() && !img.startsWith("http")) {
            dto.getAuthor().setProfileImage(s3Service.createPresignedGetUrl(img, 60));
        }
    }

    private void resolveProfileImages(List<BoardResponseDTO> dtos) {
        if (dtos != null) dtos.forEach(this::resolveProfileImage);
    }

    // 사용자가 해당 게시글을 좋아요 눌렀는지 확인
    private boolean isLiked(Board board, User user) {
        return user != null && boardLikeService.isUserLiked(user.getId(), board.getId());
    }

    // 게시글 상세 응답용 DTO (좋아요/댓글 수, 첨부 목록 포함)
    private BoardResponseDTO toDetailResponse(Board board, User user) {
        long likeCount = boardLikeService.getLikeCount(board.getId());
        long commentCount = commentRepository.countByBoardIdAndStatusNot(board.getId(), STATUS.DELETED);
        var attachments = attachmentService.listAttachmentsForBoard(board.getId());
        BoardResponseDTO boardDto = BoardResponseDTO.from(board, likeCount, isLiked(board, user), commentCount, attachments);
        resolveProfileImage(boardDto);
        return boardDto;
    }

    // 게시글 목록 응답용 DTO (좋아요/댓글 수 포함)
    private List<BoardResponseDTO> toListResponse(List<Board> boards, User user) {
        List<BoardResponseDTO> dtos = boards.stream()
                .map(board -> BoardResponseDTO.from(board,
                        boardLikeService.getLikeCount(board.getId()),
                        isLiked(board, user),
                        commentRepository.countByBoardIdAndStatusNot(board.getId(), STATUS.DELETED)))
                .collect(Collectors.toList());
        resolveProfileImages(dtos);
        return dtos;
    }

    private BoardListResponseDTO toPageResponse(Page<Board> boards, User user, Category category) {
        return BoardListResponseDTO.builder()
                .posts(toListResponse(boards.getContent(), user))
                .totalElements(boards.getTotalElements())
                .totalPages(boards.getTotalPages())
                .currentPage(boards.getNumber())
                .category(category != null ? CategoryResponseDTO.from(category) : null)
                .build();
    }

    @GetMapping(UrlNames.getPostsByCategoryUrl)
    public ResponseEntity<?> getPostsByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "slug", required = false) String slug,
            @RequestParam(value = "searchKeyword", required = false) String searchKeyword,
            @RequestParam(value = "includeChildren", required = false, defaultValue = "false") Boolean includeChildren,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable safePageable = sanitizedPageable(pageable, 100);
        Category category = boardService.resolveCategory(categoryId, slug);

        User user = CustomUserDetails.getLoginUserOrNull(userDetails);

        // "카르텔" 카테고리 접근 권한 확인
        boardService.checkCategoryReadable(category, user);

        Page<Board> boards;

        // includeChildren=true 이면 하위 카테고리 게시글도 함께 조회
        if (Boolean.TRUE.equals(includeChildren)) {
            List<Category> categories = new ArrayList<>();
            categories.add(category);
            // 직계 자식 카테고리 추가
            List<Category> children = categoryRepository
                    .findByParentIdAndIsActiveTrueOrderBySortOrderAsc(category.getId());
            categories.addAll(children);
            // 손자 카테고리도 추가 (3단 지원)
            for (Category child : children) {
                List<Category> grandChildren = categoryRepository
                        .findByParentIdAndIsActiveTrueOrderBySortOrderAsc(child.getId());
                categories.addAll(grandChildren);
            }

            if (searchKeyword == null || searchKeyword.isEmpty()) {
                boards = boardService.boardListByCategories(categories, safePageable);
            } else {
                boards = boardService.boardSearchListByCategories(categories, searchKeyword, safePageable);
            }
        } else {
            if (searchKeyword == null || searchKeyword.isEmpty()) {
                boards = boardService.boardListByCategory(category, safePageable);
            } else {
                boards = boardService.boardSearchListByCategory(category, searchKeyword, safePageable);
            }
        }

        // 게시글 목록에 좋아요 개수 추가하여 DTO로 변환
        BoardListResponseDTO responseDTO = toPageResponse(boards, user, category);

        return ApiResponse.ok("게시글 목록을 성공적으로 조회했습니다.", responseDTO).toResponse();
    }

    @GetMapping(UrlNames.getPinnedPostsUrl)
    public ResponseEntity<?> getPinnedPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "slug", required = false) String slug,
            @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable safePageable = sanitizedPageable(pageable, 20);
        Category category = boardService.resolveCategory(categoryId, slug);

        User user = CustomUserDetails.getLoginUserOrNull(userDetails);
        Page<Board> boards = boardService.boardListByCategoryWithPinned(category, safePageable);

        // 고정글 목록에 좋아요 개수 추가하여 DTO로 변환
        BoardListResponseDTO responseDTO = toPageResponse(boards, user, category);

        return ApiResponse.ok("고정글 목록을 성공적으로 조회했습니다.", responseDTO).toResponse();
    }

    @GetMapping(UrlNames.getPopularPostsUrl)
    public ResponseEntity<?> getPopularPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "slug", required = false) String slug,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // 인기글은 최대 10개로 제한, sort 화이트리스트 적용
        Pageable limitedPageable = sanitizedPageable(pageable, 10);

        User user = CustomUserDetails.getLoginUserOrNull(userDetails);
        Page<Board> boards;
        Category category = null;

        if (categoryId != null || slug != null) {
            category = boardService.resolveCategory(categoryId, slug);
            boards = boardService.boardListPopularByCategory(category, limitedPageable);
        } else {
            boards = boardService.boardListPopular(limitedPageable);
        }

        // 인기글 목록에 좋아요 개수 추가하여 DTO로 변환
        BoardListResponseDTO responseDTO = toPageResponse(boards, user, category);

        return ApiResponse.ok("인기글 목록을 성공적으로 조회했습니다.", responseDTO).toResponse();
    }

    @PostMapping(UrlNames.writeBoardUrl)
    public ResponseEntity<?> write(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "attachmentIds", required = false) List<Long> attachmentIds,
            @RequestParam(value = "pinned", required = false) Boolean pinned,
            @RequestParam(value = "itemPrice", required = false) Long itemPrice,
            @RequestParam(value = "itemType", required = false) String itemType,
            @RequestParam(value = "profileDecorationId", required = false) Long profileDecorationId,
            @RequestParam(value = "thumbnailAttachmentId", required = false) Long thumbnailAttachmentId) {
        User author = CustomUserDetails.getLoginUser(userDetails);

        BoardWriteRequestDTO request = BoardWriteRequestDTO.builder()
                .categoryId(categoryId)
                .title(title)
                .content(content)
                .tagId(tagId)
                .attachmentIds(attachmentIds)
                .pinned(pinned)
                .itemPrice(itemPrice)
                .itemType(itemType)
                .profileDecorationId(profileDecorationId)
                .thumbnailAttachmentId(thumbnailAttachmentId)
                .build();

        Board board = boardService.writeBoard(author, request);

        long likeCount = boardLikeService.getLikeCount(board.getId());
        var attachments = attachmentService.listAttachmentsForBoard(board.getId());
        BoardResponseDTO boardDto = BoardResponseDTO.from(board, likeCount, false, 0L, attachments);
        resolveProfileImage(boardDto);

        return ApiResponse.ok("게시글이 성공적으로 작성되었습니다.", BoardDetailResponseDTO.of(boardDto))
                .toResponse(HttpStatus.CREATED);
    }

    @GetMapping(UrlNames.viewBoardUrl)
    public ResponseEntity<?> view(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = CustomUserDetails.getLoginUserOrNull(userDetails);
        Board board = boardService.viewBoard(id, user);

        // 삭제된 게시글인 경우
        if (board.getStatus() == BoardStatus.DELETED) {
            return ApiResponse.ok("삭제된 게시글입니다.", BoardDeletedResponseDTO.of()).toResponse();
        }

        return ApiResponse.ok("게시글을 성공적으로 조회했습니다.",
                BoardDetailResponseDTO.of(toDetailResponse(board, user))).toResponse();
    }

    @PutMapping(UrlNames.updateBoardUrl)
    public ResponseEntity<?> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "attachmentIds", required = false) List<Long> attachmentIds,
            @RequestParam(value = "pinned", required = false) Boolean pinned,
            @RequestParam(value = "itemPrice", required = false) Long itemPrice,
            @RequestParam(value = "itemType", required = false) String itemType,
            @RequestParam(value = "profileDecorationId", required = false) Long profileDecorationId,
            @RequestParam(value = "thumbnailAttachmentId", required = false) Long thumbnailAttachmentId) {
        User currentUser = CustomUserDetails.getLoginUser(userDetails);

        BoardWriteRequestDTO request = BoardWriteRequestDTO.builder()
                .categoryId(categoryId)
                .title(title)
                .content(content)
                .tagId(tagId)
                .attachmentIds(attachmentIds)
                .pinned(pinned)
                .itemPrice(itemPrice)
                .itemType(itemType)
                .profileDecorationId(profileDecorationId)
                .thumbnailAttachmentId(thumbnailAttachmentId)
                .build();

        Board board = boardService.updateBoard(id, currentUser, request);

        return ApiResponse.ok("게시글이 성공적으로 수정되었습니다.",
                BoardDetailResponseDTO.of(toDetailResponse(board, currentUser))).toResponse();
    }

    @DeleteMapping(UrlNames.deleteBoardUrl)
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User currentUser = CustomUserDetails.getLoginUser(userDetails);

        boardService.deleteBoard(id, currentUser);
        return ApiResponse.ok("게시글이 성공적으로 삭제되었습니다.").toResponse();
    }

    @PatchMapping(UrlNames.togglePinUrl)
    public ResponseEntity<?> togglePin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User currentUser = CustomUserDetails.getLoginUser(userDetails);

        // 관리자만 고정/해제 가능 (권한 체크는 서비스에서 진행)
        Board board = boardService.toggleBoardPin(id, currentUser);
        String message = board.getPinned() ? "게시글이 고정되었습니다." : "게시글 고정이 해제되었습니다.";

        return ApiResponse.ok(message, BoardDetailResponseDTO.of(toDetailResponse(board, currentUser))).toResponse();
    }

    @GetMapping(UrlNames.getMyBoardStatsUrl)
    public ResponseEntity<?> getMyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = CustomUserDetails.getLoginUser(userDetails);

        long postCount = boardService.countByAuthor(currentUser);

        return ApiResponse.ok("통계 정보를 성공적으로 조회했습니다.", BoardPostCountResponseDTO.of(postCount)).toResponse();
    }

    /**
     * 특정 유저가 작성한 게시글 목록 조회 (공개 프로필용)
     */
    @GetMapping(UrlNames.getBoardsByNicknameUrl)
    public ResponseEntity<?> getBoardsByNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String nickname) {
        User targetUser = userService.getUserByNickname(nickname);

        User user = CustomUserDetails.getLoginUserOrNull(userDetails);
        List<BoardResponseDTO> dtos = toListResponse(boardService.getMyBoards(targetUser), user);

        return ApiResponse.ok("게시글 목록을 조회했습니다.", dtos).toResponse();
    }

    /**
     * 내가 작성한 게시글 목록 조회
     */
    @GetMapping(UrlNames.getMyBoardsUrl)
    public ResponseEntity<?> getMyBoards(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = CustomUserDetails.getLoginUser(userDetails);

        List<BoardResponseDTO> dtos = toListResponse(boardService.getMyBoards(currentUser), currentUser);

        return ApiResponse.ok("내 게시글 목록을 조회했습니다.", dtos).toResponse();
    }

    /**
     * 내가 작성한 게시글 삭제 (복수)
     */
    @DeleteMapping(UrlNames.deleteMyBoardsUrl)
    public ResponseEntity<?> deleteMyBoards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody BoardDeleteRequest request) {
        User currentUser = CustomUserDetails.getLoginUser(userDetails);

        boardService.deleteMyBoards(request.getBoardIds(), currentUser);
        return ApiResponse.ok("게시글이 삭제되었습니다.", null).toResponse();
    }

    /**
     * 내가 댓글 단 게시글 목록 조회
     */
    @GetMapping(UrlNames.getMyCommentedBoardsUrl)
    public ResponseEntity<?> getMyCommentedBoards(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = CustomUserDetails.getLoginUser(userDetails);

        List<Long> boardIds = commentRepository.findDistinctBoardIdsByAuthor(
                currentUser, List.of(STATUS.DELETED));

        List<Board> boards = new ArrayList<>();
        for (Long boardId : boardIds) {
            boardService.findById(boardId).ifPresent(boards::add);
        }

        return ApiResponse.ok("댓글 단 게시글 목록을 조회했습니다.", toListResponse(boards, currentUser)).toResponse();
    }

    @GetMapping(UrlNames.getRecentBoardsUrl)
    public ResponseEntity<?> getRecentBoards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Pageable recentPageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 10),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardService.boardList(recentPageable);
        User user = CustomUserDetails.getLoginUserOrNull(userDetails);

        BoardListResponseDTO responseDTO = toPageResponse(boards, user, null);

        return ApiResponse.ok("최신글 목록을 성공적으로 조회했습니다.", responseDTO).toResponse();
    }

}
