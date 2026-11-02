package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

public class VersionMismatchException extends GlobalNimdaException{
    public VersionMismatchException(ErrorCode errorCode){
        super(errorCode);
    }

    public VersionMismatchException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
