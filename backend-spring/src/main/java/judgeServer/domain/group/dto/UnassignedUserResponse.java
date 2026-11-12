package judgeServer.domain.group.dto;

import com.nimda.cite.user.entity.User;

/** 아직 팀이 없는 참가자 (운영자 팀 빌딩의 UNASSIGNED 목록). */
public record UnassignedUserResponse(Long userId, String loginId, String nickname, String studentNum) {

    public static UnassignedUserResponse from(User user) {
        return new UnassignedUserResponse(user.getId(), user.getUserId(), user.getNickname(), user.getStudentNum());
    }
}
