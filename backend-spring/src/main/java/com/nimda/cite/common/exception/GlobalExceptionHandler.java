package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.error.VersionMismatchException;
import com.nimda.cite.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(VersionMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleVersionMismatch(VersionMismatchException ex) {

        // 로그 출력
        System.out.println("[WARN] " + ex.getMessage());

        // [유저 응답 부품 만들기]
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.clear();
        responseBody.put("error", "VERSION_MISMATCH");
        responseBody.put("message", ex.getMessage());

        // 상태 코드 출력
        return ResponseEntity
                .status(HttpStatus.UPGRADE_REQUIRED)
                .body(responseBody);
    }

    /**
     * ResponseStatusException을 ApiResponse 포맷으로 변환한다.
     * judgeServer 컨트롤러는 에러를 ResponseStatusException으로 던지는데, 그대로 두면
     * 스프링 기본 에러 바디로 나가서 {success, message, data} 공통 응답과 어긋난다.
     * 던진 상태 코드는 유지하고 reason을 message로 돌려준다.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ApiResponse.<Object>fail(message).toResponse(status);
    }
}
