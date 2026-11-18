package com.nimda.ctfgame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 물건 정보 등록·수정. 수정할 때 key 는 바꾸지 않는다 (게임 objects.json 의 물건과 key 로 이어진다).
 * 위치·그림은 받지 않는다 (게임 저장소가 맡는다, CTF_INTEGRATION D12).
 *
 * @param key           게임 objects.json 의 key 와 같은 값
 * @param challengeCode 연결할 문제 코드. 비우면 연결 없음
 */
public record GameItemRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,49}$", message = "key 는 영문 소문자·숫자·- 로 50자 이하") String key,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 5000) String description,
        @Size(max = 50) String challengeCode,
        @Min(-1000) @Max(1000) Integer sortOrder,
        Boolean visible) {
}
