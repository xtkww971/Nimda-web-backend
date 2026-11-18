package judgeServer.domain.challenge.exposure;

import com.nimda.cite.common.exception.GlobalNimdaException;
import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.user.security.CustomUserDetails;
import java.util.Optional;
import java.util.Set;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.challenge.service.SolvedChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 누가 어떤 문제를 열 수 있는지 (게임 모드, docs/api/CTF API 명세.md 2절 "노출 규칙").
 * 상세·첨부파일·인스턴스가 같은 규칙을 쓴다.
 *
 * <ul>
 *   <li>DEV: 전부 (초안 포함)</li>
 *   <li>ADMIN: 공개 문제 전부</li>
 *   <li>참가자: 공개 + 게임에 나온 문제({@link GameLinkedChallenges}), 또는 우리 팀이 푼 문제.
 *       게임 기능이 꺼져 있으면 공개 문제 전부</li>
 * </ul>
 * 규칙 밖은 없는 문제와 같은 404 (코드로 문제 존재를 알아낼 수 없게).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeExposure {

    private final ChallengeRepository challengeRepository;
    private final SolvedChallengeService solvedChallengeService;
    private final ObjectProvider<GameLinkedChallenges> gameLinked;

    /** 코드로 문제를 찾고, 이 사용자에게 열리지 않으면 404. */
    public Challenge requireOpen(String code, CustomUserDetails user) {
        Challenge challenge = challengeRepository.findByCode(code)
                .orElseThrow(() -> new GlobalNimdaException(ErrorCode.CHALLENGE_NOT_FOUND));
        if (!isOpen(challenge, user)) {
            throw new GlobalNimdaException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        return challenge;
    }

    public boolean isOpen(Challenge challenge, CustomUserDetails user) {
        if (hasRole(user, "ROLE_DEV")) {
            return true;
        }
        if (!Boolean.TRUE.equals(challenge.getIsPublic())) {
            return false;
        }
        if (hasRole(user, "ROLE_ADMIN")) {
            return true;
        }
        Optional<Set<Long>> linked = linkedIds();
        if (linked.isEmpty() || linked.get().contains(challenge.getId())) {
            return true;
        }
        // 물건을 숨겨도 이미 푼 팀은 계속 다시 본다
        return solvedChallengeService.solvedFor(user, challenge.getCode()).isPresent();
    }

    /** 참가자가 찾을 수 있는 문제 수 (대시보드 "3 / 12" 의 분모). 누가 불러도 같다. */
    public long participantTotal() {
        Optional<Set<Long>> linked = linkedIds();
        return challengeRepository.findAllByOrderByIdAsc().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPublic()))
                .filter(c -> linked.isEmpty() || linked.get().contains(c.getId()))
                .count();
    }

    /** 게임 기능이 꺼져 있으면 empty (제한 없음) */
    private Optional<Set<Long>> linkedIds() {
        GameLinkedChallenges game = gameLinked.getIfAvailable();
        return game == null ? Optional.empty() : Optional.of(game.visibleLinkedChallengeIds());
    }

    private static boolean hasRole(CustomUserDetails user, String role) {
        return user != null && user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
