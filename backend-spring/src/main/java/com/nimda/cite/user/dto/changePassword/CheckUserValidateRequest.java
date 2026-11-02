package com.nimda.cite.user.dto.changePassword;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CheckUserValidateRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String studentNum;

    @NotBlank
    private String email;
}
