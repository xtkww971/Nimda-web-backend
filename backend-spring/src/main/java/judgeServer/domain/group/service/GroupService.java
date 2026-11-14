package judgeServer.domain.group.service;

import com.nimda.cite.common.exception.AlreadyExistException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.errorcode.ctf.GroupErrorCode;
import com.nimda.cite.common.exception.errorcode.user.UserErrorCode;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.repository.UserRepository;
import judgeServer.domain.group.dto.GroupCreateRequest;
import judgeServer.domain.group.dto.GroupDetailResponse;
import judgeServer.domain.group.dto.GroupListResponse;
import judgeServer.domain.group.dto.GroupUpdateRequest;
import judgeServer.domain.group.entity.Group;
import judgeServer.domain.group.enums.GroupType;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.groupMember.enums.MemberRole;
import judgeServer.domain.groupMember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;

    // @Autowired
    // private ActivityRepository activityRepository;

    private final GroupMemberService groupMemberService;

    private final UserRepository userRepository;


    @Transactional
    public GroupDetailResponse createGroup(Long userId, GroupCreateRequest request) {
        User user = getUser(userId);
        validateUniqueName(request.getName(), null);

        Group group = Group.builder()
                .name(request.getName())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .type(GroupType.STUDY)
                .build();
        groupRepository.save(group);

        // 그룹 생성자는 자동으로 LEADER 권한의 멤버가 됨
        groupMemberService.addMember(group, user, MemberRole.LEADER);

        return GroupDetailResponse.from(group);
    }

    @Transactional
    public void updateGroup(Long groupId, Long userId, GroupUpdateRequest request) {
        Group group = getStudyGroup(groupId);
        validateLeader(groupId, userId);
        validateUniqueName(request.getName(), groupId);

        group.update(request.getName(), request.getCapacity(), request.getDescription());
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = getStudyGroup(groupId);
        validateLeader(groupId, userId);

        groupRepository.delete(group); // cascade로 GroupMember 함께 삭제
    }

    public GroupDetailResponse getGroup(Long groupId) {
        Group group = getStudyGroup(groupId);
        return GroupDetailResponse.from(group);
    }

    public List<GroupListResponse> searchGroups(String keyword) {
        return groupRepository.findByNameContainingWithMembers(keyword, GroupType.STUDY).stream()
                .map(GroupListResponse::from)
                .toList();
    }

    public List<GroupListResponse> getAllGroups() {
        return groupRepository.findAllWithMembers(GroupType.STUDY).stream()
                .map(GroupListResponse::from)
                .toList();
    }

    // CTF 팀도 groups 테이블에 있다. 스터디 그룹 API로는 CTF 팀을 보거나 바꾸지 못하게 없는 그룹으로 본다.
    private Group getStudyGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .filter(Group::isStudy)
                .orElseThrow(() -> new NotFoundException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateLeader(Long groupId, Long userId) {
        groupMemberService.validateLeader(groupId, userId);
    }

    // excludeGroupId가 null이면 생성 시 체크, 값이 있으면 본인 그룹은 제외하고 수정 시 체크
    private void validateUniqueName(String name, Long excludeGroupId) {
        boolean duplicated = (excludeGroupId == null)
                ? groupRepository.existsByNameAndType(name, GroupType.STUDY)
                : groupRepository.existsByNameAndTypeAndIdNot(name, GroupType.STUDY, excludeGroupId);
        if (duplicated) {
            throw new AlreadyExistException(GroupErrorCode.GROUP_NAME_DUPLICATED);
        }
    }



}
