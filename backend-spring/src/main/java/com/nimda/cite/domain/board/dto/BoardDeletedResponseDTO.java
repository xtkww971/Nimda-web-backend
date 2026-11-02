package com.nimda.cite.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 삭제된 게시글 조회 시 응답 DTO
 * - 프론트가 data.deleted로 읽으므로 deleted 키를 유지한다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardDeletedResponseDTO {
    private boolean deleted;

    public static BoardDeletedResponseDTO of() {
        return BoardDeletedResponseDTO.builder()
                .deleted(true)
                .build();
    }
}
