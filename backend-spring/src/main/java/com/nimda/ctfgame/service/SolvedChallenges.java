package com.nimda.ctfgame.service;

import java.util.Set;

/**
 * 사용자 쪽에서 "푼 문제" 집합. 아이템 소유 계산의 기준이다.
 * 실제 구현은 {@link TeamSolvedChallenges} (팀 공유). 테스트에서는 람다로 바꿔 끼운다.
 */
@FunctionalInterface
public interface SolvedChallenges {
    Set<Long> of(Long userId);
}
