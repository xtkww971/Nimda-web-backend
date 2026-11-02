package com.nimda.cite.domain.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * S3 업로드용 Presigned URL 응답 DTO (data.uploadUrl, data.key)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentPresignedUploadResponseDTO {
    private String uploadUrl;
    private String key;

    public static AttachmentPresignedUploadResponseDTO of(String uploadUrl, String key) {
        return AttachmentPresignedUploadResponseDTO.builder()
                .uploadUrl(uploadUrl)
                .key(key)
                .build();
    }
}
