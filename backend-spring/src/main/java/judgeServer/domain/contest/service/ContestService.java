package judgeServer.domain.contest.service;

import com.nimda.cite.common.exception.GlobalNimdaException;
import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.contest.dto.AdminContestResponse;
import judgeServer.domain.contest.dto.AdminScheduleResponse;
import judgeServer.domain.contest.dto.ContestSaveRequest;
import judgeServer.domain.contest.dto.ScheduleResponse;
import judgeServer.domain.contest.entity.Contest;
import judgeServer.domain.contest.entity.ContestChangeLog;
import judgeServer.domain.contest.enums.ContestPhase;
import judgeServer.domain.contest.repository.ContestChangeLogRepository;
import judgeServer.domain.contest.repository.ContestRepository;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * CTF 대회와 일정. 시각 판정은 모두 서버 시계(KST)로 한다.
 *
 * <ul>
 *   <li>현재 대회 = 가장 최근에 시작하는 대회. 팀·정답 기록은 현재 대회에 묶인다.</li>
 *   <li>대회가 하나도 없으면 {@link ContestPhase#NOT_SCHEDULED}이고, 상시 워게임처럼 제출·조회를 막지 않는다.</li>
 *   <li>종료 후 {@link #FINALIZE_GRACE} 지나면 최종 결과를 스냅샷으로 확정해 이력에 올린다 (운영자가 바로 확정할 수도 있다).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestService {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final int HISTORY_LIMIT = 20;

    /** 종료 직전 제출이 큐에서 채점될 시간을 기다린 뒤 확정한다. */
    public static final Duration FINALIZE_GRACE = Duration.ofMinutes(1);

    static final boolean DEFAULT_CLEANUP_INSTANCES = true;
    static final boolean DEFAULT_CHALLENGES_VISIBLE = true;
    static final boolean DEFAULT_FREEZE_SCOREBOARD = false;
    static final int DEFAULT_FREEZE_MINUTES = 30;

    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContestRepository contestRepository;
    private final ContestChangeLogRepository changeLogRepository;
    private final ContestSnapshotService snapshotService;
    private final GroupRepository groupRepository;
    private final SolveRepository solveRepository;

    /** 테스트에서 시각을 고정할 수 있게 열어둔다. */
    private Clock clock = Clock.system(KST);

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    // ---------------------------------------------------------------- 현재 대회

    public Optional<Contest> current() {
        return contestRepository.findFirstByOrderByStartsAtDescIdDesc();
    }

    public Optional<Long> currentContestId() {
        return current().map(Contest::getId);
    }

    /** 주어진 시각(KST)의 현재 대회 상태. 제출 마감 판정은 채점 시각이 아니라 제출 시각으로 한다. */
    public ContestPhase phaseAt(LocalDateTime time) {
        return current().map(c -> c.phaseAt(time)).orElse(ContestPhase.NOT_SCHEDULED);
    }

    public ScheduleResponse getSchedule() {
        LocalDateTime now = now();
        Contest c = current().orElse(null);
        if (c == null) {
            return new ScheduleResponse(null, null, null, null, ContestPhase.NOT_SCHEDULED, toKst(now));
        }
        return new ScheduleResponse(c.getId(), c.getName(), toKst(c.getStartsAt()), toKst(c.getEndsAt()),
                c.phaseAt(now), toKst(now));
    }

    public AdminScheduleResponse getAdminSchedule() {
        LocalDateTime now = now();
        Contest c = current().orElse(null);
        if (c == null) {
            return new AdminScheduleResponse(null, null, null, null, ContestPhase.NOT_SCHEDULED, toKst(now), null,
                    true, null, null, null, null, List.of());
        }
        return new AdminScheduleResponse(c.getId(), c.getName(), toKst(c.getStartsAt()), toKst(c.getEndsAt()),
                c.phaseAt(now), toKst(now), toKst(c.getFinalizedAt()), true, c.getCleanupInstancesOnEnd(),
                c.getChallengesVisibleAfterEnd(), c.getFreezeScoreboard(), c.getFreezeMinutesBeforeEnd(),
                changeLogRepository.findRecent(c.getId(), PageRequest.of(0, HISTORY_LIMIT)));
    }

    public List<AdminContestResponse> listForAdmin() {
        LocalDateTime now = now();
        Long currentId = currentContestId().orElse(null);
        return contestRepository.findAllByOrderByStartsAtDescIdDesc().stream()
                .map(c -> new AdminContestResponse(c.getId(), c.getName(), toKst(c.getStartsAt()),
                        toKst(c.getEndsAt()), c.phaseAt(now), toKst(c.getFinalizedAt()), c.getId().equals(currentId)))
                .toList();
    }

    // ---------------------------------------------------------------- 운영자 변경

    /**
     * 새 대회를 만든다. 이전 대회의 결과가 확정된 뒤에만 만들 수 있다 (진행 중인 대회의 팀·기록이 섞이지 않게).
     * 대회가 처음 만들어지면, 그 전에 만든 팀과 그 팀의 정답 기록을 이 대회에 편입한다.
     */
    @Transactional
    public AdminScheduleResponse create(ContestSaveRequest request, Long adminUserId) {
        validateRange(request);
        Contest latest = current().orElse(null);
        if (latest != null && !latest.isFinalized()) {
            throw new GlobalNimdaException(ErrorCode.UNFINALIZED_CONTEST_EXISTS,
                    "결과가 확정되지 않은 대회(" + latest.getName() + ")가 있습니다. 종료 후 결과가 확정되면 새 대회를 만들 수 있습니다.");
        }
        if (!request.getStartsAt().isAfter(latest == null ? LocalDateTime.MIN : latest.getStartsAt())) {
            throw new GlobalNimdaException(ErrorCode.CONTEST_STARTS_BEFORE_PREVIOUS);
        }

        LocalDateTime now = now();
        Contest contest = contestRepository.save(Contest.builder()
                .name(nameOrDefault(request.getName(), null, request.getStartsAt()))
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .cleanupInstancesOnEnd(pick(request.getCleanupInstancesOnEnd(), DEFAULT_CLEANUP_INSTANCES))
                .challengesVisibleAfterEnd(pick(request.getChallengesVisibleAfterEnd(), DEFAULT_CHALLENGES_VISIBLE))
                .freezeScoreboard(pick(request.getFreezeScoreboard(), DEFAULT_FREEZE_SCOREBOARD))
                .freezeMinutesBeforeEnd(request.getFreezeMinutesBeforeEnd() != null
                        ? request.getFreezeMinutesBeforeEnd() : DEFAULT_FREEZE_MINUTES)
                .createdAt(now)
                .updatedAt(now)
                .updatedBy(adminUserId)
                .build());

        if (latest == null) {
            groupRepository.assignOrphansToContest(contest.getId());
            solveRepository.assignOrphansToContest(contest.getId());
        }
        changeLogRepository.save(log(contest.getId(), "CREATED", null,
                format(contest.getStartsAt()) + " ~ " + format(contest.getEndsAt()), adminUserId, now));
        changeLogRepository.flush();
        return getAdminSchedule();
    }

    /**
     * 현재 대회의 일정·설정을 저장하고 바뀐 항목마다 이력을 남긴다. 대회가 없으면 새로 만든다.
     * 종료 시각을 과거로 바꾸면 저장 즉시 대회가 끝나고, 확정된 대회의 종료 시각을 미래로 바꾸면 다시 열린다.
     */
    @Transactional
    public AdminScheduleResponse updateCurrent(ContestSaveRequest request, Long adminUserId) {
        Long currentId = currentContestId().orElse(null);
        if (currentId == null) {
            return create(request, adminUserId);
        }
        validateRange(request);
        Contest c = contestRepository.findByIdForUpdate(currentId).orElseThrow();
        LocalDateTime now = now();

        String name = nameOrDefault(request.getName(), c.getName(), request.getStartsAt());
        boolean cleanup = pick(request.getCleanupInstancesOnEnd(), c.getCleanupInstancesOnEnd());
        boolean visible = pick(request.getChallengesVisibleAfterEnd(), c.getChallengesVisibleAfterEnd());
        boolean freeze = pick(request.getFreezeScoreboard(), c.getFreezeScoreboard());
        int freezeMinutes = request.getFreezeMinutesBeforeEnd() != null
                ? request.getFreezeMinutesBeforeEnd() : c.getFreezeMinutesBeforeEnd();

        List<ContestChangeLog> logs = new ArrayList<>();
        diff(logs, c.getId(), "NAME", c.getName(), name, adminUserId, now);
        diff(logs, c.getId(), "STARTS_AT", format(c.getStartsAt()), format(request.getStartsAt()), adminUserId, now);
        diff(logs, c.getId(), "ENDS_AT", format(c.getEndsAt()), format(request.getEndsAt()), adminUserId, now);
        diff(logs, c.getId(), "CLEANUP_INSTANCES", onOff(c.getCleanupInstancesOnEnd()), onOff(cleanup), adminUserId, now);
        diff(logs, c.getId(), "CHALLENGES_VISIBLE", onOff(c.getChallengesVisibleAfterEnd()), onOff(visible), adminUserId, now);
        diff(logs, c.getId(), "FREEZE_SCOREBOARD", onOff(c.getFreezeScoreboard()), onOff(freeze), adminUserId, now);
        diff(logs, c.getId(), "FREEZE_MINUTES", String.valueOf(c.getFreezeMinutesBeforeEnd()),
                String.valueOf(freezeMinutes), adminUserId, now);

        c.update(name, request.getStartsAt(), request.getEndsAt(), cleanup, visible, freeze, freezeMinutes,
                now, adminUserId);

        // 확정된 대회를 다시 열면 이력에서 내리고 스냅샷을 지운다. 다시 끝나면 새로 확정된다.
        if (c.isFinalized() && c.phaseAt(now) != ContestPhase.ENDED) {
            snapshotService.clear(c.getId());
            c.reopen();
            logs.add(log(c.getId(), "REOPENED", null, null, adminUserId, now));
        }
        changeLogRepository.saveAll(logs);
        changeLogRepository.flush();
        contestRepository.flush();
        return getAdminSchedule();
    }

    /** 운영자가 결과를 바로 확정한다. 이미 확정됐으면 지금 상태로 다시 만든다. */
    @Transactional
    public void finalizeContest(Long contestId, Long adminUserId) {
        Contest c = contestRepository.findByIdForUpdate(contestId)
                .orElseThrow(() -> new GlobalNimdaException(ErrorCode.CONTEST_NOT_FOUND));
        LocalDateTime now = now();
        if (c.phaseAt(now) != ContestPhase.ENDED) {
            throw new GlobalNimdaException(ErrorCode.CONTEST_NOT_ENDED);
        }
        snapshot(c, adminUserId, now);
    }

    /**
     * 종료 후 유예 시간이 지난 현재 대회를 확정한다. {@link ContestFinalizeScheduler}가 주기적으로 부른다.
     * 백엔드가 여러 대여도 행 잠금과 finalized_at 확인으로 한 번만 확정된다.
     *
     * @return 이번 호출로 확정했으면 true
     */
    @Transactional
    public boolean finalizeIfDue() {
        Long currentId = currentContestId().orElse(null);
        if (currentId == null) {
            return false;
        }
        Contest c = contestRepository.findByIdForUpdate(currentId).orElse(null);
        LocalDateTime now = now();
        if (c == null || c.isFinalized() || now.isBefore(c.getEndsAt().plus(FINALIZE_GRACE))) {
            return false;
        }
        snapshot(c, null, now);
        log.info("대회 결과 확정: id={}, name={}", c.getId(), c.getName());
        return true;
    }

    private void snapshot(Contest c, Long adminUserId, LocalDateTime now) {
        snapshotService.write(c.getId());
        c.markFinalized(now);
        changeLogRepository.save(log(c.getId(), "FINALIZED", null, format(now), adminUserId, now));
    }

    // ---------------------------------------------------------------- 접근 제어

    /**
     * 종료 후 "문제 목록 계속 공개"를 껐으면 종료 뒤 참가자의 문제 조회·다운로드·인스턴스를 막는다 (관리자·DEV 제외).
     * 시작 전 문제 비공개는 정책이 정해지지 않아 막지 않는다.
     */
    public void requireChallengesAccessible(CustomUserDetails user) {
        if (CtfTeamResolver.isStaff(user)) {
            return;
        }
        Contest c = current().orElse(null);
        if (c != null && c.phaseAt(now()) == ContestPhase.ENDED
                && !Boolean.TRUE.equals(c.getChallengesVisibleAfterEnd())) {
            throw new GlobalNimdaException(ErrorCode.CONTEST_ENDED);
        }
    }

    // ---------------------------------------------------------------- helpers

    public static OffsetDateTime toKst(LocalDateTime time) {
        return time == null ? null : time.atZone(KST).toOffsetDateTime();
    }

    private static void validateRange(ContestSaveRequest request) {
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new GlobalNimdaException(ErrorCode.INVALID_CONTEST_RANGE);
        }
    }

    private static String nameOrDefault(String requested, String existing, LocalDateTime startsAt) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        return existing != null ? existing : "NIMDACON " + startsAt.getYear();
    }

    private static boolean pick(Boolean requested, boolean fallback) {
        return requested != null ? requested : fallback;
    }

    private static String format(LocalDateTime time) {
        return time.format(LOG_FORMAT);
    }

    private static String onOff(Boolean value) {
        return Boolean.TRUE.equals(value) ? "ON" : "OFF";
    }

    private static void diff(List<ContestChangeLog> logs, Long contestId, String field, String before, String after,
                             Long adminUserId, LocalDateTime now) {
        if (!Objects.equals(before, after)) {
            logs.add(log(contestId, field, before, after, adminUserId, now));
        }
    }

    private static ContestChangeLog log(Long contestId, String field, String before, String after, Long adminUserId,
                                        LocalDateTime now) {
        return ContestChangeLog.builder()
                .contestId(contestId)
                .field(field)
                .oldValue(before)
                .newValue(after)
                .changedBy(adminUserId)
                .changedAt(now)
                .build();
    }
}
