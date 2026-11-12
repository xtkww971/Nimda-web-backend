package judgeServer.domain.schedule.controller;

import com.nimda.cite.common.response.ApiResponse;
import judgeServer.domain.schedule.service.ContestScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 참가자용 대회 일정 (로그인 필요). 팀이 없어도 볼 수 있다.
 *
 * <pre>
 *   GET /api/ctf/schedule -> {startsAt, endsAt, status, serverTime}
 * </pre>
 *
 * <p>status: NOT_SCHEDULED / UPCOMING / RUNNING / ENDED. 남은 시간은 serverTime 기준으로 계산한다.
 */
@RestController
@RequestMapping("/api/ctf/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ContestScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<?> schedule() {
        return ApiResponse.ok(scheduleService.getSchedule()).toResponse();
    }
}
