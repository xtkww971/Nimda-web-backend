package com.nimda.cite.user.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.MailService;
import com.nimda.cite.common.util.TokenProvider;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.dto.LoginRequestDTO;
import com.nimda.cite.user.dto.LoginResponseDTO;
import com.nimda.cite.user.dto.LoginSuccessResponseDTO;
import com.nimda.cite.user.dto.MyPageResponseDTO;
import com.nimda.cite.user.dto.profile.EmailHideUpdateResponseDTO;
import com.nimda.cite.user.dto.profile.ProfileDecorationUpdateRequestDTO;
import com.nimda.cite.user.dto.profile.ProfileDecorationUpdateResponseDTO;
import com.nimda.cite.user.dto.profile.ProfileImageUpdateRequestDTO;
import com.nimda.cite.user.dto.profile.ProfileImageUpdateResponseDTO;
import com.nimda.cite.user.dto.register.RegisterRequestDTO;
import com.nimda.cite.user.dto.register.RegisterResponseDTO;
import com.nimda.cite.user.dto.UpdateProfileDTO;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.security.CustomUserDetails;
import com.nimda.cite.user.service.AuthService;
import com.nimda.cite.common.s3.S3Service;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UrlNames.authUrl)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MailService mailService;
    private final S3Service s3Service;
    private final TokenProvider tokenProvider;

    @Value("${auth.cookie.secure:true}")
    private boolean secureAuthCookie;

    /**
     * 인증 쿠키를 보낼 도메인. 비우면 지금까지처럼 호스트 전용 쿠키다(그 호스트에만 전송).
     *
     * <p>CTF 문제 인스턴스는 {@code inst-{id}.도메인} 서브도메인으로 접속한다(ADR-0011).
     * 서브도메인 프록시가 소유자를 확인하려면 그 호스트에도 인증 쿠키가 가야 하므로,
     * 그때만 상위 도메인(예: nimda.kr, 로컬은 127.0.0.1.nip.io)을 지정한다.
     */
    @Value("${auth.cookie.domain:}")
    private String authCookieDomain;

    /**
     * 로그인
     *
     * @param loginRequest 로그인 요청 데이터
     * @return JWT 토큰과 사용자 정보
     */
    @PostMapping(UrlNames.loginUrl)
    public ResponseEntity<LoginSuccessResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) { // HttpServletResponse 추가
        User user = authService.validateUser(
                loginRequest.getUserId(),
                loginRequest.getPassword());

        LoginResponseDTO loginData = authService.login(user);
        String token = loginData.getAccessToken(); // DTO에서 토큰 추출

        ResponseCookie cookie = createAuthCookie(token, 60 * 60 * 24);

        // 2. 응답 헤더에 쿠키 추가 및 바디 응답 (토큰은 쿠키로만 전달하고, 바디에는 user만 내려간다)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(LoginSuccessResponseDTO.of(
                        "로그인 성공",
                        loginData.getUser()
                ));
    }

    /**
     * 로그아웃
     * 브라우저에 저장된 Authorization 쿠키를 만료시킨다.
     */
    @PostMapping(UrlNames.logoutUrl)
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie cookie = createAuthCookie("", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(
                        "로그아웃 성공"
                ));
    }

    //refactoring target : controller layer -> service layer
    private ResponseCookie createAuthCookie(String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("Authorization", value)
                .httpOnly(true)
                .secure(secureAuthCookie)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax");

        // 도메인을 지정하면 서브도메인까지 쿠키가 간다. 필요할 때만 켠다.
        if (authCookieDomain != null && !authCookieDomain.isBlank()) {
            builder.domain(authCookieDomain);
        }
        return builder.build();
    }

    /**
     * 회원가입
     * Request Data : Register DTO (userId, name, nickname, password, studentNum,
     * email, major)
     */
    @PostMapping(UrlNames.registerUrl)
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        RegisterResponseDTO response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Note. MyPageAPI - 현재 로그인한 사용자 정보를 JWT에서 추출한다.
    @GetMapping(UrlNames.getCurrentUserUrl)
    public ResponseEntity<MyPageResponseDTO> getCurrentUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // CustomUserDetails에서 User 엔터티 추출 (인증되지 않은 경우 401)
        User user = CustomUserDetails.getLoginUser(customUserDetails);

        // profileImage가 S3 키라면 Presigned GET URL로 변환
        String profileImageUrl = user.getProfileImage();
        if (s3Service != null && profileImageUrl != null && !profileImageUrl.isBlank()
                && !profileImageUrl.startsWith("http")) {
            profileImageUrl = s3Service.createPresignedGetUrl(profileImageUrl, 60);
        }

        // DTO로 변환 (민감 정보 제외)
        MyPageResponseDTO response = MyPageResponseDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .major(user.getMajor())
                .bojId(user.getBojId())
                .birth(user.getBirth())
                .studentNum(user.getStudentNum())
                .profileImage(profileImageUrl)
                .profileDecoration(user.getProfileDecoration())
                .emailHide(user.isEmailHide())
                .roles(user.getAuthorities().stream()
                        .map(authority -> authority.getAuthorityName())
                        .toList())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping(UrlNames.updateProfileUrl)
    public ResponseEntity<ApiResponse<MyPageResponseDTO>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateProfileDTO request) {
        User user = CustomUserDetails.getLoginUser(customUserDetails);
        User updated = authService.updateProfile(
                user.getId(),
                request.getNickname(),
                request.getBojId(),
                request.getBirth(),
                request.getMajor(),
                request.getStudentNum()
        );

        MyPageResponseDTO response = MyPageResponseDTO.builder()
                .userId(updated.getUserId())
                .name(updated.getName())
                .nickname(updated.getNickname())
                .email(updated.getEmail())
                .major(updated.getMajor())
                .bojId(updated.getBojId())
                .birth(updated.getBirth())
                .studentNum(updated.getStudentNum())
                .profileImage(updated.getProfileImage())
                .profileDecoration(updated.getProfileDecoration())
                .emailHide(updated.isEmailHide())
                .build();

        return ApiResponse.ok("프로필이 성공적으로 수정되었습니다.", response).toResponse();
    }

    /**
     * 프로필 이미지 변경
     * 클라이언트가 S3에 업로드 완료 후 S3 키를 전달하면 User.profileImage에 저장
     */
    @PutMapping(UrlNames.updateProfileImageUrl)
    public ResponseEntity<ProfileImageUpdateResponseDTO> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody ProfileImageUpdateRequestDTO request) {
        User user = CustomUserDetails.getLoginUser(customUserDetails);
        User updated = authService.updateProfileImage(user.getId(), request.getProfileImageKey());

        // Presigned GET URL 생성 (60분 유효)
        String imageUrl = null;
        if (s3Service != null && updated.getProfileImage() != null) {
            imageUrl = s3Service.createPresignedGetUrl(updated.getProfileImage(), 60);
        }

        return ResponseEntity.ok(ProfileImageUpdateResponseDTO.of(
                "프로필 이미지가 변경되었습니다.",
                imageUrl != null ? imageUrl : "",
                updated.getProfileImage()));
    }

    /**
     * 프로필 장식 변경
     */
    @PutMapping(UrlNames.updateProfileDecorationUrl)
    public ResponseEntity<ProfileDecorationUpdateResponseDTO> updateProfileDecoration(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody ProfileDecorationUpdateRequestDTO request) {
        User user = CustomUserDetails.getLoginUser(customUserDetails);
        User updated = authService.updateProfileDecoration(user.getId(), request.getProfileDecorationKey());

        return ResponseEntity.ok(ProfileDecorationUpdateResponseDTO.of(
                "프로필 장식이 변경되었습니다.",
                updated.getProfileDecoration() == null ? "" : updated.getProfileDecoration()));
    }

    @PostMapping(UrlNames.emailHideUrl)
    public ResponseEntity<EmailHideUpdateResponseDTO> emailHide(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // CustomUserDetails에서 User 엔터티 추출 (인증되지 않은 경우 401)
        User user = CustomUserDetails.getLoginUser(customUserDetails);
        boolean emailHide = authService.toggleEmailHide(user.getId());

        return ResponseEntity.ok(EmailHideUpdateResponseDTO.of(
                "이메일 숨김 설정이 변경되었습니다.",
                emailHide));
    }
}
