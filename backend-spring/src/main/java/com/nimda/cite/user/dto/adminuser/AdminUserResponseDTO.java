package com.nimda.cite.user.dto.adminuser;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 어드민 사용자 단건 응답 DTO ({success, user} / 승인·거부는 {success, message, user})
 * - 프론트가 최상위 필드(user)를 바로 읽으므로 ApiResponse로 감싸지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDTO {
    private boolean success;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    private AdminUserDetailResponseDTO user;

    public static AdminUserResponseDTO of(AdminUserDetailResponseDTO user) {
        return of(null, user);
    }

    public static AdminUserResponseDTO of(String message, AdminUserDetailResponseDTO user) {
        return AdminUserResponseDTO.builder()
                .success(true)
                .message(message)
                .user(user)
                .build();
    }
}
