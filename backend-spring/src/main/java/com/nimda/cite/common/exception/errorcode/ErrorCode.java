package com.nimda.cite.common.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "CO001", "잘못된 요청입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "CO002", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CO003", "서버 오류가 발생했습니다."),
    VERSION_MISMATCH(HttpStatus.UPGRADE_REQUIRED, "CO004", "앱 버전이 일치하지 않습니다. 최신 버전으로 업데이트 해주세요."),
    // 첨부 파일
    UPLOAD_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "UF001", "업로드할 파일이 없습니다."),
    FILE_FORM_NOT_ALLOW(HttpStatus.BAD_REQUEST, "UF002", "허용되지 않는 파일 형식입니다"),
    FILE_ROLE_NOT_EXIST(HttpStatus.FORBIDDEN, "UF003", "삭제 권한이 없는 파일이 포함되어 있습니다"),
    S3_KEY_INNEED(HttpStatus.BAD_REQUEST, "UF004", "S3 key가 필요합니다."),
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "UF005", "파일이 존재하지 않습니다."),
    FILE_IDS_REQUIRED(HttpStatus.BAD_REQUEST, "UF006", "fileIds가 필요합니다."),
    FILE_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "UF007", "파일 업로드 서비스를 사용할 수 없습니다."),
    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "UF008", "이미지 처리에 실패했습니다."),
    ATTACHMENT_LINK_FORBIDDEN(HttpStatus.FORBIDDEN, "UF009", "첨부 소유자만 게시글에 연결할 수 있습니다."),
    ATTACHMENT_ALREADY_LINKED(HttpStatus.BAD_REQUEST, "UF010", "이미 다른 게시글에 연결된 첨부입니다."),
    ATTACHMENT_CATEGORY_MISMATCH(HttpStatus.BAD_REQUEST, "UF011", "첨부 카테고리가 게시글 카테고리와 일치하지 않습니다."),
    ATTACHMENT_REMOVE_FORBIDDEN(HttpStatus.FORBIDDEN, "UF012", "다른 사용자가 올린 첨부는 제거할 수 없습니다."),
    INVALID_THUMBNAIL(HttpStatus.BAD_REQUEST, "UF013", "썸네일은 첨부된 이미지 중에서 선택해야 합니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "UF014", "잘못된 파일명입니다."),
    INVALID_FILE_PATH(HttpStatus.FORBIDDEN, "UF015", "잘못된 파일 경로입니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "UF016", "파일을 찾을 수 없습니다."),
    FILE_READ_FORBIDDEN(HttpStatus.FORBIDDEN, "UF017", "파일에 접근할 권한이 없습니다."),
    // 출석
    ALREADY_CHECKED_IN(HttpStatus.BAD_REQUEST, "AT001", "이미 오늘 출석을 완료했습니다."),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "AT002", "출석 정보가 없습니다."),
    // 게시글
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
    DELETE_TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "BO015", "삭제할 게시글을 선택해주세요."),
    // 카테고리
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
    MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CT012", "카테고리는 최대 2단계까지만 설정 가능합니다."),
    // 댓글
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "댓글을 찾을 수 없습니다."),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM002", "부모 댓글을 찾을 수 없습니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CM003", "대댓글에는 답글을 달 수 없습니다."),
    PARENT_COMMENT_DELETED(HttpStatus.BAD_REQUEST, "CM004", "삭제된 댓글에는 답글을 달 수 없습니다."),
    DELETED_COMMENT_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "CM005", "삭제된 댓글은 상태를 변경할 수 없습니다."),
    COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "CM006", "댓글 수정 권한이 없습니다."),
    DELETED_COMMENT_UPDATE(HttpStatus.BAD_REQUEST, "CM007", "삭제된 댓글은 수정할 수 없습니다."),
    COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "CM008", "댓글 삭제 권한이 없습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "CM009", "이미 삭제된 댓글입니다."),
    // 알림
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT001", "알림이 존재하지 않습니다."),
    // 포인트
    BALANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PT001", "포인트 계좌가 존재하지 않습니다."),
    BALANCE_NOT_FOUND_PENDING(HttpStatus.NOT_FOUND, "PT002", "포인트 계좌가 존재하지 않습니다! 승인 대기 중일 수 있습니다."),
    ITEM_NOT_PURCHASABLE(HttpStatus.BAD_REQUEST, "PT003", "구매할 수 없는 상품입니다."),
    NOT_SHOP_ITEM(HttpStatus.BAD_REQUEST, "PT004", "마일리지 구매 상품이 아닙니다."),
    ITEM_PRICE_NOT_SET(HttpStatus.BAD_REQUEST, "PT005", "상품 가격이 설정되지 않았습니다."),
    ITEM_BADGE_NOT_LINKED(HttpStatus.BAD_REQUEST, "PT006", "상품에 연결된 배지가 없습니다."),
    BADGE_ALREADY_OWNED(HttpStatus.BAD_REQUEST, "PT007", "이미 보유한 배지입니다."),
    INVALID_SPEND_AMOUNT(HttpStatus.BAD_REQUEST, "PT008", "차감할 마일리지가 올바르지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "PT009", "보유 마일리지가 부족합니다."),
    // 프로필 장식
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
    IMAGE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PD014", "배지 이미지 서비스를 사용할 수 없습니다."),
    // 태그
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TG001", "태그가 존재하지 않습니다."),
    TAG_NOT_IN_CATEGORY(HttpStatus.BAD_REQUEST, "TG002", "이 태그는 해당 카테고리에 속하지 않습니다."),
    // 관리자 사용자
    ADMIN_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US001", "User not found"),
    USER_PENDING_FAILED(HttpStatus.BAD_REQUEST, "US002", "User pending failed"),
    USER_CONVERT_ROLE_FAILED(HttpStatus.BAD_REQUEST, "US003", "User role convert failed"),
    USER_CONVERT_STATUS_FAILED(HttpStatus.BAD_REQUEST, "US004", "User status convert failed"),
    USER_ROLE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "US005", "User role not found"),
    USER_GRANT_ROLE_FAILED(HttpStatus.BAD_REQUEST, "US006", "User role grant failed"),
    USER_REVOKE_ROLE_FAILED(HttpStatus.BAD_REQUEST, "US007", "User role revoke failed"),
    AUTHORITY_NOT_EXIST(HttpStatus.BAD_REQUEST, "US008", "Authority not found"),
    // 인증
    CANNOT_AUTHORIZED(HttpStatus.UNAUTHORIZED, "AE001", "인증이 필요합니다."),
    AUTH_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AE002", "해당 유저 정보를 찾을 수 없습니다."),
    STATUS_PENDING(HttpStatus.FORBIDDEN, "AE003", "승인 대기 중인 계정입니다. 관리자 승인 후 로그인할 수 있습니다."),
    STATUS_REJECTED(HttpStatus.FORBIDDEN, "AE004", "승인이 거부된 계정입니다."),
    STATUS_NOT_APPROVED(HttpStatus.FORBIDDEN, "AE005", "승인되지 않은 계정입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AE006", "Invalid user ID or password"),
    // 사용자
    USER_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "UE001", "해당 정보를 가진 유저가 이미 존재합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "UE002", "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "UE003", "이미 사용 중인 닉네임입니다."),
    PROFILE_IMAGE_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "UE004", "프로필 이미지 키가 필요합니다."),
    // 계정 복구
    RECOVERY_SESSION_EXPIRED(HttpStatus.FORBIDDEN, "UR001", "인증 세션이 만료되었습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.UNAUTHORIZED, "UR002", "인증에 실패했습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "UR003", "이메일 인증이 완료되지 않은 요청입니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
