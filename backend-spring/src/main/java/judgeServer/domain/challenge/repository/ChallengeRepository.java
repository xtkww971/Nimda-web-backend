package judgeServer.domain.challenge.repository;

import jakarta.persistence.LockModeType;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.ChallengeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    /** 정답 처리 순서를 문제 단위로 세우기 위해 행을 잠근다. 트랜잭션의 첫 조회로 불러야 한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Challenge c where c.id = :id")
    Optional<Challenge> findByIdForUpdate(@Param("id") Long id);

    Optional<Challenge> findByCode(String code);
    boolean existsByCode(String code);

    /** 참가자에게 보여줄 목록 (공개된 것만). */
    List<Challenge> findByIsPublicTrueOrderByIdAsc();

    /** 관리자 목록 (초안 포함). */
    List<Challenge> findAllByOrderByIdAsc();
    Page<Challenge> findByCategory(ChallengeCategory category, Pageable pageable);
    Page<Challenge> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
