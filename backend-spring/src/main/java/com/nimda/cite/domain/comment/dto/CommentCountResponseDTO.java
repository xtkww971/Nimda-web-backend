package com.nimda.cite.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내가 작성한 댓글 수 응답 DTO (data.commentCount)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentCountResponseDTO {
    private long commentCount;

    public static CommentCountResponseDTO of(long commentCount) {
        return CommentCountResponseDTO.builder()
                .commentCount(commentCount)
                .build();
    }
}
