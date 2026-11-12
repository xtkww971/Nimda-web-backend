package judgeServer.domain.group.dto;

import judgeServer.domain.group.entity.Group;
import judgeServer.domain.groupMember.entity.GroupMember;
import judgeServer.domain.groupMember.enums.MemberRole;

import java.util.Comparator;
import java.util.List;

/** CTF 팀 카드 (운영자 팀 빌딩, 내 팀 조회). 멤버는 팀장 먼저, 그다음 닉네임 순. */
public record TeamResponse(Long id, String name, String description, int capacity, int memberCount,
                           List<Member> members) {

    /**
     * @param userId  users.id (API에서 유저를 가리킬 때 쓰는 값)
     * @param loginId 로그인 아이디 (화면에 닉네임 옆에 보여주는 값)
     */
    public record Member(Long userId, String loginId, String nickname, String studentNum, MemberRole role) {
        static Member from(GroupMember m) {
            return new Member(m.getUser().getId(), m.getUser().getUserId(), m.getUser().getNickname(),
                    m.getUser().getStudentNum(), m.getRole());
        }
    }

    public static TeamResponse from(Group group) {
        List<Member> members = group.getMembers().stream()
                .map(Member::from)
                .sorted(Comparator.comparing((Member m) -> m.role().ordinal())
                        .thenComparing(Member::nickname, Comparator.nullsLast(String::compareTo)))
                .toList();
        return new TeamResponse(group.getId(), group.getName(), group.getDescription(), group.getCapacity(),
                members.size(), members);
    }
}
