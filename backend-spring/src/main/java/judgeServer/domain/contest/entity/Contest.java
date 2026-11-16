package judgeServer.domain.contest.entity;

import com.nimda.cite.common.util.TableNames;
import jakarta.persistence.*;
import judgeServer.domain.contest.enums.ContestPhase;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CTF 대회. 일정·종료 후 동작 설정과 최종 결과 확정 여부를 가진다. 시각은 서버 시간(KST) 기준이다.
 * 가장 최근에 시작하는 대회가 "현재 대회"이고, 팀·정답 기록은 현재 대회에 묶인다.
 */
@Entity
@Table(name = TableNames.ctfContestsTable)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

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

    /** 최종 결과 스냅샷을 만든 시각. null이면 아직 이력에 오르지 않았다. */
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    public boolean isFinalized() {
        return finalizedAt != null;
    }

    public void update(String name, LocalDateTime startsAt, LocalDateTime endsAt, boolean cleanupInstancesOnEnd,
                       boolean challengesVisibleAfterEnd, boolean freezeScoreboard, int freezeMinutesBeforeEnd,
                       LocalDateTime updatedAt, Long updatedBy) {
        this.name = name;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.cleanupInstancesOnEnd = cleanupInstancesOnEnd;
        this.challengesVisibleAfterEnd = challengesVisibleAfterEnd;
        this.freezeScoreboard = freezeScoreboard;
        this.freezeMinutesBeforeEnd = freezeMinutesBeforeEnd;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public void markFinalized(LocalDateTime at) {
        this.finalizedAt = at;
    }

    /** 종료 시각을 미래로 바꿔 다시 열 때. 스냅샷은 호출하는 쪽에서 지운다. */
    public void reopen() {
        this.finalizedAt = null;
    }
}
