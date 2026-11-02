package com.nimda.cite.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 게시글 작성/수정 요청 DTO
 * - 컨트롤러는 기존과 동일하게 @RequestParam으로 받고, 서비스에는 이 DTO로 묶어서 전달한다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardWriteRequestDTO {
    private Long categoryId;
    private String title;
    private String content;
    private Long tagId;
    private List<Long> attachmentIds;
    private Boolean pinned;
    private Long itemPrice;
    private String itemType;
    private Long profileDecorationId;
    private Long thumbnailAttachmentId;
}
