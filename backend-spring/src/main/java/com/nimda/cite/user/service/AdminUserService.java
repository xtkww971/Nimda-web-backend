package com.nimda.cite.user.service;

import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.errorcode.user.AdminUserErrorCode;
import com.nimda.cite.config.CheckConfig;
import com.nimda.cite.user.dto.adminuser.AdminUserDetailResponseDTO;
import com.nimda.cite.user.dto.adminuser.AdminUserListResponseDTO;
import com.nimda.cite.user.dto.UserSuggestionResponseDTO;
import com.nimda.cite.user.entity.Authority;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.enums.ApprovalStatus;
import com.nimda.cite.user.repository.AuthorityRepository;
import com.nimda.cite.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 사용자 관리 서비스
 *
 * [역할]
 * - 사용자 승인/거부
 * - 승인 대기 사용자 목록 조회
 * - 모든 사용자 조회
 *
 * [책임 분리]
 * - 일반 사용자 기능: UserService
 * - 관리자 기능: AdminUserService
 */

// npe check config 공통 class생성시 각 메서드 별로 parameter를 최상단에 검사
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    /**
     * 모든 사용자 조회
     */
    public List<AdminUserListResponseDTO> findAll() {
        return userRepository.findAll()
                .stream().map(AdminUserListResponseDTO::from).toList();
    }

    /**
     * ID로 사용자 조회
     */
    public AdminUserDetailResponseDTO findById(Long userId) {
        CheckConfig.npeAndNegativeCheck(userId, "userId");

        return AdminUserDetailResponseDTO.from(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.USER_NOT_FOUND)));
    }

    /**
     * 승인 상태로 사용자 목록 조회
     */
    public List<AdminUserListResponseDTO> findByStatus(ApprovalStatus status) {
        CheckConfig.npeCheck(status, "status");

        return userRepository.findByStatus(status)
                .stream()
                .map(AdminUserListResponseDTO::from)
                .toList();
    }

    /**
     * 사용자 승인
     * - status를 APPROVED로 변경
     * - ROLE_USER 권한 부여
     *
     *
     * 문제점
     *  - user.Authorities 필드는 지연탐색을 통해 필요에 따라 탐색이 발생함
     *      -> userRole을 통해 Authority 마스터 테이블에 해당 권한(ROLE_USER)이 존재하는지 파악 : (authority_id, authority_name)반환
     *      -> user.getAUthorities().contains()를 통해 지연탐색 시작
     *      -> 이때 user에게 이미 ROLE_USER가 존재한다고 가정하에, 탐색을 하게 되면 찾는 row가 위 마스터 테이블에서 찾는 값과 동일함
     *      -> hibernate는 default설정이 1차 캐시를 통해 동일한 값은 값은 객체를 가르키도록 함
     *      -> 따라서 contains()를 통해 찾은 row은 마스터 테이블에서 찾은 row와 동일 객체를 가르킴
     *      -> set의 contains()는 내부적으로 원소를 순회하며 equals()를 호출
     *      -> set의 주체인 Authority엔티티 내부에서 equals를 따로 오버라이딩하지 않아 두 값을 비교하는 것이 아닌, objects.equals()를 호출하여 두 객체가 같은건지 파악
     *      -> 지금은 1차 캐시로 인해 두 객체가 같아 통과되지만, 추후에 같은 세션의 map을 안거치게 되는 모든 경우에서 에러 가능
     *      -> 따라서 object.equals()를 유도하는 contains() 대신 authority_name을 통해 직접 비교
     */
    @Transactional
    public AdminUserDetailResponseDTO approveUser(Long userId) {
        CheckConfig.npeAndNegativeCheck(userId, "userId");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.USER_NOT_FOUND));

        // status를 APPROVED로 변경
        user.changeStatus(ApprovalStatus.APPROVED);

        // ROLE_USER 권한 부여 (이미 권한이 있으면 추가하지 않음)
        Authority userRole = authorityRepository.findByAuthorityName("ROLE_USER")
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.USER_ROLE_NOT_FOUND));

        user.addAuthority(userRole);

        return AdminUserDetailResponseDTO.from(user);
    }

    /**
     * 사용자 거부
     * - status를 REJECTED로 변경
     */
    @Transactional
    public AdminUserDetailResponseDTO rejectUser(Long userId) {
        CheckConfig.npeAndNegativeCheck(userId, "userId");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.USER_NOT_FOUND));

        // status를 REJECTED로 변경
        user.changeStatus(ApprovalStatus.REJECTED);

        return AdminUserDetailResponseDTO.from(user);
    }

    /**
     * 서버에 등록된 권한 목록 조회
     */
    public List<String> findAllRoleNames() {
        return authorityRepository.findAll().stream()
                .map(Authority::getAuthorityName)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    /**
     * 사용자에게 권한 1개를 추가 부여한다. (기존 권한은 유지)
     */
    @Transactional
    public AdminUserDetailResponseDTO grantRole(Long id, String roleName) {
        CheckConfig.npeAndNegativeCheck(id, "id");
        CheckConfig.npeAndBlankCheck(roleName, "roleName");

        // 1. 권한 이름 정규화 (ROLE_ 접두사 처리)
        String normalized = roleName.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        Authority authority = authorityRepository.findByAuthorityName(normalized)
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.AUTHORITY_NOT_EXIST));

        // 2. 사용자 및 권한 존재 여부 확인
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.USER_NOT_FOUND));

        // 3. 권한 추가(중복 체크가 내부 메서드에 구현되어 있음)
        user.addAuthority(authority);

        return AdminUserDetailResponseDTO.from(user);
    }

    /**
     * 사용자에게서 권한 1개를 제거한다.
     */
    @Transactional
    public AdminUserDetailResponseDTO revokeRole(Long userId, String roleName) {
        CheckConfig.npeAndNegativeCheck(userId, "userId");
        CheckConfig.npeAndBlankCheck(roleName, "roleName");

        String normalized = roleName.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        Authority authority = authorityRepository.findByAuthorityName(normalized)
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.AUTHORITY_NOT_EXIST));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(AdminUserErrorCode.USER_NOT_FOUND));

        user.removeAuthority(authority);

        return AdminUserDetailResponseDTO.from(user);
    }

    public List<UserSuggestionResponseDTO> findUserSuggestions(){
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> UserSuggestionResponseDTO.builder()
                        .studentNum(user.getStudentNum())
                        .name(user.getName())
                        .build())
                .toList();
    }
}
