package com.nimda.cite.domain.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 첨부파일 다운로드 URL 응답 DTO (data.downloadUrl)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDownloadUrlResponseDTO {
    private String downloadUrl;

    public static AttachmentDownloadUrlResponseDTO of(String downloadUrl) {
        return AttachmentDownloadUrlResponseDTO.builder()
                .downloadUrl(downloadUrl)
                .build();
    }
}
