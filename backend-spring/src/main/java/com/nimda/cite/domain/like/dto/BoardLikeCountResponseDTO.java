package com.nimda.cite.domain.like.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좋아요 누른 게시글 수 응답 DTO (data.likeCount)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardLikeCountResponseDTO {
    private long likeCount;

    public static BoardLikeCountResponseDTO of(long likeCount) {
        return BoardLikeCountResponseDTO.builder()
                .likeCount(likeCount)
                .build();
    }
}
