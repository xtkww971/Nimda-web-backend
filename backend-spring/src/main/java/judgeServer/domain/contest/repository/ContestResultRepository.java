package judgeServer.domain.contest.repository;

import judgeServer.domain.contest.entity.ContestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContestResultRepository extends JpaRepository<ContestResult, Long> {

    /** 순위 순서 (점수 0인 팀은 뒤에, 순위 없음). */
    @Query("""
            select r from ContestResult r where r.contestId = :contestId
            order by case when r.teamRank is null then 1 else 0 end, r.teamRank asc, r.teamName asc
            """)
    List<ContestResult> findStandings(@Param("contestId") Long contestId);

    List<ContestResult> findByContestIdIn(Collection<Long> contestIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ContestResult r where r.contestId = :contestId")
    void deleteByContestId(@Param("contestId") Long contestId);
}
