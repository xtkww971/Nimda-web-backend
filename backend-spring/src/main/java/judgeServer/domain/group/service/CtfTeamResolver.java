package judgeServer.domain.group.service;

import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.groupMember.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * CTF는 팀전이다. 유저가 어느 팀에 속했는지 찾고, 팀이 없는 참가자의 문제 접근을 막는다.
 *
 * <p>관리자와 DEV는 출제·검수를 해야 하므로 팀이 없어도 통과시킨다. 다만 팀이 없으면 점수는 기록되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class CtfTeamResolver {

    public static final String NO_TEAM_MESSAGE = "팀에 소속되어 있지 않습니다. 운영진이 팀을 배정하면 문제가 공개됩니다.";

    private final GroupMemberRepository memberRepository;

    public Optional<Long> findTeamId(Long userId) {
        List<Long> teamIds = memberRepository.findGroupIdsByUserId(userId);
        return teamIds.isEmpty() ? Optional.empty() : Optional.of(teamIds.get(0));
    }

    /** 팀이 없는 일반 참가자면 403. 관리자·DEV는 통과. */
    public void requireTeamOrStaff(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (isStaff(user)) {
            return;
        }
        if (findTeamId(user.getUser().getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NO_TEAM_MESSAGE);
        }
    }

    public static boolean isStaff(CustomUserDetails user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_DEV".equals(role));
    }
}
