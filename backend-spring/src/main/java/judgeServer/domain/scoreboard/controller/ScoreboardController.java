package judgeServer.domain.scoreboard.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.scoreboard.service.ScoreboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * CTF 팀 점수판 (로그인 필요). 팀이 없는 사용자도 볼 수 있다.
 *
 * <pre>
 *   GET /api/ctf/scoreboard?page=0&size=50  -> 점수가 있는 팀의 순위 목록 (size 최대 100)
 *   GET /api/ctf/scoreboard/me              -> 내 팀 점수·순위·멤버별 기여·최근 해결. 팀이 없으면 {team: null}
 * </pre>
 *
 * <p>순위: 점수 내림차순, 동점이면 마지막 득점이 이른 팀이 위. 점수가 0인 팀은 순위에 나오지 않는다.
 */
@RestController
@RequestMapping(UrlNames.scoreboardUrl)
@RequiredArgsConstructor
public class ScoreboardController {

    private final ScoreboardService scoreboardService;

    @GetMapping(UrlNames.getScoreboardUrl)
    public ResponseEntity<?> scoreboard(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(scoreboardService.getScoreboard(page, size)).toResponse();
    }

    @GetMapping(UrlNames.getMyTeamScoreUrl)
    public ResponseEntity<?> myTeam(@AuthenticationPrincipal CustomUserDetails user) {
        // Map.of는 null 값을 허용하지 않아 HashMap을 쓴다.
        Map<String, Object> body = new HashMap<>();
        body.put("team", scoreboardService.getMyTeam(user.getUser().getId()).orElse(null));
        return ApiResponse.ok(body).toResponse();
    }
}
