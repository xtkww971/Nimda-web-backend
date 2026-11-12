package judgeServer.domain.group.service;

import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.enums.ApprovalStatus;
import com.nimda.cite.user.repository.UserRepository;
import judgeServer.domain.group.dto.*;
import judgeServer.domain.group.entity.Group;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.groupJoinRequest.repository.GroupJoinRequestRepository;
import judgeServer.domain.groupMember.entity.GroupMember;
import judgeServer.domain.groupMember.enums.MemberRole;
import judgeServer.domain.groupMember.repository.GroupMemberRepository;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 운영자의 CTF 팀 관리. 스키마는 group 도메인(groups, group_members)을 그대로 쓴다.
 *
 * <p>일반 그룹 API({@link GroupService})와 다른 점:
 * <ul>
 *   <li>운영자는 팀 멤버가 되지 않는다. 팀장(LEADER)은 운영자가 지정한다.</li>
 *   <li>Activity가 CTF 하나뿐이므로 한 유저는 한 팀에만 속할 수 있다.</li>
 *   <li>승인(APPROVED)된 유저만 팀에 넣을 수 있다.</li>
 *   <li>팀을 만들 때 팀 점수판(ctf_team_scores) 행을 같이 만든다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTeamService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final CtfTeamScoreRepository teamScoreRepository;

    /** 팀 목록 (멤버 포함, 팀 이름 순). */
    public List<TeamResponse> getTeams() {
        return groupRepository.findAllWithMembers().stream()
                .sorted(Comparator.comparing(Group::getName, String.CASE_INSENSITIVE_ORDER))
                .map(TeamResponse::from)
                .toList();
    }

    public TeamResponse getTeam(Long teamId) {
        return TeamResponse.from(groupRepository.findByIdWithMembers(teamId).orElseThrow(this::teamNotFound));
    }

    /** 아직 팀이 없는 승인된 참가자. keyword는 닉네임·아이디 부분 일치. */
    public List<UnassignedUserResponse> getUnassignedUsers(String keyword) {
        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return memberRepository.findUnassignedApprovedUsers(normalized).stream()
                .map(UnassignedUserResponse::from)
                .toList();
    }

    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        validateUniqueName(request.getName(), null);

        Long leaderId = request.getLeaderUserId();
        LinkedHashSet<Long> memberIds = new LinkedHashSet<>();
        if (leaderId != null) {
            memberIds.add(leaderId);
        }
        if (request.getMemberUserIds() != null) {
            memberIds.addAll(request.getMemberUserIds());
        }
        if (memberIds.size() > request.getCapacity()) {
            throw badRequest("멤버 수(" + memberIds.size() + ")가 최대 인원(" + request.getCapacity() + ")을 넘습니다.");
        }

        Map<Long, User> users = loadAssignableUsers(memberIds);

        Group team = Group.builder()
                .name(request.getName())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .build();
        for (Long userId : memberIds) {
            team.addMember(GroupMember.builder()
                    .group(team)
                    .user(users.get(userId))
                    .role(userId.equals(leaderId) ? MemberRole.LEADER : MemberRole.MEMBER)
                    .build());
        }
        groupRepository.save(team);
        // 팀 점수판은 팀을 만들 때 0점으로 같이 만든다. 점수가 0인 동안은 순위에 나오지 않는다.
        teamScoreRepository.initialize(team.getId());
        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse updateTeam(Long teamId, GroupUpdateRequest request) {
        Group team = lockTeam(teamId);
        validateUniqueName(request.getName(), teamId);
        if (request.getCapacity() < team.getMembers().size()) {
            throw badRequest("현재 멤버 수(" + team.getMembers().size() + ")보다 최대 인원을 작게 할 수 없습니다.");
        }
        team.update(request.getName(), request.getCapacity(), request.getDescription());
        return TeamResponse.from(team);
    }

    /** 팀을 지운다. 멤버는 미배정으로 돌아가고 팀 점수판 행도 지워진다. 정답 기록(solves)은 남는다. */
    @Transactional
    public void deleteTeam(Long teamId) {
        Group team = lockTeam(teamId);
        joinRequestRepository.deleteByGroupId(teamId);
        teamScoreRepository.deleteById(teamId);
        groupRepository.delete(team); // cascade로 group_members 함께 삭제
    }

    /** 여러 명을 한 번에 MEMBER로 배정한다. 한 명이라도 안 되면 아무도 배정하지 않는다. */
    @Transactional
    public TeamResponse addMembers(Long teamId, TeamMemberAddRequest request) {
        // 정원 확인과 추가 사이에 다른 추가가 끼어들지 않도록 팀 행을 잠근다.
        Group team = lockTeam(teamId);
        LinkedHashSet<Long> userIds = new LinkedHashSet<>(request.getUserIds());
        int remaining = team.getCapacity() - team.getMembers().size();
        if (userIds.size() > remaining) {
            throw badRequest("팀 정원(" + team.getCapacity() + ")을 넘습니다. 남은 자리: " + Math.max(remaining, 0) + "명");
        }

        Map<Long, User> users = loadAssignableUsers(userIds);
        for (Long userId : userIds) {
            team.addMember(GroupMember.builder().group(team).user(users.get(userId)).role(MemberRole.MEMBER).build());
        }
        groupRepository.flush(); // 응답에 새 멤버가 반영되도록 INSERT를 먼저 보낸다
        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse removeMember(Long teamId, Long userId) {
        Group team = lockTeam(teamId);
        if (!team.removeMemberByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저는 이 팀의 멤버가 아닙니다.");
        }
        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse changeLeader(Long teamId, TeamLeaderRequest request) {
        Group team = lockTeam(teamId);
        GroupMember target = team.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(request.getUserId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저는 이 팀의 멤버가 아닙니다."));

        team.getMembers().stream()
                .filter(m -> m.getRole() == MemberRole.LEADER)
                .forEach(m -> m.updateRole(MemberRole.MEMBER));
        target.updateRole(MemberRole.LEADER);
        return TeamResponse.from(team);
    }

    /** 존재하고, 승인됐고, 아직 어느 팀에도 속하지 않은 유저만 돌려준다. 하나라도 어긋나면 404/400/409. */
    private Map<Long, User> loadAssignableUsers(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> missing = userIds.stream().filter(id -> !users.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다: " + missing);
        }

        List<String> notApproved = users.values().stream()
                .filter(u -> u.getStatus() != ApprovalStatus.APPROVED)
                .map(User::getNickname)
                .toList();
        if (!notApproved.isEmpty()) {
            throw badRequest("승인되지 않은 유저는 팀에 넣을 수 없습니다: " + String.join(", ", notApproved));
        }

        List<String> alreadyInTeam = memberRepository.findByUserIdIn(userIds).stream()
                .map(m -> m.getUser().getNickname())
                .distinct()
                .toList();
        if (!alreadyInTeam.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 다른 팀에 속한 유저가 있습니다: " + String.join(", ", alreadyInTeam));
        }
        return users;
    }

    private Group lockTeam(Long teamId) {
        return groupRepository.findByIdForUpdate(teamId).orElseThrow(this::teamNotFound);
    }

    private void validateUniqueName(String name, Long excludeTeamId) {
        boolean duplicated = (excludeTeamId == null)
                ? groupRepository.existsByName(name)
                : groupRepository.existsByNameAndIdNot(name, excludeTeamId);
        if (duplicated) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 팀 이름입니다.");
        }
    }

    private ResponseStatusException teamNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 팀입니다.");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
