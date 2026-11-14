package com.nimda.cite.common.exception.errorcode.ctf;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContestErrorCode implements ErrorCode {
    CONTEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CN001", "존재하지 않는 대회입니다."),
    UNFINALIZED_CONTEST_EXISTS(HttpStatus.CONFLICT, "CN002", "결과가 확정되지 않은 대회가 있습니다. 종료 후 결과가 확정되면 새 대회를 만들 수 있습니다."),
    CONTEST_STARTS_BEFORE_PREVIOUS(HttpStatus.BAD_REQUEST, "CN003", "새 대회는 이전 대회보다 늦게 시작해야 합니다."),
    CONTEST_NOT_ENDED(HttpStatus.CONFLICT, "CN004", "아직 종료되지 않은 대회입니다."),
    CONTEST_ENDED(HttpStatus.FORBIDDEN, "CN005", "대회가 종료되었습니다."),
    INVALID_CONTEST_RANGE(HttpStatus.BAD_REQUEST, "CN006", "종료 시각은 시작 시각보다 늦어야 합니다."),
    CONTEST_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "CN007", "결과가 확정된 대회가 아닙니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
