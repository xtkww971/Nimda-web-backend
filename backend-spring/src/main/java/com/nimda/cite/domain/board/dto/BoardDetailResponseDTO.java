package com.nimda.cite.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 단건 응답 DTO (작성/조회/수정/고정)
 * - 프론트가 data.board로 읽으므로 board 키를 유지한다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardDetailResponseDTO {
    private BoardResponseDTO board;

    public static BoardDetailResponseDTO of(BoardResponseDTO board) {
        return BoardDetailResponseDTO.builder()
                .board(board)
                .build();
    }
}
