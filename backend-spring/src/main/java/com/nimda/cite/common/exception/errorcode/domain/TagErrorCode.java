package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TagErrorCode implements ErrorCode {
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TG001", "태그가 존재하지 않습니다."),
    TAG_NOT_IN_CATEGORY(HttpStatus.BAD_REQUEST, "TG002", "이 태그는 해당 카테고리에 속하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
