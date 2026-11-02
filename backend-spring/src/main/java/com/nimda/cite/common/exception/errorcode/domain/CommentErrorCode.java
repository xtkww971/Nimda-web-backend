package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "댓글을 찾을 수 없습니다."),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM002", "부모 댓글을 찾을 수 없습니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CM003", "대댓글에는 답글을 달 수 없습니다."),
    PARENT_COMMENT_DELETED(HttpStatus.BAD_REQUEST, "CM004", "삭제된 댓글에는 답글을 달 수 없습니다."),
    DELETED_COMMENT_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "CM005", "삭제된 댓글은 상태를 변경할 수 없습니다."),
    COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "CM006", "댓글 수정 권한이 없습니다."),
    DELETED_COMMENT_UPDATE(HttpStatus.BAD_REQUEST, "CM007", "삭제된 댓글은 수정할 수 없습니다."),
    COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "CM008", "댓글 삭제 권한이 없습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "CM009", "이미 삭제된 댓글입니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
