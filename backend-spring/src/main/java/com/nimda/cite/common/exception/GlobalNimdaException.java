package com.nimda.cite.common.exception;


import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;

@Getter
public class GlobalNimdaException extends RuntimeException {
    private final ErrorCode errorCode;

    public GlobalNimdaException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GlobalNimdaException(ErrorCode errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }
}
