package judgeServer.domain.schedule.entity;

import jakarta.persistence.*;
import judgeServer.domain.schedule.enums.ContestPhase;
import lombok.*;

import java.time.LocalDateTime;

/** CTF 대회 일정. 대회가 하나뿐이라 id = 1 한 행만 쓴다. 시각은 서버 시간(KST) 기준이다. */
@Entity
@Table(name = "ctf_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestSchedule {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "cleanup_instances_on_end", nullable = false)
    private Boolean cleanupInstancesOnEnd;

    @Column(name = "challenges_visible_after_end", nullable = false)
    private Boolean challengesVisibleAfterEnd;

    @Column(name = "freeze_scoreboard", nullable = false)
    private Boolean freezeScoreboard;

    @Column(name = "freeze_minutes_before_end", nullable = false)
    private Integer freezeMinutesBeforeEnd;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    /** 시작 시각 이상, 종료 시각 미만이면 진행 중이다. */
    public ContestPhase phaseAt(LocalDateTime time) {
        if (time.isBefore(startsAt)) {
            return ContestPhase.UPCOMING;
        }
        if (time.isBefore(endsAt)) {
            return ContestPhase.RUNNING;
        }
        return ContestPhase.ENDED;
    }

    public void update(LocalDateTime startsAt, LocalDateTime endsAt, boolean cleanupInstancesOnEnd,
                       boolean challengesVisibleAfterEnd, boolean freezeScoreboard, int freezeMinutesBeforeEnd,
                       LocalDateTime updatedAt, Long updatedBy) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.cleanupInstancesOnEnd = cleanupInstancesOnEnd;
        this.challengesVisibleAfterEnd = challengesVisibleAfterEnd;
        this.freezeScoreboard = freezeScoreboard;
        this.freezeMinutesBeforeEnd = freezeMinutesBeforeEnd;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
}
