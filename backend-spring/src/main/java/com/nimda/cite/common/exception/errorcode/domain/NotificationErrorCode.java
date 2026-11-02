package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT001", "알림이 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
