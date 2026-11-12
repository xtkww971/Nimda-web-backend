package judgeServer.domain.group.controller;

import com.nimda.cite.common.response.ApiResponse;
import jakarta.validation.Valid;
import judgeServer.domain.group.dto.GroupUpdateRequest;
import judgeServer.domain.group.dto.TeamCreateRequest;
import judgeServer.domain.group.dto.TeamLeaderRequest;
import judgeServer.domain.group.dto.TeamMemberAddRequest;
import judgeServer.domain.group.service.AdminTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 운영자 CTF 팀 관리 (ADMIN 전용). 스키마는 group 도메인을 재사용한다.
 *
 * <pre>
 *   GET    /api/admin/ctf/teams                          -> 팀 목록 (멤버 포함, 팀 이름 순)
 *   GET    /api/admin/ctf/teams/unassigned?keyword=      -> 미배정 참가자 (닉네임·아이디 검색)
 *   GET    /api/admin/ctf/teams/{teamId}                 -> 팀 상세 (멤버 포함)
 *   POST   /api/admin/ctf/teams                          -> 팀 생성 (멤버·팀장 함께 지정 가능, 팀 점수판도 생성)
 *   PATCH  /api/admin/ctf/teams/{teamId}                 -> 이름·정원·설명 수정
 *   DELETE /api/admin/ctf/teams/{teamId}                 -> 팀 삭제
 *   POST   /api/admin/ctf/teams/{teamId}/members         -> 멤버 일괄 배정 ({userIds:[...]})
 *   DELETE /api/admin/ctf/teams/{teamId}/members/{userId} -> 멤버 제거
 *   PATCH  /api/admin/ctf/teams/{teamId}/leader          -> 팀장 변경
 * </pre>
 *
 * <p>{@code /api/admin/**} URL 규칙(SecurityConfig)과 메서드 보안 두 곳에서 ADMIN을 확인한다.
 */
@RestController
@RequestMapping("/api/admin/ctf/teams")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTeamController {

    private final AdminTeamService teamService;

    @GetMapping
    public ResponseEntity<?> getTeams() {
        return ApiResponse.ok("팀 목록을 조회하였습니다.", teamService.getTeams()).toResponse();
    }

    @GetMapping("/unassigned")
    public ResponseEntity<?> getUnassignedUsers(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok("미배정 참가자를 조회하였습니다.", teamService.getUnassignedUsers(keyword)).toResponse();
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getTeam(@PathVariable Long teamId) {
        return ApiResponse.ok("팀을 조회하였습니다.", teamService.getTeam(teamId)).toResponse();
    }

    @PostMapping
    public ResponseEntity<?> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        return ApiResponse.ok("팀이 생성되었습니다.", teamService.createTeam(request))
                .toResponse(HttpStatus.CREATED);
    }

    @PatchMapping("/{teamId}")
    public ResponseEntity<?> updateTeam(@PathVariable Long teamId,
                                        @Valid @RequestBody GroupUpdateRequest request) {
        return ApiResponse.ok("팀이 수정되었습니다.", teamService.updateTeam(teamId, request)).toResponse();
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> deleteTeam(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ApiResponse.ok("팀이 삭제되었습니다.").toResponse();
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long teamId,
                                       @Valid @RequestBody TeamMemberAddRequest request) {
        return ApiResponse.ok("멤버가 배정되었습니다.", teamService.addMembers(teamId, request))
                .toResponse(HttpStatus.CREATED);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        return ApiResponse.ok("멤버가 제거되었습니다.", teamService.removeMember(teamId, userId)).toResponse();
    }

    @PatchMapping("/{teamId}/leader")
    public ResponseEntity<?> changeLeader(@PathVariable Long teamId,
                                          @Valid @RequestBody TeamLeaderRequest request) {
        return ApiResponse.ok("팀장이 변경되었습니다.", teamService.changeLeader(teamId, request)).toResponse();
    }
}
