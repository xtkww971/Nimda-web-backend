package com.nimda.cite.user.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 이미지 변경 응답 DTO
 * - 프론트가 최상위 필드(profileImageUrl 등)를 바로 읽으므로 ApiResponse로 감싸지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageUpdateResponseDTO {
    private boolean success;
    private String message;
    private String profileImageUrl;
    private String profileImageKey;

    public static ProfileImageUpdateResponseDTO of(String message, String profileImageUrl, String profileImageKey) {
        return ProfileImageUpdateResponseDTO.builder()
                .success(true)
                .message(message)
                .profileImageUrl(profileImageUrl)
                .profileImageKey(profileImageKey)
                .build();
    }
}
