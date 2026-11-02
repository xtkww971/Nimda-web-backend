package com.nimda.cite.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내가 작성한 게시글 수 응답 DTO
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardPostCountResponseDTO {
    private long postCount;

    public static BoardPostCountResponseDTO of(long postCount) {
        return BoardPostCountResponseDTO.builder()
                .postCount(postCount)
                .build();
    }
}
