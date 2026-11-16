package judgeServer.domain.groupMember.repository;

import com.nimda.cite.user.entity.User;
import judgeServer.domain.groupMember.entity.GroupMember;
import judgeServer.domain.groupMember.enums.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // 특정 그룹의 멤버 전체 조회
    List<GroupMember> findByGroupId(Long groupId);

    // 특정 유저가 속한 그룹 멤버십 전체 조회 (내가 가입한 그룹 목록 등)
    List<GroupMember> findByUserId(Long userId);

    // 그룹-유저로 특정 멤버십 조회 (권한 체크, role 확인 등)
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    // 이미 가입되어 있는지 여부 확인 (가입 신청/초대 승인 시 중복 체크)
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    // 그룹 인원 수 카운트 (capacity 체크용)
    long countByGroupId(Long groupId);

    // 특정 role을 가진 멤버 조회 (예: 그룹장 조회)
    Optional<GroupMember> findByGroupIdAndRole(Long groupId, MemberRole role);

    // 대회 안에서 이미 팀에 속한 멤버십 (CTF는 대회마다 한 사람이 한 팀에만 속한다)
    @Query("select m from GroupMember m join fetch m.user join fetch m.group g where m.user.id in :userIds and g.type = judgeServer.domain.group.enums.GroupType.CTF_TEAM and (g.contestId = :contestId or (:contestId is null and g.contestId is null))")
    List<GroupMember> findInContestByUserIds(@Param("userIds") Collection<Long> userIds,
                                             @Param("contestId") Long contestId);

    // 대회 안에서 유저가 속한 팀 id
    @Query("select g.id from GroupMember m join m.group g where m.user.id = :userId and g.type = judgeServer.domain.group.enums.GroupType.CTF_TEAM and (g.contestId = :contestId or (:contestId is null and g.contestId is null)) order by m.id asc")
    List<Long> findGroupIdsByUserIdInContest(@Param("userId") Long userId, @Param("contestId") Long contestId);

    // 대회 안에서 아직 팀이 없는 승인된 유저 (운영자 팀 빌딩의 UNASSIGNED 목록)
    @Query("""
            select u from User u
            where u.status = com.nimda.cite.user.enums.ApprovalStatus.APPROVED
              and not exists (select 1 from GroupMember m join m.group g where m.user = u and g.type = judgeServer.domain.group.enums.GroupType.CTF_TEAM and (g.contestId = :contestId or (:contestId is null and g.contestId is null)))
              and (:keyword is null
                   or lower(u.nickname) like lower(concat('%', :keyword, '%'))
                   or lower(u.userId) like lower(concat('%', :keyword, '%')))
            order by u.nickname asc
            """)
    List<User> findUnassignedApprovedUsers(@Param("keyword") String keyword, @Param("contestId") Long contestId);

    // 탈퇴 처리 (그룹-유저 조합으로 삭제)
    void deleteByGroupIdAndUserId(Long groupId, Long userId);

}

