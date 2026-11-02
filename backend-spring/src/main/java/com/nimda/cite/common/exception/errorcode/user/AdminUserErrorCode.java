package com.nimda.cite.common.exception.errorcode.user;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminUserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US001", "User not found"),
    USER_PENDING_FAILED(HttpStatus.BAD_REQUEST, "US002", "User pending failed"),
    USER_CONVERT_ROLE_FAILED(HttpStatus.BAD_REQUEST, "US003", "User role convert failed"),
    USER_CONVERT_STATUS_FAILED(HttpStatus.BAD_REQUEST, "US004", "User status convert failed"),
    USER_ROLE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "US005", "User role not found"),
    USER_GRANT_ROLE_FAILED(HttpStatus.BAD_REQUEST, "US006", "User role grant failed"),
    USER_REVOKE_ROLE_FAILED(HttpStatus.BAD_REQUEST, "US007", "User role revoke failed"),
    AUTHORITY_NOT_EXIST(HttpStatus.BAD_REQUEST, "US008", "Authority not found");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
