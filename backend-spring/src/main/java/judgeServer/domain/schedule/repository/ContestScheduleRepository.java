package judgeServer.domain.schedule.repository;

import jakarta.persistence.LockModeType;
import judgeServer.domain.schedule.entity.ContestSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContestScheduleRepository extends JpaRepository<ContestSchedule, Integer> {

    /** 운영자 둘이 동시에 저장해도 이력이 어긋나지 않도록 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ContestSchedule s where s.id = :id")
    Optional<ContestSchedule> findByIdForUpdate(@Param("id") Integer id);
}
