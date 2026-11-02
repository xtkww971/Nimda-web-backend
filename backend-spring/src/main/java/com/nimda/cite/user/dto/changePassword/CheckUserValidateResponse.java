package com.nimda.cite.user.dto.changePassword;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class CheckUserValidateResponse {
    private boolean isValidateUserId;
    private boolean isValidateStudentNum;
    private boolean isValidateEmail;
}
