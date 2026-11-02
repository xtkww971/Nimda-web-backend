package com.nimda.cite.user.dto.adminuser;

import com.nimda.cite.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponseDTO {

    private Long id;
    private String userId;
    private String nickname;
    private String email;
    private LocalDateTime createdAt;

    public static AdminUserListResponseDTO from(User user){
        return builder()
                .id(user.getId())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
