package judgeServer.domain.contest.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import judgeServer.domain.contest.dto.ContestSaveRequest;
import judgeServer.domain.contest.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 운영자 대회 관리 (ADMIN 전용).
 *
 * <pre>
 *   GET  /api/admin/ctf/contests                   -> 전체 대회 (최신순, current 표시)
 *   POST /api/admin/ctf/contests                   -> 새 대회. 이전 대회 결과가 확정된 뒤에만 가능
 *   POST /api/admin/ctf/contests/{id}/finalize     -> 종료된 대회 결과를 지금 확정 (이미 확정됐으면 다시 만든다)
 * </pre>
 *
 * <p>종료 후 1분이 지나면 결과는 자동으로 확정된다.
 */
@RestController
@RequestMapping("/api/admin/ctf/contests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminContestController {

    private final ContestService contestService;

    @GetMapping
    public ResponseEntity<?> list() {
        return ApiResponse.ok(contestService.listForAdmin()).toResponse();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ContestSaveRequest request,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok("대회가 생성되었습니다.", contestService.create(request, user.getUser().getId()))
                .toResponse(HttpStatus.CREATED);
    }

    @PostMapping("/{contestId}/finalize")
    public ResponseEntity<?> finalizeContest(@PathVariable Long contestId,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        contestService.finalizeContest(contestId, user.getUser().getId());
        return ApiResponse.ok("대회 결과가 확정되었습니다.").toResponse();
    }
}
