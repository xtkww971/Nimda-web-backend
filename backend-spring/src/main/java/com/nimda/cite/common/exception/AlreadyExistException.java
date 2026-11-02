package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

public class AlreadyExistException extends GlobalNimdaException{
    public AlreadyExistException(ErrorCode errorCode){
        super(errorCode);
    }

    public AlreadyExistException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
