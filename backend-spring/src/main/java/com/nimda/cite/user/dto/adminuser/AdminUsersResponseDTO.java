package com.nimda.cite.user.dto.adminuser;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 어드민 사용자 목록 응답 DTO ({success, users} / 승인 대기 목록은 {success, users, count})
 * - 프론트가 최상위 필드(users)를 바로 읽으므로 ApiResponse로 감싸지 않는다.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUsersResponseDTO<T> {
    private boolean success;
    private List<T> users;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer count;

    public static <T> AdminUsersResponseDTO<T> of(List<T> users) {
        return AdminUsersResponseDTO.<T>builder()
                .success(true)
                .users(users)
                .build();
    }

    public static <T> AdminUsersResponseDTO<T> withCount(List<T> users) {
        return AdminUsersResponseDTO.<T>builder()
                .success(true)
                .users(users)
                .count(users.size())
                .build();
    }
}
