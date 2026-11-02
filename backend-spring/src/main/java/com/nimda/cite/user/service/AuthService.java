package com.nimda.cite.user.service;

import com.nimda.cite.common.exception.AlreadyExistException;
import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.NotIncludeRoleException;
import com.nimda.cite.common.exception.UnauthorizedException;
import com.nimda.cite.common.exception.errorcode.domain.ProfileDecorationErrorCode;
import com.nimda.cite.common.exception.errorcode.user.AuthErrorCode;
import com.nimda.cite.common.exception.errorcode.user.UserErrorCode;
import com.nimda.cite.config.CheckConfig;
import com.nimda.cite.user.dto.register.RegisterRequestDTO;
import com.nimda.cite.user.dto.register.RegisterResponseDTO;
import lombok.RequiredArgsConstructor;
import redis.util.RedisUtil;
import com.nimda.cite.user.dto.LoginResponseDTO;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.enums.ApprovalStatus;
import com.nimda.cite.common.exception.UserNotApprovedException;
import com.nimda.cite.domain.profiledecoration.entity.ProfileDecoration;
import com.nimda.cite.domain.profiledecoration.repository.ProfileDecorationRepository;
import com.nimda.cite.domain.profiledecoration.ownership.ProfileDecorationOwnershipService;
import com.nimda.cite.common.util.JwtUtil;
import com.nimda.cite.user.repository.UserRepository;
import com.nimda.cite.user.security.CustomUserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserService userService; // service 내부에서 service를 부르는 무한 순환 참조 문제 대상

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final ProfileDecorationRepository profileDecorationRepository;

    private final ProfileDecorationOwnershipService profileDecorationOwnershipService;
    private final RedisUtil redisUtil;

    /**
     * 사용자 인증
     *
     * @param userId   사용자 ID
     * @param password 비밀번호
     * @return 인증된 사용자
     * @throws UnauthorizedException    아이디가 없거나 비밀번호가 틀린 경우 (계정 존재 여부를 노출하지 않도록 동일한 에러 사용)
     * @throws UserNotApprovedException 승인되지 않은 사용자인 경우
     */
    public User validateUser(String userId, String password) {
        CheckConfig.npeAndBlankCheck(userId, "userId");
        CheckConfig.npeAndBlankCheck(password, "password");

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.INVALID_CREDENTIALS));

        // 1. 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 승인 상태 확인
        // Note. 유저의 현재 상태에 따른 커스텀 예회를 반환한다.
        if (user.getStatus() != ApprovalStatus.APPROVED) {
            if (user.getStatus() == ApprovalStatus.PENDING) {
                throw new UserNotApprovedException(AuthErrorCode.STATUS_PENDING);
            } else if (user.getStatus() == ApprovalStatus.REJECTED) {
                throw new UserNotApprovedException(AuthErrorCode.STATUS_REJECTED);
            } else {
                throw new UserNotApprovedException(AuthErrorCode.STATUS_NOT_APPROVED);
            }
        }

        // 3. 인증 성공
        return user;
    }

    /* 로그인 처리 */

    @Transactional(readOnly = true)
    public LoginResponseDTO login(User user) {
        // 권한 정보를 포함한 전체 User 객체 조회 (@EntityGraph로 권한 정보 함께 로드)
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException(AuthErrorCode.USER_NOT_FOUND));

        CustomUserDetails userDetails = new CustomUserDetails(fullUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        eventPublisher.publishEvent(new AuthenticationSuccessEvent(authentication));

        // 사용자의 권한 목록 추출 (@EntityGraph로 이미 로드됨)
        java.util.List<String> authorities = fullUser.getAuthorities().stream()
                .map(authority -> authority.getAuthorityName())
                .collect(java.util.stream.Collectors.toList());

        // 디버깅용 로그
        System.out.println("[AuthService] User: " + fullUser.getNickname() + " (ID: " + fullUser.getId() + ")");
        System.out.println("[AuthService] Authority count: " + authorities.size());
        System.out.println("[AuthService] Authorities: " + authorities);

        String token = jwtUtil.generateToken(fullUser.getNickname(), fullUser.getId(), authorities); // JWT 토큰 생성

        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .id(fullUser.getId())
                .userId(fullUser.getUserId())
                .nickname(fullUser.getNickname())
                .email(fullUser.getEmail())
                .profileImage(fullUser.getProfileImage())
                .profileDecoration(fullUser.getProfileDecoration())
                .roles(authorities)
                .build();

        return LoginResponseDTO.builder()
                .accessToken(token)
                .user(userInfo)
                .build();
    }

    /**
     * 회원가입 처리 (UserService에 위임)
     *
     * 이것도 굉장히 위태위태 하다. 회원가입을 userService에서 의존해야 된다는 사실은 인지했는데, 이걸 왜 Auth쪽에서 가져가는거지?
     * Auth는 Authentication, 즉 인증을 위한 기능들을 넣는 것인데, 회원가입 자체만으로 인증을 한다고 볼 수 있나?
     */
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO registerRequest) {
        return userService.createUser(registerRequest);
    }

    @Transactional
    public boolean toggleEmailHide(Long userId) {
        // 1. 유저 조회 (없으면 예외 발생)
        User user = findUser(userId);

        // 2. 상태 반전 (Dirty Checking에 의해 메서드 종료 시 자동 UPDATE)
        return user.toggleEmailHide();
    }

    /**
     * 프로필 이미지 변경 (S3 키 저장)
     */
    @Transactional
    public User updateProfileImage(Long userId, String profileImageKey) {
        if (profileImageKey == null || profileImageKey.isBlank()) {
            throw new NotAllowFormException(UserErrorCode.PROFILE_IMAGE_KEY_REQUIRED);
        }

        User user = findUser(userId);
        user.changeProfileImage(profileImageKey);
        return user;
    }

    /**
     * 프로필 장식 변경
     */
    @Transactional
    public User updateProfileDecoration(Long userId, String profileDecorationKey) {
        User user = findUser(userId);
        if (profileDecorationKey == null || profileDecorationKey.isBlank()) {
            user.changeProfileDecoration(null);
            return user;
        }

        String decorationKey = profileDecorationKey.trim();
        ProfileDecoration decoration = profileDecorationRepository.findByKey(decorationKey)
                .orElseThrow(() -> new NotFoundException(ProfileDecorationErrorCode.DECORATION_NOT_FOUND));
        if (!decoration.isActive()) {
            throw new NotAllowFormException(ProfileDecorationErrorCode.DECORATION_NOT_USABLE);
        }

        if (!profileDecorationOwnershipService.owns(userId, decoration.getId())) {
            throw new NotIncludeRoleException(ProfileDecorationErrorCode.DECORATION_NOT_OWNED);
        }

        user.changeProfileDecoration(decorationKey);
        return user;
    }

    /**
     * 프로필 정보 수정 (닉네임, 백준 ID, 생년월일, 학과, 학번)
     * null인 필드는 수정하지 않음 (부분 업데이트)
     */
    @Transactional
    public User updateProfile(Long userId, String nickname, String bojId, String birth, String major, String studentNum) {
        User user = findUser(userId);

        // 닉네임 변경 시 중복 체크
        if (nickname != null && !nickname.isBlank() && !nickname.equals(user.getNickname())
                && userRepository.existsByNickname(nickname)) {
            throw new AlreadyExistException(UserErrorCode.NICKNAME_ALREADY_EXIST);
        }

        // Dirty Checking에 의해 자동 UPDATE
        user.updateProfile(nickname, bojId, birth, major, studentNum);
        return user;
    }

    @Transactional
    public void changePassword(String userId, String password) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(AuthErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(password);
        user.changePassword(encodedPassword);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(AuthErrorCode.USER_NOT_FOUND));
    }
}
