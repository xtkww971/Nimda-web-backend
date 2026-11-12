package judgeServer.domain.contest.repository;

import judgeServer.domain.contest.entity.ContestResultMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContestResultMemberRepository extends JpaRepository<ContestResultMember, Long> {

    Optional<ContestResultMember> findByContestIdAndUserId(Long contestId, Long userId);

    List<ContestResultMember> findByContestIdAndTeamId(Long contestId, Long teamId);

    /** 유저가 참가한 대회들. "참가한 대회" 필터와 목록의 내 결과에 쓴다. */
    List<ContestResultMember> findByUserIdAndContestIdIn(Long userId, Collection<Long> contestIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ContestResultMember m where m.contestId = :contestId")
    void deleteByContestId(@Param("contestId") Long contestId);
}
