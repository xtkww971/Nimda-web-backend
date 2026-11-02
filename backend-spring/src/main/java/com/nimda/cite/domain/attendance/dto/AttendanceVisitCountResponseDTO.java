package com.nimda.cite.domain.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내 누적 출석 횟수 응답 DTO (data.visitCount)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceVisitCountResponseDTO {
    private Long visitCount;

    public static AttendanceVisitCountResponseDTO of(Long visitCount) {
        return AttendanceVisitCountResponseDTO.builder()
                .visitCount(visitCount)
                .build();
    }
}
