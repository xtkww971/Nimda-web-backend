package judgeServer.domain.group.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import judgeServer.domain.group.dto.GroupCreateRequest;
import judgeServer.domain.group.dto.GroupDetailResponse;
import judgeServer.domain.group.dto.GroupListResponse;
import judgeServer.domain.group.dto.GroupUpdateRequest;
import judgeServer.domain.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(UrlNames.groupUrl)
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // 그룹 생성
    @PostMapping(UrlNames.createGroupUrl)
    public ResponseEntity<?> createGroup(
            @Valid @RequestBody GroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        GroupDetailResponse response = groupService.createGroup(userId, request);

        return ApiResponse.ok("그룹이 성공적으로 생성되었습니다.", response)
                .toResponse(HttpStatus.CREATED);
    }

    // 그룹 목록 조회 (keyword가 있으면 이름 검색)
    @GetMapping(UrlNames.getGroupsUrl)
    public ResponseEntity<?> getGroups(
            @RequestParam(required = false) String keyword
    ) {
        List<GroupListResponse> groups = (keyword == null || keyword.isBlank())
                ? groupService.getAllGroups()
                : groupService.searchGroups(keyword);

        return ApiResponse.ok("그룹 목록을 성공적으로 조회하였습니다.", groups)
                .toResponse(HttpStatus.OK);
    }

    // 그룹 상세 조회
    @GetMapping(UrlNames.getGroupUrl)
    public ResponseEntity<?> getGroup(@PathVariable Long groupId) {
        GroupDetailResponse response = groupService.getGroup(groupId);

        return ApiResponse.ok("그룹을 성공적으로 조회하였습니다.", response)
                .toResponse(HttpStatus.OK);
    }

    // 그룹 수정 (그룹장만)
    @PatchMapping(UrlNames.updateGroupUrl)
    public ResponseEntity<?> updateGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GroupUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        groupService.updateGroup(groupId, userId, request);

        return ApiResponse.ok("그룹이 성공적으로 수정되었습니다.").toResponse(HttpStatus.OK);
    }

    // 그룹 삭제 (그룹장만)
    @DeleteMapping(UrlNames.deleteGroupUrl)
    public ResponseEntity<?> deleteGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        groupService.deleteGroup(groupId, userId);

        return ApiResponse.ok("그룹이 성공적으로 삭제되었습니다.").toResponse(HttpStatus.OK);
    }
}
