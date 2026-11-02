package com.nimda.cite.user.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nimda.cite.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 닉네임으로 조회하는 공개 프로필 응답 DTO
 * - 이메일은 사용자가 숨김 설정한 경우 응답에서 제외 (null이면 필드 생략)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicProfileResponseDTO {
    private String nickname;
    private String profileImage;
    private String profileDecoration;
    private String bojId;
    private String major;
    private LocalDateTime createdAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;

    public static UserPublicProfileResponseDTO of(User user, String profileImageUrl) {
        return UserPublicProfileResponseDTO.builder()
                .nickname(user.getNickname())
                .profileImage(profileImageUrl)
                .profileDecoration(user.getProfileDecoration())
                .bojId(user.getBojId())
                .major(user.getMajor())
                .createdAt(user.getCreatedAt())
                .email(user.isEmailHide() ? null : user.getEmail())
                .build();
    }
}
