package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AttendanceErrorCode implements ErrorCode {
    ALREADY_CHECKED_IN(HttpStatus.BAD_REQUEST, "AT001", "이미 오늘 출석을 완료했습니다."),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "AT002", "출석 정보가 없습니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
