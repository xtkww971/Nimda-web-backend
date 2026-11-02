package com.nimda.cite.user.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일 숨김 설정 변경 응답 DTO
 * - 프론트가 최상위 필드(emailHide)를 바로 읽으므로 ApiResponse로 감싸지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailHideUpdateResponseDTO {
    private boolean success;
    private boolean emailHide;
    private String message;

    public static EmailHideUpdateResponseDTO of(String message, boolean emailHide) {
        return EmailHideUpdateResponseDTO.builder()
                .success(true)
                .emailHide(emailHide)
                .message(message)
                .build();
    }
}
