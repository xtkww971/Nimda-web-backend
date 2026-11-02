package com.nimda.cite.user.controller;

import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.dto.adminuser.AdminUserListResponseDTO;
import com.nimda.cite.user.dto.adminuser.AdminUserResponseDTO;
import com.nimda.cite.user.dto.adminuser.AdminUserRoleUpdateRequestDTO;
import com.nimda.cite.user.dto.adminuser.AdminUserRolesResponseDTO;
import com.nimda.cite.user.dto.adminuser.AdminUsersResponseDTO;
import com.nimda.cite.user.dto.UserSuggestionResponseDTO;
import com.nimda.cite.user.enums.ApprovalStatus;
import com.nimda.cite.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 관리자용 사용자 관리 컨트롤러
 *
 * [역할]
 * - 사용자 승인/거부
 * - 승인 대기 사용자 목록 조회
 * - 모든 사용자 조회
 *
 * [보안]
 * - 클래스 레벨: @PreAuthorize("hasRole('ADMIN')")로 관리자 권한 체크
 * - SecurityConfig: /api/admin/** 패턴으로 관리자 권한 설정
 */
@RestController
@RequestMapping(UrlNames.adminUserUrl)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    private final AdminUserService adminUserService;

    /**
     * 모든 사용자 조회
     *
     * @return 사용자 목록
     */
    @GetMapping(UrlNames.getAllUsersUrl)
    public ResponseEntity<AdminUsersResponseDTO<AdminUserListResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(AdminUsersResponseDTO.of(adminUserService.findAll()));
    }

    /**
     * 승인 대기 사용자 목록 조회
     *
     * @return 승인 대기 사용자 목록 ({success, users, count})
     */
    @GetMapping(UrlNames.getPendingUserUrl)
    public ResponseEntity<AdminUsersResponseDTO<AdminUserListResponseDTO>> getPendingUsers() {
        return ResponseEntity.ok(AdminUsersResponseDTO.withCount(
                adminUserService.findByStatus(ApprovalStatus.PENDING)));
    }

    /**
     * 사용자 승인
     * - status를 APPROVED로 변경
     * - ROLE_USER 권한 부여
     *
     * @param id 사용자 ID
     * @return 승인된 사용자 정보 ({success, message, user})
     */
    @PutMapping(UrlNames.approveUserUrl)
    public ResponseEntity<AdminUserResponseDTO> approveUser(@PathVariable Long id) {
        return ResponseEntity.ok(AdminUserResponseDTO.of(
                "사용자가 승인되었습니다.",
                adminUserService.approveUser(id)));
    }

    /**
     * 사용자 거부
     * - status를 REJECTED로 변경
     *
     * @param id 사용자 ID
     * @return 거부된 사용자 정보 ({success, message, user})
     */
    @PutMapping(UrlNames.rejectUserUrl)
    public ResponseEntity<AdminUserResponseDTO> rejectUser(@PathVariable Long id) {
        return ResponseEntity.ok(AdminUserResponseDTO.of(
                "사용자 승인이 거부되었습니다.",
                adminUserService.rejectUser(id)));
    }

    /**
     * 서버에 등록된 전체 권한 목록 조회 ({success, roles})
     */
    @GetMapping(UrlNames.getAvailableRolesUrl)
    public ResponseEntity<AdminUserRolesResponseDTO> getAvailableRoles() {
        return ResponseEntity.ok(AdminUserRolesResponseDTO.of(adminUserService.findAllRoleNames()));
    }

    /**
     * 사용자 권한 1개를 추가 부여 ({success, message, roles} : 부여 후 사용자의 권한 목록)
     */
    @PutMapping(UrlNames.grantRoleUrl)
    public ResponseEntity<AdminUserRolesResponseDTO> grantRole(
            @PathVariable Long id,
            @RequestBody AdminUserRoleUpdateRequestDTO request) {
        return ResponseEntity.ok(AdminUserRolesResponseDTO.of(
                "권한이 부여되었습니다.",
                adminUserService.grantRole(id, request.getRole()).getRoles()));
    }

    /**
     * 사용자 권한 1개 제거 ({success, message, roles} : 제거 후 사용자의 권한 목록)
     */
    @PutMapping(UrlNames.revokeRoleUrl)
    public ResponseEntity<AdminUserRolesResponseDTO> revokeRole(
            @PathVariable Long id,
            @RequestBody AdminUserRoleUpdateRequestDTO request) {
        return ResponseEntity.ok(AdminUserRolesResponseDTO.of(
                "권한이 제거되었습니다.",
                adminUserService.revokeRole(id, request.getRole()).getRoles()));
    }

    /**
     * 특정 사용자 상세 조회
     *
     * @param id 사용자 ID
     * @return 사용자 정보 ({success, user})
     */
    @GetMapping(UrlNames.getUserByIdUrl)
    public ResponseEntity<AdminUserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(AdminUserResponseDTO.of(adminUserService.findById(id)));
    }

    /**
     *  adminUserService에 deleteTag 기능 자체가 없음
     */
    @PostMapping(UrlNames.deleteTagUrl)
    public ResponseEntity<String> deleteTag(@PathVariable String tag) {
        return ResponseEntity.status(HttpStatus.OK).body(tag);
    }

    /**
     * 마일리지 유저 추천 목록
     */
    @GetMapping(UrlNames.getUserSuggestionsUrl)
    public ResponseEntity<AdminUsersResponseDTO<UserSuggestionResponseDTO>> getUserSuggestions() {
        return ResponseEntity.ok(AdminUsersResponseDTO.of(adminUserService.findUserSuggestions()));
    }
}

