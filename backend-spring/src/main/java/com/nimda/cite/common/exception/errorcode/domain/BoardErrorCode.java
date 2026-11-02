package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BoardErrorCode implements ErrorCode {
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "BO001", "게시글을 찾을 수 없습니다."),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "BO002", "접근 권한이 없습니다."),
    BOARD_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "BO003", "게시글을 수정할 권한이 없습니다."),
    BOARD_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "BO004", "게시글을 삭제할 권한이 없습니다."),
    BOARD_PIN_FORBIDDEN(HttpStatus.FORBIDDEN, "BO005", "게시글을 고정/해제할 권한이 없습니다. 관리자만 가능합니다."),
    NEWS_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "BO006", "새 소식 게시판은 관리자만 수정할 수 있습니다."),
    SHOP_WRITE_FORBIDDEN(HttpStatus.FORBIDDEN, "BO007", "상품 등록은 관리자만 가능합니다."),
    SHOP_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "BO008", "상품 수정은 관리자만 가능합니다."),
    INVALID_ITEM_PRICE(HttpStatus.BAD_REQUEST, "BO009", "상품 가격을 1 NC 이상으로 입력해주세요."),
    INVALID_ITEM_TYPE(HttpStatus.BAD_REQUEST, "BO010", "지원하지 않는 상품 종류입니다."),
    SHOP_BADGE_REQUIRED(HttpStatus.BAD_REQUEST, "BO011", "배지 상품은 배지를 선택해야 합니다."),
    SHOP_BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "BO012", "존재하지 않는 배지입니다."),
    SHOP_BADGE_INACTIVE(HttpStatus.BAD_REQUEST, "BO013", "비활성화된 배지는 상품으로 등록할 수 없습니다."),
    SHOP_THUMBNAIL_REQUIRED(HttpStatus.BAD_REQUEST, "BO014", "마일리지 상점 상품은 썸네일 이미지가 필요합니다."),
    DELETE_TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "BO015", "삭제할 게시글을 선택해주세요.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
