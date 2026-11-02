package com.nimda.cite.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 단건 응답 DTO (작성/수정/숨김, data.comment)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentDetailResponseDTO {
    private CommentResponse comment;

    public static CommentDetailResponseDTO of(CommentResponse comment) {
        return CommentDetailResponseDTO.builder()
                .comment(comment)
                .build();
    }
}
