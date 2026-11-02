package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "카테고리를 찾을 수 없습니다."),
    INVALID_SLUG(HttpStatus.BAD_REQUEST, "CT002", "유효하지 않은 카테고리 slug입니다."),
    CATEGORY_PARAM_REQUIRED(HttpStatus.BAD_REQUEST, "CT003", "categoryId 또는 slug 파라미터가 필요합니다."),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "CT004", "관리자 권한이 필요합니다."),
    SELF_PARENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CT005", "자기 자신을 부모로 설정할 수 없습니다."),
    CIRCULAR_REFERENCE(HttpStatus.BAD_REQUEST, "CT006", "순환 참조가 발생합니다. 해당 카테고리를 부모로 설정할 수 없습니다."),
    HAS_CHILD_CATEGORY(HttpStatus.BAD_REQUEST, "CT007", "하위 카테고리가 존재하여 삭제할 수 없습니다. 먼저 하위 카테고리를 삭제하거나 비활성화하세요."),
    HAS_POSTS(HttpStatus.BAD_REQUEST, "CT008", "해당 카테고리에 게시글이 존재하여 삭제할 수 없습니다."),
    SLUG_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "CT009", "이미 사용 중인 슬러그입니다."),
    PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CT010", "부모 카테고리를 찾을 수 없습니다."),
    INACTIVE_PARENT(HttpStatus.BAD_REQUEST, "CT011", "비활성화된 카테고리는 부모로 설정할 수 없습니다."),
    MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CT012", "카테고리는 최대 2단계까지만 설정 가능합니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
