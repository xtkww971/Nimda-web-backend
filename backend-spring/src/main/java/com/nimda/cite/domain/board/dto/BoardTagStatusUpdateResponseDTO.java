package com.nimda.cite.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 - 태그 기반 게시글 활성화/비활성화 응답 DTO
 * - 기존 응답 모양({success, message, affectedCount})을 유지하기 위해 ApiResponse로 감싸지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardTagStatusUpdateResponseDTO {
    private boolean success;
    private String message;
    private int affectedCount;

    public static BoardTagStatusUpdateResponseDTO of(String message, int affectedCount) {
        return BoardTagStatusUpdateResponseDTO.builder()
                .success(true)
                .message(message)
                .affectedCount(affectedCount)
                .build();
    }
}
