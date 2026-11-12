package judgeServer.domain.group.repository;

import jakarta.persistence.LockModeType;
import judgeServer.domain.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    // 단건 조회 - 멤버까지 함께 (상세 조회용)
    @Query("select g from Group g left join fetch g.members where g.id = :id")
    Optional<Group> findByIdWithMembers(@Param("id") Long id);

    // 목록 조회 - 멤버까지 함께 (currentMemberCount 계산용)
    @Query("select distinct g from Group g left join fetch g.members where g.name like %:keyword%")
    List<Group> findByNameContainingWithMembers(@Param("keyword") String keyword);

    // 전체 목록 조회 - 멤버까지 함께
    @Query("select distinct g from Group g left join fetch g.members")
    List<Group> findAllWithMembers();

    // 대회의 팀 목록 - 멤버까지 함께. contestId가 null이면 대회에 묶이지 않은 팀
    @Query("select distinct g from Group g left join fetch g.members where (g.contestId = :contestId or (:contestId is null and g.contestId is null))")
    List<Group> findAllWithMembersInContest(@Param("contestId") Long contestId);

    @Query("select count(g) from Group g where (g.contestId = :contestId or (:contestId is null and g.contestId is null))")
    long countInContest(@Param("contestId") Long contestId);

    // 팀 이름 중복 체크 (대회 안에서). excludeId가 있으면 그 팀은 빼고 본다
    @Query("select count(g) > 0 from Group g where g.name = :name and (g.contestId = :contestId or (:contestId is null and g.contestId is null)) and (:excludeId is null or g.id <> :excludeId)")
    boolean existsByNameInContest(@Param("name") String name, @Param("contestId") Long contestId,
                                  @Param("excludeId") Long excludeId);

    // 첫 대회를 만들 때, 대회 없이 만든 팀을 그 대회에 편입한다
    @Modifying
    @Query("update Group g set g.contestId = :contestId where g.contestId is null")
    int assignOrphansToContest(@Param("contestId") Long contestId);

    // 그룹명 중복 체크 (생성 시)
    boolean existsByName(String name);

    // 그룹명 중복 체크 (수정 시, 본인 그룹 제외)
    boolean existsByNameAndIdNot(String name, Long id);

    // 정원 초과 방지용 비관적 락 조회 (가입 요청 수락 시 동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Group g where g.id = :id")
    Optional<Group> findByIdForUpdate(@Param("id") Long id);

}