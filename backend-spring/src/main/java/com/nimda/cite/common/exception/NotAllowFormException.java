package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

public class NotAllowFormException extends GlobalNimdaException{
    public NotAllowFormException(ErrorCode errorCode){
        super(errorCode);
    }

    public NotAllowFormException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
