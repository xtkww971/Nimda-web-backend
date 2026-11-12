package judgeServer.domain.group.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.group.dto.TeamResponse;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.group.service.CtfTeamResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 참가자용 팀 조회 (로그인 필요).
 *
 * <pre>
 *   GET /api/ctf/teams/me  -> {team: null} 또는 {team: {id, name, description, capacity, memberCount, members}}
 * </pre>
 *
 * <p>프론트가 팀 소속 여부로 "팀에 소속되어 있지 않습니다" 화면을 띄울 때 쓴다.
 */
@RestController
@RequestMapping("/api/ctf/teams")
@RequiredArgsConstructor
public class CtfTeamController {

    private final CtfTeamResolver teamResolver;
    private final GroupRepository groupRepository;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> myTeam(@AuthenticationPrincipal CustomUserDetails user) {
        TeamResponse team = teamResolver.findTeamId(user.getUser().getId())
                .flatMap(groupRepository::findByIdWithMembers)
                .map(TeamResponse::from)
                .orElse(null);

        // Map.of는 null 값을 허용하지 않아 HashMap을 쓴다.
        Map<String, Object> body = new HashMap<>();
        body.put("team", team);
        return ApiResponse.ok(body).toResponse();
    }
}
