package com.nimda.cite.common.exception.errorcode.user;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    CANNOT_AUTHORIZED(HttpStatus.UNAUTHORIZED, "AE001", "인증이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AE002", "해당 유저 정보를 찾을 수 없습니다."),
    // 프론트는 로그인 403을 승인 대기/거부 계정으로 처리한다.
    STATUS_PENDING(HttpStatus.FORBIDDEN, "AE003", "승인 대기 중인 계정입니다. 관리자 승인 후 로그인할 수 있습니다."),
    STATUS_REJECTED(HttpStatus.FORBIDDEN, "AE004", "승인이 거부된 계정입니다."),
    STATUS_NOT_APPROVED(HttpStatus.FORBIDDEN, "AE005", "승인되지 않은 계정입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AE006", "Invalid user ID or password");


    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
