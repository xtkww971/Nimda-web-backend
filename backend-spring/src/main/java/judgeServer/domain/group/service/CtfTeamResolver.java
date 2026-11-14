package judgeServer.domain.group.service;

import com.nimda.cite.common.exception.NotIncludeRoleException;
import com.nimda.cite.common.exception.UnauthorizedException;
import com.nimda.cite.common.exception.errorcode.ctf.TeamErrorCode;
import com.nimda.cite.common.exception.errorcode.user.AuthErrorCode;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.groupMember.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * CTF는 팀전이다. 유저가 현재 대회에서 어느 팀에 속했는지 찾고, 팀이 없는 참가자의 문제 접근을 막는다.
 *
 * <p>관리자와 DEV는 출제·검수를 해야 하므로 팀이 없어도 통과시킨다. 다만 팀이 없으면 점수는 기록되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class CtfTeamResolver {

    private final GroupMemberRepository memberRepository;
    private final ContestService contestService;

    /** 현재 대회에서 속한 팀. 지난 대회 팀은 보지 않는다. */
    public Optional<Long> findTeamId(Long userId) {
        List<Long> teamIds = memberRepository.findGroupIdsByUserIdInContest(userId,
                contestService.currentContestId().orElse(null));
        return teamIds.isEmpty() ? Optional.empty() : Optional.of(teamIds.get(0));
    }

    /** 팀이 없는 일반 참가자면 403. 관리자·DEV는 통과. */
    public void requireTeamOrStaff(CustomUserDetails user) {
        if (user == null) {
            throw new UnauthorizedException(AuthErrorCode.CANNOT_AUTHORIZED);
        }
        if (isStaff(user)) {
            return;
        }
        if (findTeamId(user.getUser().getId()).isEmpty()) {
            throw new NotIncludeRoleException(TeamErrorCode.NO_TEAM);
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
