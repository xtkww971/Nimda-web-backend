package com.nimda.cite.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 작성 댓글 목록 응답 DTO (마이페이지/공개 프로필, data.comments)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyCommentListResponseDTO {
    private List<MyCommentResponse> comments;

    public static MyCommentListResponseDTO of(List<MyCommentResponse> comments) {
        return MyCommentListResponseDTO.builder()
                .comments(comments)
                .build();
    }
}
