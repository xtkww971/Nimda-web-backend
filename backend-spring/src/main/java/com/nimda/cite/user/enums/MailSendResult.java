package com.nimda.cite.user.enums;

/**
 * 비밀번호 찾기 인증 메일 발송 결과
 * - 프론트(userRecovery.ts)는 send-authMail 응답 본문을 이 이름 문자열로 구분한다. (항상 HTTP 200)
 */
public enum MailSendResult {
    OK,
    UNAUTHORIZED,
    LIMIT_EXCEEDED
}
