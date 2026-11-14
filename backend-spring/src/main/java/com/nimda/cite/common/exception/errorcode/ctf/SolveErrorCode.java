package com.nimda.cite.common.exception.errorcode.ctf;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SolveErrorCode implements ErrorCode {
    TOO_MANY_SUBMISSIONS(HttpStatus.TOO_MANY_REQUESTS, "SV001", "제출이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    SUBMISSION_FORBIDDEN(HttpStatus.FORBIDDEN, "SV002", "본인의 제출이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
