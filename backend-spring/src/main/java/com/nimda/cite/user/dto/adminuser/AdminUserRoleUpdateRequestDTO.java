package com.nimda.cite.user.dto.adminuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRoleUpdateRequestDTO {
    private String role;
}
