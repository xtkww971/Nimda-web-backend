package judgeServer.domain.schedule.service;

import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.schedule.dto.AdminScheduleResponse;
import judgeServer.domain.schedule.dto.ScheduleResponse;
import judgeServer.domain.schedule.dto.ScheduleUpdateRequest;
import judgeServer.domain.schedule.entity.ContestSchedule;
import judgeServer.domain.schedule.entity.ContestScheduleHistory;
import judgeServer.domain.schedule.enums.ContestPhase;
import judgeServer.domain.schedule.repository.ContestScheduleHistoryRepository;
import judgeServer.domain.schedule.repository.ContestScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * CTF 대회 일정. 시각 판정은 모두 서버 시계(KST)로 한다.
 *
 * <p>일정이 등록되지 않았으면 {@link ContestPhase#NOT_SCHEDULED}이고, 상시 워게임처럼 제출·조회를 막지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestScheduleService {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final int HISTORY_LIMIT = 20;

    static final boolean DEFAULT_CLEANUP_INSTANCES = true;
    static final boolean DEFAULT_CHALLENGES_VISIBLE = true;
    static final boolean DEFAULT_FREEZE_SCOREBOARD = false;
    static final int DEFAULT_FREEZE_MINUTES = 30;

    private static final DateTimeFormatter HISTORY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContestScheduleRepository scheduleRepository;
    private final ContestScheduleHistoryRepository historyRepository;

    /** 테스트에서 시각을 고정할 수 있게 열어둔다. */
    private Clock clock = Clock.system(KST);

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public Optional<ContestSchedule> current() {
        return scheduleRepository.findById(ContestSchedule.SINGLETON_ID);
    }

    /** 주어진 시각(KST)의 대회 상태. 제출 마감 판정은 채점 시각이 아니라 제출 시각으로 한다. */
    public ContestPhase phaseAt(LocalDateTime time) {
        return current().map(s -> s.phaseAt(time)).orElse(ContestPhase.NOT_SCHEDULED);
    }

    public ScheduleResponse getSchedule() {
        LocalDateTime now = now();
        ContestSchedule schedule = current().orElse(null);
        if (schedule == null) {
            return new ScheduleResponse(null, null, ContestPhase.NOT_SCHEDULED, toKst(now));
        }
        return new ScheduleResponse(toKst(schedule.getStartsAt()), toKst(schedule.getEndsAt()),
                schedule.phaseAt(now), toKst(now));
    }

    public AdminScheduleResponse getAdminSchedule() {
        LocalDateTime now = now();
        ContestSchedule s = current().orElse(null);
        var history = historyRepository.findRecent(PageRequest.of(0, HISTORY_LIMIT));
        if (s == null) {
            return new AdminScheduleResponse(null, null, ContestPhase.NOT_SCHEDULED, toKst(now), true,
                    null, null, null, null, history);
        }
        return new AdminScheduleResponse(toKst(s.getStartsAt()), toKst(s.getEndsAt()), s.phaseAt(now), toKst(now),
                true, s.getCleanupInstancesOnEnd(), s.getChallengesVisibleAfterEnd(), s.getFreezeScoreboard(),
                s.getFreezeMinutesBeforeEnd(), history);
    }

    /** 일정·설정을 저장하고 바뀐 항목마다 이력을 남긴다. 종료 시각을 과거로 바꾸면 저장 즉시 대회가 끝난다. */
    @Transactional
    public AdminScheduleResponse update(ScheduleUpdateRequest request, Long adminUserId) {
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
        }
        LocalDateTime now = now();
        ContestSchedule existing = scheduleRepository.findByIdForUpdate(ContestSchedule.SINGLETON_ID).orElse(null);

        boolean cleanup = pick(request.getCleanupInstancesOnEnd(),
                existing == null ? DEFAULT_CLEANUP_INSTANCES : existing.getCleanupInstancesOnEnd());
        boolean visible = pick(request.getChallengesVisibleAfterEnd(),
                existing == null ? DEFAULT_CHALLENGES_VISIBLE : existing.getChallengesVisibleAfterEnd());
        boolean freeze = pick(request.getFreezeScoreboard(),
                existing == null ? DEFAULT_FREEZE_SCOREBOARD : existing.getFreezeScoreboard());
        int freezeMinutes = request.getFreezeMinutesBeforeEnd() != null ? request.getFreezeMinutesBeforeEnd()
                : existing == null ? DEFAULT_FREEZE_MINUTES : existing.getFreezeMinutesBeforeEnd();

        List<ContestScheduleHistory> history = new ArrayList<>();
        if (existing == null) {
            history.add(entry("CREATED", null,
                    format(request.getStartsAt()) + " ~ " + format(request.getEndsAt()), adminUserId, now));
            scheduleRepository.save(ContestSchedule.builder()
                    .id(ContestSchedule.SINGLETON_ID)
                    .startsAt(request.getStartsAt())
                    .endsAt(request.getEndsAt())
                    .cleanupInstancesOnEnd(cleanup)
                    .challengesVisibleAfterEnd(visible)
                    .freezeScoreboard(freeze)
                    .freezeMinutesBeforeEnd(freezeMinutes)
                    .updatedAt(now)
                    .updatedBy(adminUserId)
                    .build());
        } else {
            diff(history, "STARTS_AT", format(existing.getStartsAt()), format(request.getStartsAt()), adminUserId, now);
            diff(history, "ENDS_AT", format(existing.getEndsAt()), format(request.getEndsAt()), adminUserId, now);
            diff(history, "CLEANUP_INSTANCES", onOff(existing.getCleanupInstancesOnEnd()), onOff(cleanup), adminUserId, now);
            diff(history, "CHALLENGES_VISIBLE", onOff(existing.getChallengesVisibleAfterEnd()), onOff(visible), adminUserId, now);
            diff(history, "FREEZE_SCOREBOARD", onOff(existing.getFreezeScoreboard()), onOff(freeze), adminUserId, now);
            diff(history, "FREEZE_MINUTES", String.valueOf(existing.getFreezeMinutesBeforeEnd()),
                    String.valueOf(freezeMinutes), adminUserId, now);
            existing.update(request.getStartsAt(), request.getEndsAt(), cleanup, visible, freeze, freezeMinutes,
                    now, adminUserId);
        }
        historyRepository.saveAll(history);
        historyRepository.flush();
        scheduleRepository.flush();
        return getAdminSchedule();
    }

    /**
     * 종료 후 "문제 목록 계속 공개"를 껐으면 종료 뒤 참가자의 문제 조회·다운로드·인스턴스를 막는다 (관리자·DEV 제외).
     * 시작 전 문제 비공개는 정책이 정해지지 않아 막지 않는다.
     */
    public void requireChallengesAccessible(CustomUserDetails user) {
        if (CtfTeamResolver.isStaff(user)) {
            return;
        }
        ContestSchedule schedule = current().orElse(null);
        if (schedule != null && schedule.phaseAt(now()) == ContestPhase.ENDED
                && !Boolean.TRUE.equals(schedule.getChallengesVisibleAfterEnd())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "대회가 종료되었습니다.");
        }
    }

    public static OffsetDateTime toKst(LocalDateTime time) {
        return time == null ? null : time.atZone(KST).toOffsetDateTime();
    }

    private static boolean pick(Boolean requested, boolean fallback) {
        return requested != null ? requested : fallback;
    }

    private static String format(LocalDateTime time) {
        return time.format(HISTORY_FORMAT);
    }

    private static String onOff(Boolean value) {
        return Boolean.TRUE.equals(value) ? "ON" : "OFF";
    }

    private static void diff(List<ContestScheduleHistory> history, String field, String before, String after,
                             Long adminUserId, LocalDateTime now) {
        if (!Objects.equals(before, after)) {
            history.add(entry(field, before, after, adminUserId, now));
        }
    }

    private static ContestScheduleHistory entry(String field, String before, String after, Long adminUserId,
                                                LocalDateTime now) {
        return ContestScheduleHistory.builder()
                .field(field)
                .oldValue(before)
                .newValue(after)
                .changedBy(adminUserId)
                .changedAt(now)
                .build();
    }
}
