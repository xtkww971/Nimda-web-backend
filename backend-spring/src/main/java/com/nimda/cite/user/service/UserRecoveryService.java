package com.nimda.cite.user.service;

import com.nimda.cite.common.exception.UnauthorizedException;
import com.nimda.cite.common.exception.errorcode.user.UserRecoveryErrorCode;
import com.nimda.cite.common.util.JwtUtil;
import com.nimda.cite.common.util.MailLimitExceededException;
import com.nimda.cite.common.util.MailService;
import com.nimda.cite.common.util.TokenProvider;
import com.nimda.cite.config.CheckConfig;
import com.nimda.cite.user.dto.changePassword.CheckUserValidateRequest;
import com.nimda.cite.user.dto.changePassword.CheckUserValidateResponse;
import com.nimda.cite.user.enums.MailSendResult;
import com.nimda.cite.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRecoveryService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private MailService mailService;

    // 컨트롤러에서 DTO화해서 반환
    @Transactional
    public CheckUserValidateResponse checkValidate(String userId, String studentNum, String email) {
        CheckConfig.npeAndBlankCheck(userId, "userId");
        CheckConfig.npeAndBlankCheck(studentNum, "studentNum");
        CheckConfig.npeAndBlankCheck(email, "email");

        boolean idExists = userRepository.existsByUserId(userId);

        // 2. 아이디가 없으면 모두 false로 반환
        if (!idExists) {
            return new CheckUserValidateResponse(false, false, false);
        }
        return new CheckUserValidateResponse(
                true,
                userRepository.existsByUserIdAndStudentNum(userId, studentNum),
                userRepository.existsByUserIdAndEmail(userId, email)
        );
    }

    // 유저 정보 확인 토큰을 검증한 뒤 인증 메일 전송
    // 프론트 계약상 실패도 예외가 아니라 결과값으로 돌려준다. (send-authMail은 항상 HTTP 200)
    public MailSendResult sendAuthMail(CheckUserValidateRequest req, String token) {
        boolean isVerified = jwtUtil.validateToken(
                token, req.getUserId(), req.getStudentNum(), req.getEmail()
        );
        if (!isVerified) {
            return MailSendResult.UNAUTHORIZED;
        }

        try {
            mailService.sendAuthCode(req.getEmail());
            return MailSendResult.OK;
        } catch (MailLimitExceededException e) {
            // 하루 발송 횟수 제한 초과 (세션 만료와 구분되는 원인)
            return MailSendResult.LIMIT_EXCEEDED;
        }
    }

    // 인증 코드 확인 후, 이메일 인증이 완료된 토큰을 재발급한다.
    public String verifyAuthCode(String token, String authCode) {
        // 1. 토큰이 유효한지 먼저 체크하고, 토큰 내부에 저장된 이메일을 꺼냅니다.
        if (!tokenProvider.validateToken(token)) {
            throw new UnauthorizedException(UserRecoveryErrorCode.RECOVERY_SESSION_EXPIRED);
        }

        String emailFromToken = jwtUtil.extractClaimByKey(token, "email");
        if (!mailService.verifyCode(emailFromToken, authCode)) {
            throw new UnauthorizedException(UserRecoveryErrorCode.AUTH_CODE_MISMATCH);
        }

        String userId = jwtUtil.extractClaimByKey(token, "userId");
        String studentNum = jwtUtil.extractClaimByKey(token, "studentNum");
        return tokenProvider.createTokenForPasswordChange(userId, studentNum, emailFromToken, true);
    }

    // 비밀번호 변경 토큰을 검증하고, 토큰에 담긴 userId를 반환한다.
    public String validatePasswordChangeToken(String token) {
        if (!tokenProvider.validateToken(token)) {
            throw new UnauthorizedException(UserRecoveryErrorCode.RECOVERY_SESSION_EXPIRED);
        }

        Boolean isEmailVerified =
                jwtUtil.extractClaim(token, claims -> claims.get("isEmailVerified", Boolean.class));
        if (isEmailVerified == null || !isEmailVerified) {
            // 인증하지 않고 우회한 경우
            throw new UnauthorizedException(UserRecoveryErrorCode.EMAIL_NOT_VERIFIED);
        }

        return jwtUtil.extractId(token);
    }
}
