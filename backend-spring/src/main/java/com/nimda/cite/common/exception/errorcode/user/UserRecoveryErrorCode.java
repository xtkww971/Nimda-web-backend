package com.nimda.cite.common.exception.errorcode.user;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserRecoveryErrorCode implements ErrorCode {
    RECOVERY_SESSION_EXPIRED(HttpStatus.FORBIDDEN, "UR001", "인증 세션이 만료되었습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.UNAUTHORIZED, "UR002", "인증에 실패했습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "UR003", "이메일 인증이 완료되지 않은 요청입니다.");
    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
