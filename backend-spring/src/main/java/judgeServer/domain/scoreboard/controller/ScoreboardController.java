package judgeServer.domain.scoreboard.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.contest.entity.Contest;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
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
import java.util.Optional;

/**
 * CTF 팀 점수판 (로그인 필요). 팀이 없는 사용자도 볼 수 있다.
 *
 * <pre>
 *   GET /api/ctf/scoreboard?page=0&size=50  -> 점수가 있는 팀의 순위 목록 (size 최대 100)
 *   GET /api/ctf/scoreboard/me              -> 내 팀 점수·순위·멤버별 기여·최근 해결. 팀이 없으면 {team: null}
 * </pre>
 *
 * <p>순위: 점수 내림차순, 동점이면 마지막 득점이 이른 팀이 위. 점수가 0인 팀은 순위에 나오지 않는다.
 *
 * <p><b>순위 비공개</b>: 대회 설정에서 켜면 종료 N분 전부터 종료까지 참가자에게 두 경로 모두
 * {@code {hidden: true, hiddenFrom, revealAt}} 만 준다 (모든 팀의 순위·점수, 우리 팀 점수·등수·기여·최근 해결 전부 없음).
 * 종료와 함께 전체가 다시 보인다. ADMIN·DEV 는 언제나 전체를 본다.
 */
@RestController
@RequestMapping(UrlNames.scoreboardUrl)
@RequiredArgsConstructor
public class ScoreboardController {

    private final ScoreboardService scoreboardService;
    private final ContestService contestService;

    @GetMapping(UrlNames.getScoreboardUrl)
    public ResponseEntity<?> scoreboard(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        Optional<Map<String, Object>> hidden = hiddenFor(user);
        if (hidden.isPresent()) {
            return ApiResponse.ok(hidden.get()).toResponse();
        }
        return ApiResponse.ok(scoreboardService.getScoreboard(page, size)).toResponse();
    }

    @GetMapping(UrlNames.getMyTeamScoreUrl)
    public ResponseEntity<?> myTeam(@AuthenticationPrincipal CustomUserDetails user) {
        Optional<Map<String, Object>> hidden = hiddenFor(user);
        if (hidden.isPresent()) {
            return ApiResponse.ok(hidden.get()).toResponse();
        }
        // Map.of는 null 값을 허용하지 않아 HashMap을 쓴다.
        Map<String, Object> body = new HashMap<>();
        body.put("team", scoreboardService.getMyTeam(user.getUser().getId()).orElse(null));
        return ApiResponse.ok(body).toResponse();
    }

    /** 지금 이 사용자에게 순위를 가려야 하면 가림 응답. ADMIN·DEV 는 가리지 않는다. */
    Optional<Map<String, Object>> hiddenFor(CustomUserDetails user) {
        if (CtfTeamResolver.isStaff(user)) {
            return Optional.empty();
        }
        Contest c = contestService.current().orElse(null);
        if (c == null || !ContestService.isScoreboardHidden(c, contestService.now())) {
            return Optional.empty();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("hidden", true);
        body.put("hiddenFrom", ContestService.toKst(ContestService.scoreboardHiddenFrom(c).orElse(null)));
        body.put("revealAt", ContestService.toKst(c.getEndsAt()));
        return Optional.of(body);
    }
}
