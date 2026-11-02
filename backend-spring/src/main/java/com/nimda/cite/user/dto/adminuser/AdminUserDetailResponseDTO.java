package com.nimda.cite.user.dto.adminuser;

import com.nimda.cite.user.entity.Authority;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민 페이지 유저 상세 정보 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponseDTO {

    private Long id;
    private String userId;
    private String name;
    private String nickname;
    private String email;
    private String studentNum;
    private String major;
    private String birth;

    private ApprovalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roles;

    private String profileImage;
    private String profileDecoration;

    public static AdminUserDetailResponseDTO from(User user){
        return builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .studentNum(user.getStudentNum())
                .major(user.getMajor())
                .birth(user.getBirth())

                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())

                .roles(user.getAuthorities().stream()
                        .map(Authority::getAuthorityName)
                        .toList())

                .profileImage(user.getProfileImage())
                .profileDecoration(user.getProfileDecoration())

                .build();
    }

}
