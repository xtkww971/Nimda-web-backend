package com.nimda.cite.domain.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 첨부파일 메타정보 조회 응답 DTO (data.attachment)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDetailResponseDTO {
    private AttachmentResponseDto attachment;

    public static AttachmentDetailResponseDTO of(AttachmentResponseDto attachment) {
        return AttachmentDetailResponseDTO.builder()
                .attachment(attachment)
                .build();
    }
}
