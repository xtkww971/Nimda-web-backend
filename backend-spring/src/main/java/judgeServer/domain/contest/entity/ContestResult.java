package judgeServer.domain.contest.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 대회 종료 시점의 팀 최종 순위 스냅샷. 점수가 0인 팀은 teamRank가 null이다. */
@Entity
@Table(name = "ctf_contest_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    /** 원본 groups.id. 원본이 지워져도 남는다. */
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "team_rank")
    private Integer teamRank;

    @Column(name = "team_name", nullable = false, length = 50)
    private String teamName;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "solve_count", nullable = false)
    private Integer solveCount;

    @Column(name = "total_score", nullable = false)
    private Long totalScore;

    @Column(name = "last_solved_at")
    private LocalDateTime lastSolvedAt;
}
