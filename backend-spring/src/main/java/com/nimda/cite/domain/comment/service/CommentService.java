package com.nimda.cite.domain.comment.service;

import com.nimda.cite.common.exception.InvalidStateException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.NotIncludeRoleException;
import com.nimda.cite.common.exception.errorcode.domain.BoardErrorCode;
import com.nimda.cite.common.exception.errorcode.domain.CommentErrorCode;
import com.nimda.cite.common.exception.errorcode.user.UserErrorCode;
import com.nimda.cite.domain.alarm.Event.AddChildCommentEvent;
import com.nimda.cite.domain.alarm.Event.AddCommentEvent;
import com.nimda.cite.domain.board.entity.Board;
import com.nimda.cite.domain.board.repository.BoardRepository;
import com.nimda.cite.domain.comment.dto.*;
import com.nimda.cite.domain.comment.entity.Comment;
import com.nimda.cite.domain.comment.enums.STATUS;
import com.nimda.cite.domain.comment.repository.CommentRepository;
import com.nimda.cite.domain.like.repository.CommentLikeRepositroy;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CommentLikeRepositroy commentLikeRepository;

    // =============== CREATE ===============

    // 댓글 등록
    @Transactional
    public CommentResponse createComment(Long boardId, CommentCreateRequest request, Long userId) {

        // 연관 엔티티 조회
        // 게시글
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(BoardErrorCode.BOARD_NOT_FOUND));

        // 작성자
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        // 부모 댓글 조회
        Comment parent = null;
        if (request.getParentId() != null && request.getParentId() > 0) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException(CommentErrorCode.PARENT_COMMENT_NOT_FOUND));

            // 깊이 제한: 부모 댓글이 이미 대댓글인지 확인
            if (parent.getParent() != null) {
                throw new InvalidStateException(CommentErrorCode.REPLY_DEPTH_EXCEEDED);
            }

            // 상태 확인: 부모 댓글 삭제 여부 확인
            if (parent.getStatus() == STATUS.DELETED) {
                throw new InvalidStateException(CommentErrorCode.PARENT_COMMENT_DELETED);
            }
        }

        // 엔티티 생성
        Comment comment = Comment.builder()
                .context(request.getContext())
                .board(board)
                .author(author)
                .parent(parent)
                .status(STATUS.PUBLIC)
                .build();

        // 저장
        Comment saved = commentRepository.save(comment);

        // 게시글에 댓글이 달린 경우
        if(parent == null) {
            // 게시글 작성자와 댓글 작성자가 다를 때만 알림
            if(!board.getAuthor().getId().equals(userId)) {
                eventPublisher.publishEvent(
                        new AddCommentEvent(this, board.getAuthor(), comment.getAuthor(), board.getTitle(), boardId, comment.getContext())
                );
            }
        }
        // 댓글에 대댓글이 달린 경우
        else {
            if (!parent.getAuthor().getId().equals(userId)) {
                eventPublisher.publishEvent(
                        new AddChildCommentEvent(this, board, comment.getAuthor(), parent.getAuthor())
                );
            }
        }

        return CommentResponse.forUser(saved, userId);
    }


    // =============== READ ===============

    // 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long boardId, Long userId, boolean isAdmin) {
        List<Comment> allComments = commentRepository.findAllCommentsByBoardId(boardId);

        List<CommentResponse> rootComments = new ArrayList<>();
        Map<Long, CommentResponse> map = new HashMap<>();

        for (Comment comment : allComments) {
            CommentResponse dto = isAdmin
                    ? CommentResponse.forAdmin(comment, userId)
                    : CommentResponse.forUser(comment, userId);

            // 좋아요 상태 설정
            if (userId != null) {
                dto.setIsLiked(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), userId));
            } else {
                dto.setIsLiked(false);
            }

            map.put(dto.getId(), dto);

            if (comment.getParent() == null) {
                // 댓글인 경우
                rootComments.add(dto);
            } else {
                // 대댓글인 경우
                CommentResponse parentDto = map.get(comment.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                } else {
                    // 자식 댓글의 부모가 db에 없는 경우
                    // TODO 로그 남기기
                }
            }
        }

        return rootComments;
    }

    // 마이 페이지 작성 댓글 조회
    public List<MyCommentResponse> getMyComments(Long userId) {
        // 작성자
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        List<Comment> comments = commentRepository.findByMyComments(user, List.of(STATUS.DELETED));

        return comments.stream()
                .map(MyCommentResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 유저의 작성 댓글 조회 (공개 프로필용)
    @Transactional(readOnly = true)
    public List<MyCommentResponse> getCommentsByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        return getMyComments(user.getId());
    }

    // =============== UPDATE ===============

    @Transactional
    public CommentResponse updateCommentStatus(Long commentId, CommentStatusUpdateRequest request) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (comment.getStatus() == STATUS.DELETED) {
            throw new InvalidStateException(CommentErrorCode.DELETED_COMMENT_STATUS_CHANGE);
        }

        comment.updateStatus(request.getStatus());

        return CommentResponse.forAdmin(comment);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        if(!comment.getAuthor().getId().equals(userId)) {
            throw new NotIncludeRoleException(CommentErrorCode.COMMENT_UPDATE_FORBIDDEN);
        }

        if (comment.getStatus() == STATUS.DELETED) {
            throw new InvalidStateException(CommentErrorCode.DELETED_COMMENT_UPDATE);
        }

        comment.updateContext(request.getContext());

        return CommentResponse.forUser(comment, userId);
    }


    // =============== DELETE ===============

    // SOFT DELETE
    @Transactional
    public void deleteComment(Long commentId, Long userId) {

        Comment comment = commentRepository.findWithAuthorById(commentId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotIncludeRoleException(CommentErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        if (comment.getStatus() == STATUS.DELETED) {
            throw new InvalidStateException(CommentErrorCode.COMMENT_ALREADY_DELETED);
        }

        comment.updateStatus(STATUS.DELETED);
    }

    @Transactional
    public void deleteMyComments(List<Long> commentIds, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        commentRepository.deleteAllByIdInAndAuthor(commentIds, user);
    }
    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        // 삭제된 댓글(DELETED)은 개수에서 제외합니다.
        return commentRepository.countByAuthorIdAndStatusNot(userId, STATUS.DELETED);
    }

    // 내가 댓글을 단 게시글 ID 목록 조회
    @Transactional(readOnly = true)
    public List<Long> getCommentedBoardIds(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        return commentRepository.findDistinctBoardIdsByAuthor(user, List.of(STATUS.DELETED));
    }
}