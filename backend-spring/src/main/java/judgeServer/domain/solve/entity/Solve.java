package judgeServer.domain.solve.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 정답 기록. 같은 사람이 같은 문제를 두 번 맞히는 것은 DB 유니크 키로 막는다. */
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

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
