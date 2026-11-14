package judgeServer.domain.contest.entity;

import com.nimda.cite.common.util.TableNames;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 대회 종료 시점의 문제별 결과 스냅샷 (최대 → 종료 시점 배점, 해결 팀 수, First Blood). */
@Entity
@Table(name = TableNames.ctfContestChallengesTable)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestChallengeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "max_point", nullable = false)
    private Integer maxPoint;

    @Column(name = "final_point", nullable = false)
    private Integer finalPoint;

    @Column(name = "solved_teams", nullable = false)
    private Integer solvedTeams;

    @Column(name = "first_blood_team_id")
    private Long firstBloodTeamId;

    @Column(name = "first_blood_team_name", length = 50)
    private String firstBloodTeamName;

    @Column(name = "first_blood_at")
    private LocalDateTime firstBloodAt;
}
