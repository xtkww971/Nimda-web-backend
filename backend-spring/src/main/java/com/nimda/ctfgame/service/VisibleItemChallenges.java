package com.nimda.ctfgame.service;

import com.nimda.ctfgame.repository.GameItemRepository;
import java.util.HashSet;
import java.util.Set;
import judgeServer.domain.challenge.exposure.GameLinkedChallenges;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * judgeServer 의 {@link GameLinkedChallenges} 구현: 보이는(visible) 게임 물건에 연결된 문제.
 * 게임 물건 기능이 켜져 있을 때만 빈이 생기고, 없으면 judgeServer 는 제한 없이 공개 문제 전부를 연다.
 * 의존 방향: ctfgame → judgeServer (인터페이스). judgeServer 는 ctfgame 을 모른다.
 */
@Component
@ConditionalOnProperty(prefix = "nimda.ctf-game", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class VisibleItemChallenges implements GameLinkedChallenges {

    private final GameItemRepository repository;

    @Override
    public Set<Long> visibleLinkedChallengeIds() {
        return new HashSet<>(repository.findVisibleLinkedChallengeIds());
    }
}
