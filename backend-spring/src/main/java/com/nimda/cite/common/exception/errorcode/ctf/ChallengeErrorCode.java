package com.nimda.cite.common.exception.errorcode.ctf;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChallengeErrorCode implements ErrorCode {
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "존재하지 않는 문제입니다."),
    CHALLENGE_CODE_DUPLICATED(HttpStatus.CONFLICT, "CH002", "이미 존재하는 문제 코드입니다."),
    INVALID_CHALLENGE_RULE(HttpStatus.BAD_REQUEST, "CH003", "허용되지 않는 문제 유형 조합입니다."),
    INVALID_POINT_RANGE(HttpStatus.BAD_REQUEST, "CH004", "최소 점수는 최대 점수보다 클 수 없습니다."),
    STATIC_FLAG_REQUIRED(HttpStatus.BAD_REQUEST, "CH005", "STATIC 문제는 플래그가 필요합니다."),
    DOWNLOAD_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "CH006", "인스턴스 문제는 첨부파일 다운로드를 지원하지 않습니다."),
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CH007", "첨부파일이 없는 문제입니다."),
    DOWNLOAD_FORBIDDEN(HttpStatus.FORBIDDEN, "CH008", "본인이 요청한 다운로드가 아닙니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
