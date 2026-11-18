package com.nimda.ctfgame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 물건 등록·수정. 수정할 때 key 는 바꾸지 않는다 (게임이 key 로 물건을 구분한다).
 *
 * @param challengeCode 연결할 문제 코드. 비우면 연결 없음
 * @param zone          게임 구역 번호 (0 로비, 1 Stage1, 2 Stage2)
 */
public record GameItemRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,49}$", message = "key 는 영문 소문자·숫자·- 로 50자 이하") String key,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 5000) String description,
        @Size(max = 50) String challengeCode,
        @NotNull @Min(0) @Max(50) Integer zone,
        @NotNull @Min(0) @Max(10000) Integer x,
        @NotNull @Min(0) @Max(10000) Integer y,
        @Min(-1000) @Max(1000) Integer sortOrder,
        Boolean visible) {
}
