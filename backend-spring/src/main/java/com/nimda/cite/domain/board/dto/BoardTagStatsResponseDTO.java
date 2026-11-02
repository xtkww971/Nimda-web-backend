package com.nimda.cite.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리자 - 카테고리의 태그별 게시글 통계 응답 DTO
 * - 기존 응답 모양({success, categoryId, tagStats})을 유지하기 위해 ApiResponse로 감싸지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardTagStatsResponseDTO {
    private boolean success;
    private Long categoryId;
    private List<TagStatInfo> tagStats;

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagStatInfo {
        private Long tagId;
        private String tagName;
        private long activeCount;
        private long hiddenCount;
    }

    public static BoardTagStatsResponseDTO of(Long categoryId, List<TagStatInfo> tagStats) {
        return BoardTagStatsResponseDTO.builder()
                .success(true)
                .categoryId(categoryId)
                .tagStats(tagStats)
                .build();
    }
}
