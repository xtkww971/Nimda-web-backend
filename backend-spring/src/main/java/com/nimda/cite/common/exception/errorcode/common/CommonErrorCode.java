package com.nimda.cite.common.exception.errorcode.common;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "CO001", "잘못된 요청입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "CO002", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CO003", "서버 오류가 발생했습니다."),
    VERSION_MISMATCH(HttpStatus.UPGRADE_REQUIRED, "CO004", "앱 버전이 일치하지 않습니다. 최신 버전으로 업데이트 해주세요.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
