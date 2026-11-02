package com.nimda.cite.domain.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 내 첨부파일 목록 응답 DTO (data.attachments)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentListResponseDTO {
    private List<AttachmentResponseDto> attachments;

    public static AttachmentListResponseDTO of(List<AttachmentResponseDto> attachments) {
        return AttachmentListResponseDTO.builder()
                .attachments(attachments)
                .build();
    }
}
