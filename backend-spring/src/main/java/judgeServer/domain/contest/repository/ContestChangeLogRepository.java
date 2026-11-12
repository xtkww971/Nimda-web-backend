package judgeServer.domain.contest.repository;

import judgeServer.domain.contest.dto.ChangeLogResponse;
import judgeServer.domain.contest.entity.ContestChangeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContestChangeLogRepository extends JpaRepository<ContestChangeLog, Long> {

    /** 최근 변경 순. 바꾼 운영자의 닉네임을 같이 준다 (자동 확정이나 탈퇴 등으로 없으면 null). */
    @Query("""
            select new judgeServer.domain.contest.dto.ChangeLogResponse(
                h.changedAt, u.nickname, h.field, h.oldValue, h.newValue)
            from ContestChangeLog h left join User u on u.id = h.changedBy
            where h.contestId = :contestId
            order by h.changedAt desc, h.id desc
            """)
    List<ChangeLogResponse> findRecent(@Param("contestId") Long contestId, Pageable pageable);
}
