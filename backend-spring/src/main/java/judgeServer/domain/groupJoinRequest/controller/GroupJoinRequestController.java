package judgeServer.domain.groupJoinRequest.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.groupJoinRequest.dto.GroupJoinRequestIdResponse;
import judgeServer.domain.groupJoinRequest.service.GroupJoinRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UrlNames.groupJoinRequestUrl)
@RequiredArgsConstructor
public class GroupJoinRequestController {

    private final GroupJoinRequestService groupJoinRequestService;

    // 가입 신청
    @PostMapping(UrlNames.applyToGroupUrl)
    public ResponseEntity<?> apply(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        Long requestId = groupJoinRequestService.applyToGroup(groupId, userId);

        return ApiResponse.ok("가입 신청이 완료되었습니다.", GroupJoinRequestIdResponse.of(requestId))
                .toResponse(HttpStatus.CREATED);
    }

    // 초대
    @PostMapping(UrlNames.inviteToGroupUrl)
    public ResponseEntity<?> invite(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long inviteeId
    ) {
        Long inviterId = userDetails.getUser().getId();
        Long requestId = groupJoinRequestService.inviteToGroup(groupId, inviterId, inviteeId);

        return ApiResponse.ok("초대가 완료되었습니다.", GroupJoinRequestIdResponse.of(requestId))
                .toResponse(HttpStatus.CREATED);
    }

    // 대기중인 요청 목록 (그룹장만 조회 가능)
    @GetMapping(UrlNames.getPendingRequestsUrl)
    public ResponseEntity<?> getPendingRequests(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long requesterId = userDetails.getUser().getId();

        return ApiResponse.ok("신청 목록을 성공적으로 조회하였습니다.",
                        groupJoinRequestService.getPendingRequestsOfGroup(groupId, requesterId))
                .toResponse(HttpStatus.OK);
    }

    // 수락
    @PostMapping(UrlNames.acceptJoinRequestUrl)
    public ResponseEntity<?> accept(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        groupJoinRequestService.accept(requestId, userId);

        return ApiResponse.ok("가입 요청을 수락하였습니다.").toResponse(HttpStatus.OK);
    }

    // 거절
    @PostMapping(UrlNames.rejectJoinRequestUrl)
    public ResponseEntity<?> reject(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        groupJoinRequestService.reject(requestId, userId);

        return ApiResponse.ok("가입 요청을 거절하였습니다.").toResponse(HttpStatus.OK);
    }

    // 취소 (신청자 본인)
    @DeleteMapping(UrlNames.cancelJoinRequestUrl)
    public ResponseEntity<?> cancel(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        groupJoinRequestService.cancel(requestId, userId);

        return ApiResponse.ok("신청을 취소하였습니다.").toResponse(HttpStatus.OK);
    }
}
