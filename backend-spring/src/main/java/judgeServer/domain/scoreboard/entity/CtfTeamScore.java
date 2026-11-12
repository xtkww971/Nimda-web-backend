package judgeServer.domain.scoreboard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 팀별 CTF 점수 집계. 원본은 {@code solves.awarded_points}이고 이 테이블은 순위 조회용 집계본이다.
 *
 * <p>행은 운영자가 팀을 만들 때 생긴다. 값은 엔티티 수정이 아니라
 * {@link judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository}의 원자적 쿼리로만 바꾼다.
 * 같은 팀의 정답이 여러 대에서 동시에 처리돼도 점수가 덮어써지지 않게 하기 위함이다.
 */
@Entity
@Table(name = "ctf_team_scores")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CtfTeamScore {

    @Id
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "total_score", nullable = false)
    private Long totalScore;

    @Column(name = "solve_count", nullable = false)
    private Integer solveCount;

    /** 마지막으로 점수를 얻은 시각. 동점이면 이른 팀이 위다. */
    @Column(name = "last_solved_at")
    private LocalDateTime lastSolvedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
