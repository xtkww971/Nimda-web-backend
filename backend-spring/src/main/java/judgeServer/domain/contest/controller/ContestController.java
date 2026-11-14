package judgeServer.domain.contest.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.contest.service.ContestHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 대회 이력 (로그인한 모든 사용자). 종료 후 확정된 스냅샷만 보여준다.
 *
 * <pre>
 *   GET /api/ctf/contests?participated=false&sort=latest -> {current, ended[]}
 *       current: 아직 확정 전인 현재 대회 (배너용, 없으면 null)
 *       ended  : 확정된 대회 + 우승 팀 + 내 결과 (sort=oldest면 오래된 순)
 *   GET /api/ctf/contests/{id}                            -> 요약 통계, 최종 순위, 문제별 결과, 내 팀
 * </pre>
 */
@RestController
@RequestMapping(UrlNames.contestUrl)
@RequiredArgsConstructor
public class ContestController {

    private final ContestHistoryService historyService;

    @GetMapping(UrlNames.getContestsUrl)
    public ResponseEntity<?> list(@RequestParam(defaultValue = "false") boolean participated,
                                  @RequestParam(defaultValue = "latest") String sort,
                                  @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok(historyService.list(user.getUser().getId(), participated, "oldest".equalsIgnoreCase(sort)))
                .toResponse();
    }

    @GetMapping(UrlNames.getContestDetailUrl)
    public ResponseEntity<?> detail(@PathVariable Long contestId,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok(historyService.detail(contestId, user.getUser().getId())).toResponse();
    }
}
