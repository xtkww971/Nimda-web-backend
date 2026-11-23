package com.nimda.ctfgame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 게임 설정 바꾸기 (운영진).
 *
 * @param stage2OpenMinutes 대회 시작 후 몇 분 뒤 Stage2 를 모두에게 열지 (1~1440). null 이면 시간 개방 없음
 */
public record GameSettingsRequest(
        @Min(value = 1, message = "Stage2 개방은 대회 시작 1분 뒤부터 정할 수 있습니다.")
        @Max(value = 1440, message = "Stage2 개방은 대회 시작 1440분(24시간) 뒤까지 정할 수 있습니다.")
        Integer stage2OpenMinutes) {
}
