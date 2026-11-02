package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

/**
 * 인증(로그인) 관련 예외
 * - 로그인 정보가 없거나, 인증 토큰/코드가 유효하지 않은 경우 발생
 */
public class UnauthorizedException extends GlobalNimdaException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
