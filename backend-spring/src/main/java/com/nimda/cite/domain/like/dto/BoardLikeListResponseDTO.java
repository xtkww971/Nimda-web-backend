package com.nimda.cite.domain.like.dto;

import com.nimda.cite.domain.board.dto.BoardResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좋아요 누른 게시글 목록 응답 DTO (data.boards)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardLikeListResponseDTO {
    private List<BoardResponseDTO> boards;

    public static BoardLikeListResponseDTO of(List<BoardResponseDTO> boards) {
        return BoardLikeListResponseDTO.builder()
                .boards(boards)
                .build();
    }
}
