package com.nimda.cite.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 성공 응답 DTO
 * - 프론트가 최상위 필드(user)를 바로 읽으므로 ApiResponse로 감싸지 않는다.
 * - 토큰은 HttpOnly 쿠키로만 전달하고 응답 바디에는 포함하지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginSuccessResponseDTO {
    private boolean success;
    private String message;
    private LoginResponseDTO.UserInfo user;

    public static LoginSuccessResponseDTO of(String message, LoginResponseDTO.UserInfo user) {
        return LoginSuccessResponseDTO.builder()
                .success(true)
                .message(message)
                .user(user)
                .build();
    }
}
