package com.nimda.ctfgame.dto;

import java.time.LocalDateTime;

/** 관리자 화면용: 숨긴 물건과 이야기까지 전부. */
public record AdminGameItemResponse(
        Long id,
        String key,
        String name,
        String description,
        String challengeCode,
        String challengeTitle,
        int zone,
        int x,
        int y,
        int sortOrder,
        boolean visible,
        LocalDateTime updatedAt) {
}
