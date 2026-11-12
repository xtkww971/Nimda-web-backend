package judgeServer.domain.contest.entity;

import jakarta.persistence.*;
import lombok.*;

/** 대회 종료 시점의 팀 멤버와 멤버별 기여 스냅샷. */
@Entity
@Table(name = "ctf_contest_result_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestResultMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 종료 시점 닉네임. 이후 닉네임을 바꿔도 이력은 그대로다. */
    @Column(nullable = false, length = 20)
    private String nickname;

    /** LEADER / MEMBER */
    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "solve_count", nullable = false)
    private Integer solveCount;

    @Column(nullable = false)
    private Long points;
}
