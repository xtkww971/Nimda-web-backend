package judgeServer.domain.schedule.repository;

import judgeServer.domain.schedule.dto.ScheduleHistoryResponse;
import judgeServer.domain.schedule.entity.ContestScheduleHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContestScheduleHistoryRepository extends JpaRepository<ContestScheduleHistory, Long> {

    /** 최근 변경 순. 바꾼 운영자의 닉네임을 같이 준다 (탈퇴 등으로 없으면 null). */
    @Query("""
            select new judgeServer.domain.schedule.dto.ScheduleHistoryResponse(
                h.changedAt, u.nickname, h.field, h.oldValue, h.newValue)
            from ContestScheduleHistory h left join User u on u.id = h.changedBy
            order by h.changedAt desc, h.id desc
            """)
    List<ScheduleHistoryResponse> findRecent(Pageable pageable);
}
