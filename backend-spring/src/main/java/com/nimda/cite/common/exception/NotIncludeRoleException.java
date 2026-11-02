package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;

public class NotIncludeRoleException extends GlobalNimdaException{
    public NotIncludeRoleException(ErrorCode errorCode){
        super(errorCode);
    }

    public NotIncludeRoleException(ErrorCode errorCode, String message){
        super(errorCode, message);
    }
}
