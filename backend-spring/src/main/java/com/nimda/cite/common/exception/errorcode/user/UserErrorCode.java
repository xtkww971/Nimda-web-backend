package com.nimda.cite.common.exception.errorcode.user;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "UE001", "해당 정보를 가진 유저가 이미 존재합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "UE002", "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "UE003", "이미 사용 중인 닉네임입니다."),
    PROFILE_IMAGE_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "UE004", "프로필 이미지 키가 필요합니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
