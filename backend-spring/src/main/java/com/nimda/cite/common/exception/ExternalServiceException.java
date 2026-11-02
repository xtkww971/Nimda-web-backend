package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

/**
 * 외부 서비스(S3 등)를 사용할 수 없거나 처리에 실패한 경우의 예외
 */
public class ExternalServiceException extends GlobalNimdaException {
    public ExternalServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalServiceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
