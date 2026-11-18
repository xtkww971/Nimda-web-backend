package com.nimda.ctfgame.repository;

import com.nimda.ctfgame.entity.GameItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameItemRepository extends JpaRepository<GameItem, Long> {

    List<GameItem> findByVisibleTrueOrderBySortOrderAscIdAsc();

    List<GameItem> findAllByOrderBySortOrderAscIdAsc();

    Optional<GameItem> findByItemKey(String itemKey);

    boolean existsByItemKey(String itemKey);

    // ── 소유 계산용: 정답 기록(solves, judgeServer 의 Solve 엔티티)을 읽기만 한다 ──

    /** 이 팀이 현재 대회에서 푼 문제들 */
    @Query("select distinct s.challengeId from Solve s where s.teamId = :teamId"
            + " and (s.contestId = :contestId or (:contestId is null and s.contestId is null))")
    List<Long> findSolvedChallengeIdsByTeam(@Param("teamId") Long teamId, @Param("contestId") Long contestId);

    /** 팀이 없을 때: 본인이 현재 대회에서 푼 문제들 */
    @Query("select distinct s.challengeId from Solve s where s.userId = :userId"
            + " and (s.contestId = :contestId or (:contestId is null and s.contestId is null))")
    List<Long> findSolvedChallengeIdsByUser(@Param("userId") Long userId, @Param("contestId") Long contestId);
}
