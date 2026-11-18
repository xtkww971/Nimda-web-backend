package judgeServer.domain.challenge.exposure;

import java.util.Set;

/**
 * 게임(CTF 게임 이벤트)에 나온 문제. 구현은 이벤트 모듈 {@code com.nimda.ctfgame} 에 있고
 * ({@code CTF_GAME_ENABLED=true} 일 때만 빈이 생긴다), judgeServer 는 이 인터페이스만 안다.
 * 빈이 없으면 게임 제한이 없다 → 공개 문제 전부 (이벤트가 끝나 ctfgame 을 지워도 judgeServer 는 그대로 돈다).
 */
public interface GameLinkedChallenges {

    /** {@code visible} 게임 물건에 연결된 문제 id (초안 포함 여부는 따지지 않는다) */
    Set<Long> visibleLinkedChallengeIds();
}
