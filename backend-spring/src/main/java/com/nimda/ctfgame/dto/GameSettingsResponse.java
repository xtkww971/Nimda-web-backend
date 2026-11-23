package com.nimda.ctfgame.dto;

import java.time.OffsetDateTime;

/**
 * 게임 설정과 그것을 현재 대회에 적용한 값. 시각은 모두 KST(+09:00).
 * 게임은 남은 시간을 참가자 PC 시계가 아니라 serverTime 기준으로 계산한다 (일정 API 와 같은 방식).
 *
 * @param stage2OpenMinutes 대회 시작 후 몇 분 뒤 Stage2 를 모두에게 여는지. null 이면 시간 개방 없음
 * @param stage2OpenAt      실제로 여는 시각 = 현재 대회 시작 + stage2OpenMinutes. 시간 개방이 없거나 대회가 없으면 null
 * @param stage2Open        지금 시간 개방이 된 상태인지 (serverTime ≥ stage2OpenAt)
 * @param serverTime        서버 시각
 * @param updatedAt         마지막으로 바꾼 때 (운영진 응답에만, 참가자 응답은 null)
 */
public record GameSettingsResponse(Integer stage2OpenMinutes, OffsetDateTime stage2OpenAt, boolean stage2Open,
                                   OffsetDateTime serverTime, OffsetDateTime updatedAt) {
}
