package judgeServer.domain.solve.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 정답 기록. 같은 팀이 같은 문제를 두 번 맞히는 것은 DB 유니크 키(challenge_id, team_id)로 막는다. */
@Entity
@Table(name = "solves")
@Builder
@Getter @NoArgsConstructor @AllArgsConstructor
public class Solve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    /** 맞힌 사람. 팀 안에서의 기여도를 보여줄 때 쓴다. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 점수를 받는 팀(groups.id). 팀전 이전 기록은 NULL이다. */
    @Column(name = "team_id")
    private Long teamId;

    /** 정답을 낸 대회(ctf_contests.id). 배점 감소는 대회 안의 해결 수로 계산한다. 대회가 없을 때의 기록은 NULL이다. */
    @Column(name = "contest_id")
    private Long contestId;

    /** 맞힌 시점의 점수. 이후 해결자가 늘어도 변하지 않는다. */
    @Column(name = "awarded_points", nullable = false)
    private Integer awardedPoints;

    @Column(name = "solved_at", nullable = false)
    private LocalDateTime solvedAt;

    @PrePersist
    public void prePersist() {
        this.solvedAt = LocalDateTime.now();
    }
}
