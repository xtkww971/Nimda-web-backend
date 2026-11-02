package com.nimda.cite.user.controller;

import com.nimda.cite.common.exception.UnauthorizedException;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.TokenProvider;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.dto.changePassword.ChangePasswordRequest;
import com.nimda.cite.user.dto.changePassword.CheckAuthCodeRequest;
import com.nimda.cite.user.dto.changePassword.CheckUserValidateRequest;
import com.nimda.cite.user.dto.changePassword.CheckUserValidateResponse;
import com.nimda.cite.user.enums.MailSendResult;
import com.nimda.cite.user.service.AuthService;
import com.nimda.cite.user.service.UserRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UrlNames.passwordChangeUrl)
@RequiredArgsConstructor
public class UserRecoveryController {
    private final UserRecoveryService userRecoveryService;
    private final AuthService authService;
    private final TokenProvider tokenProvider;

    // 비밀번호 변경 시 유저 정보 확인 API
    @PostMapping(UrlNames.checkUserInfoUrl)
    public ResponseEntity<ApiResponse<CheckUserValidateResponse>> checkUserInfo(@RequestBody CheckUserValidateRequest req) {
        CheckUserValidateResponse dto =
                userRecoveryService.checkValidate(req.getUserId(),req.getStudentNum(),req.getEmail());

        if(dto.isValidateEmail() && dto.isValidateUserId() && dto.isValidateStudentNum()) {
            String passwordToken =
                tokenProvider.createTokenForPasswordChange(req.getUserId(), req.getStudentNum(), req.getEmail(),false);

            ResponseCookie cookie = createPasswordChangeCookie(passwordToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(dto));
        }
        // 프론트에서 어떤 정보가 잘못되었는지 표기
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    // 메일 전송 버튼과 연결됨
    // body와 쿠키를 받기 위해 Post로 해야함
    // 응답 본문은 항상 HTTP 200 + "OK" | "UNAUTHORIZED" | "LIMIT_EXCEEDED" 문자열 중 하나 (프론트 userRecovery.ts 계약)
    @PostMapping(UrlNames.sendAuthMailUrl)
    public ResponseEntity<String> sendMail(@RequestBody CheckUserValidateRequest req,
                                           @CookieValue(name = "password_change_token") String token) {
        MailSendResult result = userRecoveryService.sendAuthMail(req, token);

        // OK면 인증 화면으로 이동
        return ResponseEntity.ok(result.name());
    }

    // 인증 버튼과 연결
    // 응답 본문은 문자열 (성공: "인증이 완료되었습니다.", 실패: 403/401 + 에러 메시지 문자열)
    @PostMapping(UrlNames.checkAuthCodeUrl)
    public ResponseEntity<String> checkAuthCode(@RequestBody CheckAuthCodeRequest req,
                                                @CookieValue(name = "password_change_token") String token) {
        String emailVerifiedToken = userRecoveryService.verifyAuthCode(token, req.getAuthCode());

        // 인증 완료 토큰 재발급
        ResponseCookie cookie = createPasswordChangeCookie(emailVerifiedToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("인증이 완료되었습니다.");
    }

    // 응답 본문은 문자열 (성공: "비밀번호가 재설정되었습니다.", 실패: 403 + 에러 메시지 문자열)
    @PostMapping(UrlNames.changePasswordUrl)
    public ResponseEntity<String> changePassword(@CookieValue(name = "password_change_token") String token,
                                                 @RequestBody ChangePasswordRequest req) {
        String userId = userRecoveryService.validatePasswordChangeToken(token);
        authService.changePassword(userId, req.getPassword());
        return ResponseEntity.ok("비밀번호가 재설정되었습니다.");
    }

    /**
     * 비밀번호 재설정 인증 실패(세션 만료, 인증 코드 불일치, 이메일 미인증)는
     * 프론트 계약에 맞춰 ApiResponse가 아닌 에러 메시지 문자열로 응답한다. (상태 코드는 ErrorCode 기준)
     * - 컨트롤러 내부 @ExceptionHandler라 GlobalExceptionHandler보다 우선 적용되고, 이 컨트롤러에만 영향을 준다.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleRecoveryUnauthorized(UnauthorizedException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(e.getMessage());
    }

    private ResponseCookie createPasswordChangeCookie(String token) {
        return ResponseCookie.from("password_change_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/api/cite/passwordChange")
                .maxAge(5 * 60) // 5분
                .sameSite("Strict")
                .build();
    }
}
