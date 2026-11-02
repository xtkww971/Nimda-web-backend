package com.nimda.cite.user.dto.adminuser;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 어드민 권한 목록 응답 DTO ({success, roles} / 권한 부여·제거는 {success, message, roles})
 * - 프론트가 최상위 필드(roles)를 바로 읽으므로 ApiResponse로 감싸지 않는다.
 * - 전체 권한 목록 조회, 권한 부여/제거 후 사용자의 권한 목록에 사용한다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRolesResponseDTO {
    private boolean success;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    private List<String> roles;

    public static AdminUserRolesResponseDTO of(List<String> roles) {
        return of(null, roles);
    }

    public static AdminUserRolesResponseDTO of(String message, List<String> roles) {
        return AdminUserRolesResponseDTO.builder()
                .success(true)
                .message(message)
                .roles(roles)
                .build();
    }
}
