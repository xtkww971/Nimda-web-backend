package judgeServer.domain.contest.entity;

import com.nimda.cite.common.util.TableNames;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 대회 일정 변경 이력 (운영 설정 화면의 CHANGE LOG).
 *
 * <p>field: CREATED, NAME, STARTS_AT, ENDS_AT, CLEANUP_INSTANCES, CHALLENGES_VISIBLE, FREEZE_SCOREBOARD,
 * FREEZE_MINUTES, REOPENED, FINALIZED
 */
@Entity
@Table(name = TableNames.ctfContestChangeLogsTable)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(nullable = false, length = 20)
    private String field;

    @Column(name = "old_value", length = 100)
    private String oldValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
