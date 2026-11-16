package judgeServer.domain.group.service;

import com.nimda.cite.common.exception.GlobalNimdaException;
import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.repository.UserRepository;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.dto.GroupCreateRequest;
import judgeServer.domain.group.entity.Group;
import judgeServer.domain.group.enums.GroupType;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.groupJoinRequest.repository.GroupJoinRequestRepository;
import judgeServer.domain.groupMember.repository.GroupMemberRepository;
import judgeServer.domain.groupMember.service.GroupMemberService;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** groups 테이블을 함께 쓰는 스터디 그룹과 CTF 팀이 서로의 API에서 보이지 않는지 확인한다. */
class GroupTypeSeparationTest {

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final GroupMemberRepository memberRepository = mock(GroupMemberRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    // Java 24에서는 Mockito가 구체 클래스를 mock하지 못해 실제 서비스에 mock 레포를 넣는다.
    private final GroupMemberService memberService = new GroupMemberService(memberRepository, groupRepository);
    private final GroupService groupService = new GroupService(groupRepository, memberService, userRepository);
    private final ContestService contestService = new ContestService(null, null, null, null, null) {
        @Override
        public Optional<Long> currentContestId() {
            return Optional.of(9L);
        }
    };
    private final AdminTeamService teamService = new AdminTeamService(groupRepository, memberRepository,
            mock(GroupJoinRequestRepository.class), userRepository, mock(CtfTeamScoreRepository.class),
            contestService);

    @Test
    void createdStudyGroupIsStudyType() {
        User user = User.builder().nickname("admin").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        groupService.createGroup(1L, createRequest("웹 스터디", 5));

        ArgumentCaptor<Group> saved = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(saved.capture());
        assertEquals(GroupType.STUDY, saved.getValue().getType());
        // 이름 중복은 스터디 그룹 안에서만 본다 (CTF 팀 이름과 겹쳐도 된다)
        verify(groupRepository).existsByNameAndType("웹 스터디", GroupType.STUDY);
    }

    @Test
    void groupBuiltWithoutTypeIsCtfTeam() {
        // V39 마이그레이션의 기본값과 같다.
        assertTrue(Group.builder().name("team").capacity(4).build().isCtfTeam());
    }

    @Test
    void studyApiHidesCtfTeam() {
        when(groupRepository.findById(5L)).thenReturn(Optional.of(group(5L, GroupType.CTF_TEAM)));

        GlobalNimdaException e = assertThrows(GlobalNimdaException.class, () -> groupService.getGroup(5L));
        assertEquals(ErrorCode.GROUP_NOT_FOUND, e.getErrorCode());
        GlobalNimdaException members = assertThrows(GlobalNimdaException.class,
                () -> memberService.getMembers(5L));
        assertEquals(ErrorCode.GROUP_NOT_FOUND, members.getErrorCode());
        verify(groupRepository, never()).delete(any());
    }

    @Test
    void teamApiHidesStudyGroup() {
        when(groupRepository.findByIdWithMembers(7L)).thenReturn(Optional.of(group(7L, GroupType.STUDY)));
        when(groupRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(group(7L, GroupType.STUDY)));

        GlobalNimdaException get = assertThrows(GlobalNimdaException.class, () -> teamService.getTeam(7L));
        assertEquals(ErrorCode.TEAM_NOT_FOUND, get.getErrorCode());
        GlobalNimdaException delete = assertThrows(GlobalNimdaException.class, () -> teamService.deleteTeam(7L));
        assertEquals(ErrorCode.TEAM_NOT_FOUND, delete.getErrorCode());
        verify(groupRepository, never()).delete(any());
    }

    private static Group group(Long id, GroupType type) {
        Group group = Group.builder().name("g" + id).capacity(5).type(type).build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private static GroupCreateRequest createRequest(String name, int capacity) {
        GroupCreateRequest request = new GroupCreateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "capacity", capacity);
        return request;
    }
}
