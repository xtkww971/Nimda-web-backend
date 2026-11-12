package judgeServer.domain.scoreboard.repository;

import judgeServer.domain.scoreboard.dto.TeamScoreboardRow;
import judgeServer.domain.scoreboard.entity.CtfTeamScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 현재 대회의 팀만 순위에 오른다. 순위 정렬 기준: 점수 내림차순 → 마지막 득점 시각 오름차순(먼저 도달한 팀 우선) → team_id 오름차순.
 * 목록 조회와 내 팀 순위 계산이 같은 기준을 써야 순위가 어긋나지 않는다.
 */
@Repository
public interface CtfTeamScoreRepository extends JpaRepository<CtfTeamScore, Long> {

    /** 팀을 만들 때 0점 행을 만든다. 이미 있으면 그대로 둔다. */
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO ctf_team_scores (team_id, total_score, solve_count, last_solved_at, updated_at)
            VALUES (:teamId, 0, 0, NULL, NOW(6))
            """, nativeQuery = true)
    void initialize(@Param("teamId") Long teamId);

    /**
     * 정답 한 건을 팀 점수에 더한다. 행이 없으면(V36 이전 데이터 등) 만든다.
     * 값을 읽고 쓰는 대신 한 문장으로 더하므로, 같은 팀의 정답이 동시에 처리돼도 점수가 빠지지 않는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO ctf_team_scores (team_id, total_score, solve_count, last_solved_at, updated_at)
            VALUES (:teamId, :points, 1, :solvedAt, :solvedAt)
            ON DUPLICATE KEY UPDATE
                total_score = total_score + :points,
                solve_count = solve_count + 1,
                last_solved_at = :solvedAt,
                updated_at = :solvedAt
            """, nativeQuery = true)
    void addSolve(@Param("teamId") Long teamId,
                  @Param("points") long points,
                  @Param("solvedAt") LocalDateTime solvedAt);

    /**
     * 주어진 팀들의 점수를 solves로부터 다시 계산한다. 문제 삭제처럼 solves가 지워질 때 쓴다.
     * 지워진 solves가 반영되도록 실행 전에 영속성 컨텍스트를 flush한다.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE ctf_team_scores ts SET
                total_score = (SELECT COALESCE(SUM(s.awarded_points), 0) FROM solves s WHERE s.team_id = ts.team_id),
                solve_count = (SELECT COUNT(*) FROM solves s WHERE s.team_id = ts.team_id),
                last_solved_at = (SELECT MAX(s.solved_at) FROM solves s WHERE s.team_id = ts.team_id),
                updated_at = NOW(6)
            WHERE ts.team_id IN (:teamIds)
            """, nativeQuery = true)
    void recalculate(@Param("teamIds") Collection<Long> teamIds);

    /** 점수가 있는 팀만 순위 순서대로. */
    @Query(value = """
            select new judgeServer.domain.scoreboard.dto.TeamScoreboardRow(
                ts.teamId, g.name,
                (select count(m) from GroupMember m where m.group.id = ts.teamId),
                ts.totalScore, ts.solveCount, ts.lastSolvedAt)
            from CtfTeamScore ts join Group g on g.id = ts.teamId
            where ts.totalScore > 0 and (g.contestId = :contestId or (:contestId is null and g.contestId is null))
            order by ts.totalScore desc, ts.lastSolvedAt asc, ts.teamId asc
            """,
            countQuery = "select count(ts) from CtfTeamScore ts join Group g on g.id = ts.teamId where ts.totalScore > 0 and (g.contestId = :contestId or (:contestId is null and g.contestId is null))")
    Page<TeamScoreboardRow> findRanking(@Param("contestId") Long contestId, Pageable pageable);

    /** 대회의 전체 팀 수 ("#4 / 12"의 12). */
    @Query("select count(ts) from CtfTeamScore ts join Group g on g.id = ts.teamId where (g.contestId = :contestId or (:contestId is null and g.contestId is null))")
    long countInContest(@Param("contestId") Long contestId);

    /** 같은 대회에서 우리 팀보다 위에 있는 팀 수. 순위는 이 값 + 1이다. */
    @Query("""
            select count(ts) from CtfTeamScore ts join Group g on g.id = ts.teamId
            where (g.contestId = :contestId or (:contestId is null and g.contestId is null))
              and (ts.totalScore > :score
                   or (ts.totalScore = :score and ts.lastSolvedAt < :lastSolvedAt)
                   or (ts.totalScore = :score and ts.lastSolvedAt = :lastSolvedAt and ts.teamId < :teamId))
            """)
    long countAhead(@Param("contestId") Long contestId,
                    @Param("teamId") Long teamId,
                    @Param("score") Long score,
                    @Param("lastSolvedAt") LocalDateTime lastSolvedAt);
}
