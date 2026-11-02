package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum Like implements ErrorCode {
    ;

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
