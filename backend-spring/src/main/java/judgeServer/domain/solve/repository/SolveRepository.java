package judgeServer.domain.solve.repository;

import judgeServer.domain.scoreboard.dto.RecentSolveResponse;
import judgeServer.domain.solve.entity.Solve;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolveRepository extends JpaRepository<Solve, Long> {

    interface SolveCount {
        Long getChallengeId();
        Long getSolves();
    }

    /** 팀 안에서 멤버 한 명이 기여한 해결 수와 점수. */
    interface MemberContribution {
        Long getUserId();
        Long getSolveCount();
        Long getPoints();
    }

    boolean existsByChallengeIdAndTeamId(Long challengeId, Long teamId);

    /** 대회 안에서 이 문제를 푼 팀 수. 배점 감소의 기준이다. */
    @Query("select count(s) from Solve s where s.challengeId = :challengeId and (s.contestId = :contestId or (:contestId is null and s.contestId is null))")
    long countInContest(@Param("challengeId") Long challengeId, @Param("contestId") Long contestId);

    void deleteByChallengeId(Long challengeId);

    @Query("select s.challengeId as challengeId, count(s) as solves from Solve s where (s.contestId = :contestId or (:contestId is null and s.contestId is null)) group by s.challengeId")
    List<SolveCount> countGroupByChallengeIdInContest(@Param("contestId") Long contestId);

    /** 대회 결과 스냅샷용. 해결 시각 오름차순이라 문제별 첫 기록이 First Blood다. */
    List<Solve> findByContestIdOrderBySolvedAtAscIdAsc(Long contestId);

    /** 첫 대회를 만들 때, 대회 없이 쌓인 팀 정답 기록을 그 대회에 편입한다. */
    @Modifying
    @Query("update Solve s set s.contestId = :contestId where s.contestId is null and s.teamId is not null")
    int assignOrphansToContest(@Param("contestId") Long contestId);

    /** 문제를 지울 때 점수를 다시 계산해야 하는 팀들. */
    @Query("select distinct s.teamId from Solve s where s.challengeId = :challengeId and s.teamId is not null")
    List<Long> findTeamIdsByChallengeId(@Param("challengeId") Long challengeId);

    @Query("""
            select s.userId as userId, count(s) as solveCount, sum(s.awardedPoints) as points
            from Solve s where s.teamId = :teamId group by s.userId
            """)
    List<MemberContribution> findContributionsByTeamId(@Param("teamId") Long teamId);

    /** 팀의 최근 해결. 개수는 Pageable로 자른다. */
    @Query("""
            select new judgeServer.domain.scoreboard.dto.RecentSolveResponse(
                s.solvedAt, u.nickname, c.code, c.title, s.awardedPoints)
            from Solve s
                join User u on u.id = s.userId
                join Challenge c on c.id = s.challengeId
            where s.teamId = :teamId
            order by s.solvedAt desc
            """)
    List<RecentSolveResponse> findRecentByTeamId(@Param("teamId") Long teamId, Pageable pageable);
}
