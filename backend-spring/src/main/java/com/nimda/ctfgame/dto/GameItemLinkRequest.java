package com.nimda.ctfgame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 문제 쪽에서 물건 연결 (PUT /api/ctf/game/admin/challenges/{code}/items/{key}). 비운 값은 그대로 둔다.
 *
 * @param name                   물건이 없어 새로 만들 때 필수
 * @param moveFromOtherChallenge true 면 다른 문제에 연결된 물건도 이 문제로 옮긴다. 아니면 409
 */
public record GameItemLinkRequest(
        @Size(max = 100) String name,
        @Size(max = 5000) String description,
        Boolean visible,
        @Min(-1000) @Max(1000) Integer sortOrder,
        Boolean moveFromOtherChallenge) {
}
