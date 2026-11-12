package judgeServer.domain.schedule.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import judgeServer.domain.schedule.dto.ScheduleUpdateRequest;
import judgeServer.domain.schedule.service.ContestScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 운영자 대회 운영 설정 (ADMIN 전용).
 *
 * <pre>
 *   GET /api/admin/ctf/schedule -> 일정, 상태, serverTime, 종료 후 동작 설정, 변경 이력(최근 20개)
 *   PUT /api/admin/ctf/schedule -> 저장. {startsAt, endsAt, cleanupInstancesOnEnd, challengesVisibleAfterEnd,
 *                                         freezeScoreboard, freezeMinutesBeforeEnd}
 * </pre>
 *
 * <p>"지금 종료"·"+30분" 같은 빠른 조정은 프론트가 endsAt을 계산해 PUT으로 보낸다.
 */
@RestController
@RequestMapping("/api/admin/ctf/schedule")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ContestScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<?> schedule() {
        return ApiResponse.ok(scheduleService.getAdminSchedule()).toResponse();
    }

    @PutMapping
    public ResponseEntity<?> update(@Valid @RequestBody ScheduleUpdateRequest request,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok("대회 일정이 저장되었습니다.",
                scheduleService.update(request, user.getUser().getId())).toResponse();
    }
}
