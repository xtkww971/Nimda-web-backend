package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {
    BALANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PT001", "포인트 계좌가 존재하지 않습니다."),
    BALANCE_NOT_FOUND_PENDING(HttpStatus.NOT_FOUND, "PT002", "포인트 계좌가 존재하지 않습니다! 승인 대기 중일 수 있습니다."),
    ITEM_NOT_PURCHASABLE(HttpStatus.BAD_REQUEST, "PT003", "구매할 수 없는 상품입니다."),
    NOT_SHOP_ITEM(HttpStatus.BAD_REQUEST, "PT004", "마일리지 구매 상품이 아닙니다."),
    ITEM_PRICE_NOT_SET(HttpStatus.BAD_REQUEST, "PT005", "상품 가격이 설정되지 않았습니다."),
    ITEM_BADGE_NOT_LINKED(HttpStatus.BAD_REQUEST, "PT006", "상품에 연결된 배지가 없습니다."),
    BADGE_ALREADY_OWNED(HttpStatus.BAD_REQUEST, "PT007", "이미 보유한 배지입니다."),
    INVALID_SPEND_AMOUNT(HttpStatus.BAD_REQUEST, "PT008", "차감할 마일리지가 올바르지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "PT009", "보유 마일리지가 부족합니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
