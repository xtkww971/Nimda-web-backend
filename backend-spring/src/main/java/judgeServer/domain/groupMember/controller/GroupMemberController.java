package judgeServer.domain.groupMember.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.groupMember.dto.GroupMemberResponse;
import judgeServer.domain.groupMember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(UrlNames.groupMemberUrl)
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    // 그룹 멤버 목록 조회
    @GetMapping(UrlNames.getMembersUrl)
    public ResponseEntity<?> getMembers(@PathVariable Long groupId) {
        List<GroupMemberResponse> members = groupMemberService.getMembers(groupId);

        return ApiResponse.ok("그룹 멤버 목록을 성공적으로 조회하였습니다.", members)
                .toResponse(HttpStatus.OK);
    }

    // 본인 탈퇴
    @DeleteMapping(UrlNames.leaveGroupUrl)
    public ResponseEntity<?> leave(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        groupMemberService.leave(groupId, userId);

        return ApiResponse.ok("그룹에서 탈퇴하였습니다.").toResponse(HttpStatus.OK);
    }

    // 강퇴 (그룹장만)
    @DeleteMapping(UrlNames.kickMemberUrl)
    public ResponseEntity<?> kick(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long leaderId = userDetails.getUser().getId();
        groupMemberService.kick(groupId, leaderId, targetUserId);

        return ApiResponse.ok("해당 멤버를 그룹에서 강퇴하였습니다.").toResponse(HttpStatus.OK);
    }

    // 그룹장 위임 (기존 그룹장 -> targetUserId). 위임 후 기존 그룹장은 leave 가능
    @PatchMapping(UrlNames.transferLeadershipUrl)
    public ResponseEntity<?> transferLeadership(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentLeaderId = userDetails.getUser().getId();
        groupMemberService.transferLeadership(groupId, currentLeaderId, targetUserId);

        return ApiResponse.ok("그룹장 권한을 위임하였습니다.").toResponse(HttpStatus.OK);
    }
}
