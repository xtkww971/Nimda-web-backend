package judgeServer.domain.contest.repository;

import judgeServer.domain.contest.entity.ContestChallengeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContestChallengeResultRepository extends JpaRepository<ContestChallengeResult, Long> {

    interface ContestCount {
        Long getContestId();
        Long getCount();
    }

    List<ContestChallengeResult> findByContestIdOrderByMaxPointAscCodeAsc(Long contestId);

    @Query("""
            select c.contestId as contestId, count(c) as count from ContestChallengeResult c
            where c.contestId in :contestIds group by c.contestId
            """)
    List<ContestCount> countByContestIds(@Param("contestIds") Collection<Long> contestIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ContestChallengeResult c where c.contestId = :contestId")
    void deleteByContestId(@Param("contestId") Long contestId);
}
