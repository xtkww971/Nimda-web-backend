package judgeServer.domain.solve.repository;

import judgeServer.domain.solve.entity.Solve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolveRepository extends JpaRepository<Solve, Long> {

    interface SolveCount {
        Long getChallengeId();
        Long getSolves();
    }

    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);

    long countByChallengeId(Long challengeId);

    void deleteByChallengeId(Long challengeId);

    @Query("select s.challengeId as challengeId, count(s) as solves from Solve s group by s.challengeId")
    List<SolveCount> countGroupByChallengeId();
}
