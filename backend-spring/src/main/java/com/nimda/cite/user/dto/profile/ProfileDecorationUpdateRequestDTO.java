package com.nimda.cite.user.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDecorationUpdateRequestDTO {
    private String profileDecorationKey;
}
