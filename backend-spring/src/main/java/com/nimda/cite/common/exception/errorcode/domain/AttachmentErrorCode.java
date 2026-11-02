package com.nimda.cite.common.exception.errorcode.domain;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AttachmentErrorCode implements ErrorCode {
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
    FILE_READ_FORBIDDEN(HttpStatus.FORBIDDEN, "UF017", "파일에 접근할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}