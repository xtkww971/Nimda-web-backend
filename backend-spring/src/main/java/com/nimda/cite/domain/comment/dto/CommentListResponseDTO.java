package com.nimda.cite.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 게시글 댓글 목록 응답 DTO (data.comments)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentListResponseDTO {
    private List<CommentResponse> comments;

    public static CommentListResponseDTO of(List<CommentResponse> comments) {
        return CommentListResponseDTO.builder()
                .comments(comments)
                .build();
    }
}
