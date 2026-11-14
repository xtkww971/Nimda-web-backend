package com.nimda.cite.common.exception.errorcode.ctf;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InstanceErrorCode implements ErrorCode {
    INSTANCE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "IN001", "해당 문제는 인스턴스 기능을 지원하지 않습니다."),
    CTF_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "IN002", "CTF 서버가 아직 작동중이지 않습니다."),
    INSTANCE_FORBIDDEN(HttpStatus.FORBIDDEN, "IN003", "본인 인스턴스가 아닙니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
