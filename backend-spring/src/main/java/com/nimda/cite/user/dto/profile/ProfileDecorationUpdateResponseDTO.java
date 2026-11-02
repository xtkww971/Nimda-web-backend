package com.nimda.cite.user.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 장식 변경 응답 DTO
 * - 프론트가 최상위 필드(profileDecoration)를 바로 읽으므로 ApiResponse로 감싸지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDecorationUpdateResponseDTO {
    private boolean success;
    private String message;
    private String profileDecoration;

    public static ProfileDecorationUpdateResponseDTO of(String message, String profileDecoration) {
        return ProfileDecorationUpdateResponseDTO.builder()
                .success(true)
                .message(message)
                .profileDecoration(profileDecoration)
                .build();
    }
}
