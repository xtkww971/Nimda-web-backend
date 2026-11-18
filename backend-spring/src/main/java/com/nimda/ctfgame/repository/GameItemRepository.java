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

    /** 한 문제에 연결된 물건 (연결 패널) */
    List<GameItem> findByChallengeIdOrderBySortOrderAscIdAsc(Long challengeId);

    /** 보이는 물건에 연결된 문제 id (judgeServer 의 문제 노출 규칙) */
    @Query("select distinct g.challengeId from GameItem g where g.visible = true and g.challengeId is not null")
    List<Long> findVisibleLinkedChallengeIds();

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
