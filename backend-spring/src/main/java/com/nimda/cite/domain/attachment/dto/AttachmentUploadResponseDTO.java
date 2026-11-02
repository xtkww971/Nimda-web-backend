package com.nimda.cite.domain.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 첨부파일 업로드/등록 응답 DTO (data.attachmentId)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUploadResponseDTO {
    private Long attachmentId;

    public static AttachmentUploadResponseDTO of(Long attachmentId) {
        return AttachmentUploadResponseDTO.builder()
                .attachmentId(attachmentId)
                .build();
    }
}
