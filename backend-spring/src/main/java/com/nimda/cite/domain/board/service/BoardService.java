package com.nimda.cite.domain.board.service;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.NotIncludeRoleException;
import com.nimda.cite.domain.attachment.service.AttachmentService;
import com.nimda.cite.domain.alarm.service.AlarmService;
import com.nimda.cite.domain.board.dto.BoardTagStatsResponseDTO;
import com.nimda.cite.domain.board.dto.BoardWriteRequestDTO;
import com.nimda.cite.domain.board.entity.Board;
import com.nimda.cite.domain.board.entity.Category;
import com.nimda.cite.domain.board.enums.BoardStatus;
import com.nimda.cite.domain.board.enums.ShopItemType;
import com.nimda.cite.domain.board.repository.BoardRepository;
import com.nimda.cite.domain.board.repository.CategoryRepository;
import com.nimda.cite.domain.comment.repository.CommentRepository;
import com.nimda.cite.domain.like.repository.BoardLikeRepository;
import com.nimda.cite.domain.profiledecoration.entity.ProfileDecoration;
import com.nimda.cite.domain.profiledecoration.repository.ProfileDecorationRepository;
import com.nimda.cite.domain.tag.entity.Tag;
import com.nimda.cite.domain.tag.repository.TagRepository;
import com.nimda.cite.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private BoardLikeRepository boardLikeRepository;
    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private ProfileDecorationRepository profileDecorationRepository;

    /**
     * 게시글 작성/수정
     *
     * @param attachmentIds 신규: presiggned 등록 후 ID 목록. 수정: 최종 첨부 ID 목록(동기화). null이면 첨부 변경 없음(수정 시).
     */
    @Transactional
    public void write(Board board, User author, List<Long> attachmentIds) {

        boolean isNew = board.getId() == null;

        board.setAuthor(author);

        if (board.getPostView() == null) {
            board.setPostView(0);
        }
        if (board.getPinned() == null) {
            board.setPinned(false);
        }

        boardRepository.save(board);

        Long categoryId = board.getCategory() != null ? board.getCategory().getId() : null;
        if (attachmentIds != null) {
            if (isNew) {
                attachmentService.linkAttachmentsToBoard(attachmentIds, board.getId(), categoryId, author.getId());
            } else {
                attachmentService.syncAttachmentsForBoard(board.getId(), attachmentIds, categoryId, author.getId());
            }
        }

        // 2. 공지 알림 전송 (기존 main 기능)
        if (board.getCategory() != null && board.getCategory().getName().contains("공지")) {
            alarmService.sendNoticeToAll(board.getId(), board.getTitle(), author.getId());

        }
    }

    /**
     * 게시글 작성 (권한/상점 상품 검증 포함)
     */
    @Transactional
    public Board writeBoard(User author, BoardWriteRequestDTO request) {
        Category category = findCategory(request.getCategoryId());

        // validation. tagId가 있으면 해당 카테고리 소속인지 검증
        Tag tagEntity = resolveTag(request.getTagId(), category);

        boolean isAdmin = hasRole(author, "ROLE_ADMIN");

        // news 권한 체크 - ROLE_DEV인 경우, "news - notice - #패치" 에 글을 쓸 수 있음
        if (isCategoryMatch(category, "news") && !isAdmin && !isDevPatchAllowed(author, category, tagEntity)) {
            throw new NotIncludeRoleException(ErrorCode.BOARD_ACCESS_DENIED);
        }

        // "카르텔" 카테고리 접근 권한 확인
        checkCategoryReadable(category, author);

        boolean isShopCategory = isShopCategory(category);
        if (isShopCategory) {
            if (!isAdmin) {
                throw new NotIncludeRoleException(ErrorCode.SHOP_WRITE_FORBIDDEN);
            }
            validateItemPrice(request.getItemPrice());
        }

        Board board = new Board();
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setCategory(category);
        board.setTag(tagEntity);
        applyShopItem(board, isShopCategory, request);

        // 관리자만 고정 여부 설정 가능
        if (request.getPinned() != null && isAdmin) {
            board.setPinned(request.getPinned());
        }

        write(board, author, request.getAttachmentIds());
        return board;
    }

    /**
     * 게시글 수정 (작성자/관리자 권한 및 상점 상품 검증 포함)
     */
    @Transactional
    public Board updateBoard(Long id, User currentUser, BoardWriteRequestDTO request) {
        Board boardTemp = findBoard(id);

        boolean isAdmin = hasRole(currentUser, "ROLE_ADMIN");

        if (!isAdmin && !boardTemp.getAuthor().getId().equals(currentUser.getId())) {
            throw new NotIncludeRoleException(ErrorCode.BOARD_UPDATE_FORBIDDEN);
        }

        Category category = findCategory(request.getCategoryId());

        // Validation. tagId가 있으면 해당 카테고리 소속인지 검증
        Tag tagEntity = resolveTag(request.getTagId(), category);

        // news 권한 체크
        if (isCategoryMatch(category, "news") && !isAdmin && !isDevPatchAllowed(currentUser, category, tagEntity)) {
            throw new NotIncludeRoleException(ErrorCode.NEWS_UPDATE_FORBIDDEN);
        }

        // "카르텔" 카테고리 접근 권한 확인
        checkCategoryReadable(category, currentUser);

        boolean isShopCategory = isShopCategory(category);
        if (isShopCategory) {
            if (!isAdmin) {
                throw new NotIncludeRoleException(ErrorCode.SHOP_UPDATE_FORBIDDEN);
            }
            validateItemPrice(request.getItemPrice());
        }

        boardTemp.setTitle(request.getTitle());
        boardTemp.setContent(request.getContent());
        boardTemp.setCategory(category);
        boardTemp.setTag(tagEntity);
        applyShopItem(boardTemp, isShopCategory, request);

        // 관리자만 고정 여부 설정 가능
        if (request.getPinned() != null && isAdmin) {
            boardTemp.setPinned(request.getPinned());
        }

        write(boardTemp, currentUser, request.getAttachmentIds());
        return boardTemp;
    }

    /**
     * 게시글 상세 조회 (조회수 증가 + 카르텔 카테고리 접근 권한 확인)
     * - 삭제된 게시글은 권한 확인 없이 그대로 반환하고, 컨트롤러에서 삭제 여부를 응답한다.
     */
    @Transactional
    public Board viewBoard(Long id, User user) {
        Board board = boardView(id);

        if (board.getStatus() != BoardStatus.DELETED) {
            checkCategoryReadable(board.getCategory(), user);
        }
        return board;
    }

    // Note. resolveCategory - categoryId가 있으면 ID로, 없으면 slug로 카테고리를 조회한다.
    @Transactional(readOnly = true)
    public Category resolveCategory(Long categoryId, String slug) {
        if (categoryId != null) {
            return findCategory(categoryId);
        }
        if (slug != null) {
            if (!isValidSlug(slug)) {
                throw new NotAllowFormException(ErrorCode.INVALID_SLUG);
            }
            return categoryRepository.findBySlugAndIsActiveTrue(slug)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
        }
        throw new NotAllowFormException(ErrorCode.CATEGORY_PARAM_REQUIRED);
    }

    // Note. checkCategoryReadable - "카르텔" 카테고리는 ROLE_CARTEL 또는 ROLE_ADMIN만 접근할 수 있다.
    @Transactional(readOnly = true)
    public void checkCategoryReadable(Category category, User user) {
        if (isCategoryMatch(category, "cartel")
                && !hasRole(user, "ROLE_CARTEL") && !hasRole(user, "ROLE_ADMIN")) {
            throw new NotIncludeRoleException(ErrorCode.BOARD_ACCESS_DENIED);
        }
    }

    // Note. boardListByCategory - 카테고리별 게시글 목록을 페이지네이션으로 조회한다.
    @Transactional(readOnly = true)
    public Page<Board> boardListByCategory(Category category, Pageable pageable) {
        return boardRepository.findByCategoryAndStatusOrderByPinnedDescCreatedAtDesc(
            category, BoardStatus.ACTIVE, pageable);
    }

    // Note. boardListByCategoryWithPinned - 카테고리별 게시글 고정 목록을 페이지네이션으로 조회한다.
    @Transactional(readOnly = true)
    public Page<Board> boardListByCategoryWithPinned(Category category, Pageable pageable) {
        return boardRepository.findByCategoryAndStatusOrderByPinnedDescCreatedAtDesc(category, BoardStatus.ACTIVE, pageable);
    }

    // Note. boardList - 전체 게시글 목록을 페이지네이션으로 조회한다.
    @Transactional(readOnly = true)
    public Page<Board> boardList(Pageable pageable) {
        return boardRepository.findByStatus(BoardStatus.ACTIVE, pageable);
    }

    // Note. boardSearchList - "검색어"(제목 기반)를 기반으로 게시글을 조회한다.
    @Transactional(readOnly = true)
    public Page<Board> boardSearchList(String searchKeyword, Pageable pageable) {
        return boardRepository.findByTitleContainingAndStatus(searchKeyword, BoardStatus.ACTIVE, pageable);
    }

    // Note. boardSearchListByCategory - 특정한 카테고리 내부에서 검색어로 게시글을 조회한다.
    @Transactional(readOnly = true)
    public Page<Board> boardSearchListByCategory(Category category, String searchKeyword, Pageable pageable) {
        return boardRepository.findByCategoryAndTitleContainingAndStatus(category, searchKeyword, BoardStatus.ACTIVE, pageable);
    }

    // Note. boardListByCategories - 여러 카테고리(부모+자식)의 게시글을 페이지네이션으로 조회한다.
    @Transactional(readOnly = true)
    public Page<Board> boardListByCategories(List<Category> categories, Pageable pageable) {
        return boardRepository.findByCategoryInAndStatus(categories, BoardStatus.ACTIVE, pageable);
    }

    // Note. boardSearchListByCategories - 여러 카테고리 내부에서 검색어로 게시글을 조회한다.
    @Transactional(readOnly = true)
    public Page<Board> boardSearchListByCategories(List<Category> categories, String searchKeyword, Pageable pageable) {
        return boardRepository.findByCategoryInAndTitleContainingAndStatus(categories, searchKeyword, BoardStatus.ACTIVE, pageable);
    }

    // Note. boardListPopular - 전체 게시판 인기글을 조회한다.(조회수 기반)
    @Transactional(readOnly = true)
    public Page<Board> boardListPopular(Pageable pageable) {
        return boardRepository.findAllByStatusOrderByViewsDescCreatedAtDesc(BoardStatus.ACTIVE, pageable);
    }

    // Note. boardListPopularByCategory - 특정 카테고리 내부 인기글을 조회한다. (조회수 기반)
    @Transactional(readOnly = true)
    public Page<Board> boardListPopularByCategory(Category category, Pageable pageable) {
        return boardRepository.findByCategoryAndStatusOrderByViewsDescCreatedAtDesc(category, BoardStatus.ACTIVE, pageable);
    }

    // Note. boardView - 포스트 ID로 게시글 조회 및 조회수 증가 메서드
    @Transactional
    public Board boardView(Long id) {

        Board board = findBoard(id);

        board.setPostView(board.getPostView() + 1);
        boardRepository.save(board);

        return board;
    }

    // Note. deleteBoard - 작성자 또는 관리자만 게시글을 삭제(soft delete)할 수 있다.
    @Transactional
    public void deleteBoard(Long id, User currentUser) {
        Board board = findBoard(id);

        if (!hasRole(currentUser, "ROLE_ADMIN") && !board.getAuthor().getId().equals(currentUser.getId())) {
            throw new NotIncludeRoleException(ErrorCode.BOARD_DELETE_FORBIDDEN);
        }

        boardDelete(id);
    }

    // Note. boardDelete - 포스트 ID로 게시글 삭제 (soft delete)
    // ... 권한 체크는 deleteBoard에서 진행한다.
    @Transactional
    public void boardDelete(Long id) {
        Board board = findBoard(id);

        board.setStatus(BoardStatus.DELETED);
        boardRepository.save(board);

        commentRepository.deleteAllByBoardId(id);
        boardLikeRepository.deleteAllByBoardId(id);
    }

    // Note. toggleBoardPin - 게시글 고정/해제 토글 (관리자만 가능)
    @Transactional
    public Board toggleBoardPin(Long id, User currentUser) {
        if (!hasRole(currentUser, "ROLE_ADMIN")) {
            throw new NotIncludeRoleException(ErrorCode.BOARD_PIN_FORBIDDEN);
        }

        Board board = findBoard(id);

        board.setPinned(!board.getPinned());
        boardRepository.save(board);

        return board;
    }

    // BoardService.java (또는 구현체)
    public long countByAuthor(User author) {
        return boardRepository.countByAuthorAndStatus(author, BoardStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Board> getMyBoards(User author) {
        return boardRepository.findByAuthorAndStatusOrderByCreatedAtDesc(author, BoardStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<Board> findById(Long id) {
        return boardRepository.findById(id);
    }

    @Transactional
    public void deleteMyBoards(List<Long> boardIds, User author) {
        if (boardIds == null || boardIds.isEmpty()) {
            throw new NotAllowFormException(ErrorCode.DELETE_TARGET_REQUIRED);
        }

        List<Board> boards = boardRepository.findAllById(boardIds);
        // 본인 게시글만 soft delete
        boards.stream()
                .filter(b -> b.getAuthor().getId().equals(author.getId()))
                .forEach(b -> {
                    b.setStatus(BoardStatus.DELETED);
                    boardRepository.save(b);

                    commentRepository.deleteAllByBoardId(b.getId());
                    boardLikeRepository.deleteAllByBoardId(b.getId());
                });
    }

    @Transactional
    public void deleteBoardsByTag(Long categoryId, Long tagId) {
        int deletedCount = boardRepository.updateStatusByTagId(categoryId, tagId, BoardStatus.DELETED);
        System.out.println("tagId=" + tagId + " 태그를 가진 게시글 " + deletedCount + "건을 비활성화했습니다.");
    }

    @Transactional
    public int hideBoardsByTag(Long categoryId, Long tagId) {
        findCategory(categoryId);
        return boardRepository.updateStatusByTagIdAndCurrentStatus(categoryId, tagId, BoardStatus.ACTIVE, BoardStatus.HIDDEN);
    }

    @Transactional
    public int activateBoardsByTag(Long categoryId, Long tagId) {
        findCategory(categoryId);
        return boardRepository.updateStatusByTagIdAndCurrentStatus(categoryId, tagId, BoardStatus.HIDDEN, BoardStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long countBoardsByTagAndStatus(Long categoryId, Long tagId, BoardStatus status) {
        return boardRepository.countByCategoryIdAndTagIdAndStatus(categoryId, tagId, status);
    }

    // Note. getTagStats - 카테고리의 태그별 활성/숨김 게시글 수를 조회한다. (관리자용)
    @Transactional(readOnly = true)
    public BoardTagStatsResponseDTO getTagStats(Long categoryId) {
        findCategory(categoryId);

        List<BoardTagStatsResponseDTO.TagStatInfo> tagStats = tagRepository
                .findAllByCategoryIdOrderBySortValueAsc(categoryId).stream()
                .map(tag -> BoardTagStatsResponseDTO.TagStatInfo.builder()
                        .tagId(tag.getId())
                        .tagName(tag.getTagName())
                        .activeCount(countBoardsByTagAndStatus(categoryId, tag.getId(), BoardStatus.ACTIVE))
                        .hiddenCount(countBoardsByTagAndStatus(categoryId, tag.getId(), BoardStatus.HIDDEN))
                        .build())
                .toList();

        return BoardTagStatsResponseDTO.of(categoryId, tagStats);
    }

    private Board findBoard(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Tag resolveTag(Long tagId, Category category) {
        if (tagId == null) {
            return null;
        }
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TAG_NOT_FOUND));
        if (!tag.getCategory().getId().equals(category.getId())) {
            throw new NotAllowFormException(ErrorCode.TAG_NOT_IN_CATEGORY);
        }
        return tag;
    }

    /** slug 형식 검증: 영문·숫자·하이픈·언더스코어만 허용, 최대 50자 */
    private boolean isValidSlug(String slug) {
        return slug != null && !slug.isEmpty() && slug.length() <= 50
                && slug.matches("^[a-zA-Z0-9_-]+$");
    }

    // 카테고리(자신 또는 부모)의 slug가 일치하는지 확인
    private boolean isCategoryMatch(Category category, String slug) {
        if (category == null) return false;
        if (slug.equalsIgnoreCase(category.getSlug())) return true;
        if (category.getParentId() != null) {
            Category parent = categoryRepository.findById(category.getParentId()).orElse(null);
            if (parent != null && slug.equalsIgnoreCase(parent.getSlug())) return true;
        }
        return false;
    }

    private boolean isShopCategory(Category category) {
        if (category == null) return false;
        if (Boolean.TRUE.equals(category.getShopEnabled())) return true;
        if (category.getParentId() != null) {
            Category parent = categoryRepository.findById(category.getParentId()).orElse(null);
            return parent != null && Boolean.TRUE.equals(parent.getShopEnabled());
        }
        return false;
    }

    // 사용자가 특정 역할을 보유하는지 확인
    private boolean hasRole(User user, String role) {
        return user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthorityName().equals(role));
    }

    // ROLE_DEV인 경우, "news - notice - #패치" 에만 글을 쓰거나 수정할 수 있음
    private boolean isDevPatchAllowed(User user, Category category, Tag tagEntity) {
        if (!hasRole(user, "ROLE_DEV")) {
            return false;
        }
        boolean isNoticeCategory = "notice".equalsIgnoreCase(category.getSlug());
        boolean isParentNews = false;
        if (category.getParentId() != null) {
            Category parent = categoryRepository.findById(category.getParentId()).orElse(null);
            isParentNews = parent != null && "news".equalsIgnoreCase(parent.getSlug());
        }
        boolean isPatchTag = tagEntity != null && "패치".equals(tagEntity.getTagName());

        return isNoticeCategory && isParentNews && isPatchTag;
    }

    private void validateItemPrice(Long itemPrice) {
        if (itemPrice == null || itemPrice <= 0) {
            throw new NotAllowFormException(ErrorCode.INVALID_ITEM_PRICE);
        }
    }

    // 상점 카테고리면 상품 정보를, 아니면 일반 게시글 기본값을 설정한다.
    private void applyShopItem(Board board, boolean isShopCategory, BoardWriteRequestDTO request) {
        ShopItemType resolvedItemType = isShopCategory
                ? parseShopItemType(request.getItemType())
                : ShopItemType.GENERAL;
        ProfileDecoration shopDecoration = isShopCategory
                ? resolveShopProfileDecoration(resolvedItemType, request.getProfileDecorationId())
                : null;
        Long resolvedThumbnailAttachmentId = isShopCategory
                ? attachmentService.resolveThumbnailAttachmentId(request.getAttachmentIds(), request.getThumbnailAttachmentId())
                : null;
        if (isShopCategory && resolvedThumbnailAttachmentId == null) {
            throw new NotAllowFormException(ErrorCode.SHOP_THUMBNAIL_REQUIRED);
        }

        board.setItemPrice(isShopCategory ? request.getItemPrice() : 0L);
        board.setItemType(resolvedItemType);
        board.setProfileDecoration(shopDecoration);
        board.setThumbnailAttachmentId(resolvedThumbnailAttachmentId);
    }

    private ShopItemType parseShopItemType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return ShopItemType.GENERAL;
        }

        try {
            return ShopItemType.valueOf(itemType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotAllowFormException(ErrorCode.INVALID_ITEM_TYPE);
        }
    }

    private ProfileDecoration resolveShopProfileDecoration(ShopItemType itemType, Long profileDecorationId) {
        if (itemType != ShopItemType.BADGE) {
            return null;
        }
        if (profileDecorationId == null) {
            throw new NotAllowFormException(ErrorCode.SHOP_BADGE_REQUIRED);
        }

        ProfileDecoration decoration = profileDecorationRepository.findById(profileDecorationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHOP_BADGE_NOT_FOUND));
        if (!decoration.isActive()) {
            throw new NotAllowFormException(ErrorCode.SHOP_BADGE_INACTIVE);
        }
        decoration.setPurchaseRequired(true);
        profileDecorationRepository.save(decoration);
        return decoration;
    }
}
