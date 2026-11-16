package judgeServer.domain.contest.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import judgeServer.domain.contest.dto.ContestSaveRequest;
import judgeServer.domain.contest.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 운영자 대회 운영 설정 (ADMIN 전용). 현재 대회를 다룬다.
 *
 * <pre>
 *   GET /api/admin/ctf/schedule -> 현재 대회 일정, 상태, serverTime, 확정 여부, 종료 후 동작 설정, 변경 이력(최근 20개)
 *   PUT /api/admin/ctf/schedule -> 저장. 대회가 없으면 새로 만든다.
 *                                  {name?, startsAt, endsAt, cleanupInstancesOnEnd, challengesVisibleAfterEnd,
 *                                   freezeScoreboard, freezeMinutesBeforeEnd}
 * </pre>
 *
 * <p>"지금 종료"·"+30분" 같은 빠른 조정은 프론트가 endsAt을 계산해 PUT으로 보낸다.
 */
@RestController
@RequestMapping(UrlNames.adminScheduleUrl)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ContestService contestService;

    @GetMapping(UrlNames.getAdminScheduleUrl)
    public ResponseEntity<?> schedule() {
        return ApiResponse.ok(contestService.getAdminSchedule()).toResponse();
    }

    @PutMapping(UrlNames.updateScheduleUrl)
    public ResponseEntity<?> update(@Valid @RequestBody ContestSaveRequest request,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok("대회 일정이 저장되었습니다.",
                contestService.updateCurrent(request, user.getUser().getId())).toResponse();
    }
}
