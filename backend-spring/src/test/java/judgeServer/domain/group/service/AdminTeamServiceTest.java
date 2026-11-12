package judgeServer.domain.group.service;

import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.enums.ApprovalStatus;
import com.nimda.cite.user.repository.UserRepository;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.dto.TeamResponse;
import judgeServer.domain.group.dto.TeamCreateRequest;
import judgeServer.domain.group.entity.Group;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.groupJoinRequest.repository.GroupJoinRequestRepository;
import judgeServer.domain.groupMember.entity.GroupMember;
import judgeServer.domain.groupMember.enums.MemberRole;
import judgeServer.domain.groupMember.repository.GroupMemberRepository;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminTeamServiceTest {

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final GroupMemberRepository memberRepository = mock(GroupMemberRepository.class);
    private final GroupJoinRequestRepository joinRequestRepository = mock(GroupJoinRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CtfTeamScoreRepository teamScoreRepository = mock(CtfTeamScoreRepository.class);
    // Java 24에서는 Mockito가 구체 클래스를 mock하지 못해 하위 클래스로 현재 대회(id=9)를 고정한다.
    private final ContestService contestService = new ContestService(null, null, null, null, null) {
        @Override
        public Optional<Long> currentContestId() {
            return Optional.of(9L);
        }
    };
    private final AdminTeamService service =
            new AdminTeamService(groupRepository, memberRepository, joinRequestRepository, userRepository,
                    teamScoreRepository, contestService);

    @Test
    void createTeamIncludesLeaderAndAssignsRoles() {
        List<User> users = List.of(user(1L, "a"), user(2L, "b"));
        when(userRepository.findAllById(anyCollection())).thenReturn(users);
        when(memberRepository.findInContestByUserIds(anyCollection(), eq(9L))).thenReturn(List.of());

        TeamResponse response = service.createTeam(request("team", 4, 1L, List.of(2L)));

        assertEquals(2, response.memberCount());
        assertEquals(MemberRole.LEADER, response.members().get(0).role());
        assertEquals(1L, response.members().get(0).userId());
        assertEquals(MemberRole.MEMBER, response.members().get(1).role());
        ArgumentCaptor<Group> saved = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(saved.capture());
        // 팀은 현재 대회에 묶인다
        assertEquals(9L, saved.getValue().getContestId());
        // 팀을 만들 때 팀 점수판 행도 만든다
        verify(teamScoreRepository).initialize(any());
    }

    @Test
    void createTeamRejectsMembersOverCapacity() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createTeam(request("team", 1, 1L, List.of(2L))));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(groupRepository, never()).save(any());
    }

    @Test
    void createTeamRejectsUserAlreadyInAnotherTeam() {
        User a = user(1L, "a");
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(a));
        Group other = Group.builder().name("other").capacity(4).build();
        when(memberRepository.findInContestByUserIds(anyCollection(), eq(9L)))
                .thenReturn(List.of(GroupMember.builder().group(other).user(a).role(MemberRole.MEMBER).build()));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createTeam(request("team", 4, null, List.of(1L))));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void createTeamRejectsUnapprovedUser() {
        User pending = user(1L, "a");
        pending.changeStatus(ApprovalStatus.PENDING);
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(pending));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createTeam(request("team", 4, null, List.of(1L))));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    private static TeamCreateRequest request(String name, int capacity, Long leaderId, List<Long> memberIds) {
        TeamCreateRequest request = new TeamCreateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "capacity", capacity);
        ReflectionTestUtils.setField(request, "leaderUserId", leaderId);
        ReflectionTestUtils.setField(request, "memberUserIds", memberIds);
        return request;
    }

    // Java 24에서는 Mockito가 엔티티 클래스를 mock하지 못해 실제 객체를 만든다.
    private static User user(Long id, String nickname) {
        User user = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        user.changeStatus(ApprovalStatus.APPROVED);
        return user;
    }
}
