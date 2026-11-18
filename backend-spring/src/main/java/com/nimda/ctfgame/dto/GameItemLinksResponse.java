package com.nimda.ctfgame.dto;

import java.util.List;

/**
 * 문제별 연결 요약 (문제 관리 표의 "게임 물건 key" 칸).
 *
 * @param links         모든 문제(초안 포함, id 순)와 거기 연결된 물건. 연결이 없으면 items 가 빈 목록
 * @param unlinkedItems 어느 문제에도 연결되지 않은 물건 (게임에서는 장식)
 */
public record GameItemLinksResponse(List<ChallengeLink> links, List<LinkedItem> unlinkedItems) {

    public record ChallengeLink(String challengeCode, List<LinkedItem> items) {
    }

    public record LinkedItem(Long id, String key, String name, boolean visible) {
    }
}
