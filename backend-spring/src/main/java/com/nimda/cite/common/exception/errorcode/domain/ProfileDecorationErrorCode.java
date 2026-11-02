package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileDecorationErrorCode implements ErrorCode {
    DECORATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PD001", "존재하지 않는 프로필 장식입니다."),
    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PD002", "존재하지 않는 프로필 배지입니다."),
    DECORATION_NOT_USABLE(HttpStatus.BAD_REQUEST, "PD003", "사용할 수 없는 프로필 장식입니다."),
    BADGE_NOT_USABLE(HttpStatus.BAD_REQUEST, "PD004", "사용할 수 없는 프로필 배지입니다."),
    DECORATION_NOT_OWNED(HttpStatus.FORBIDDEN, "PD005", "보유한 프로필 장식만 사용할 수 있습니다."),
    BADGE_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "PD006", "배지 키를 입력해주세요."),
    BADGE_KEY_INVALID(HttpStatus.BAD_REQUEST, "PD007", "배지 키는 영문 소문자, 숫자, 하이픈만 사용할 수 있습니다."),
    BADGE_LABEL_REQUIRED(HttpStatus.BAD_REQUEST, "PD008", "배지 이름을 입력해주세요."),
    BADGE_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "PD009", "배지 이미지 경로가 필요합니다."),
    BADGE_KEY_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "PD010", "이미 존재하는 배지 키입니다."),
    STUDENT_NUM_REQUIRED(HttpStatus.BAD_REQUEST, "PD011", "studentNum 값이 필요합니다."),
    DECORATION_ID_REQUIRED(HttpStatus.BAD_REQUEST, "PD012", "decorationId 값이 필요합니다."),
    STUDENT_NUM_NOT_FOUND(HttpStatus.NOT_FOUND, "PD013", "존재하지 않는 학번입니다."),
    IMAGE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PD014", "배지 이미지 서비스를 사용할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
