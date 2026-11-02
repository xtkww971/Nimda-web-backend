package com.nimda.cite.domain.profiledecoration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 배지 지급/회수 요청 DTO
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDecorationGrantRequestDTO {
    private String studentNum;
    private Long decorationId;
}
