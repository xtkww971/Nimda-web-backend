package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

/**
 * 리소스의 현재 상태 때문에 요청을 처리할 수 없는 경우의 예외
 * - 예) 이미 삭제된 댓글 수정, 이미 처리된 가입 요청, 정원 초과 등
 */
public class InvalidStateException extends GlobalNimdaException {
    public InvalidStateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidStateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
