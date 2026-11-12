package judgeServer.domain.contest.repository;

import jakarta.persistence.LockModeType;
import judgeServer.domain.contest.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    /** 현재 대회 = 가장 최근에 시작하는 대회. */
    Optional<Contest> findFirstByOrderByStartsAtDescIdDesc();

    /** 일정 저장·결과 확정이 동시에 일어나도 어긋나지 않도록 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contest c where c.id = :id")
    Optional<Contest> findByIdForUpdate(@Param("id") Long id);

    /** 이력에 오른(결과가 확정된) 대회. */
    List<Contest> findByFinalizedAtIsNotNull();

    List<Contest> findAllByOrderByStartsAtDescIdDesc();
}
