package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

/**
 * 사용자 승인 상태 관련 예외
 * - 승인 대기 중이거나 승인 거부된 경우 발생
 */
public class UserNotApprovedException extends GlobalNimdaException {

    public UserNotApprovedException(ErrorCode errorCode){
        super(errorCode);
    }

    public UserNotApprovedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
