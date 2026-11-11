package com.nimda.cite.common.exception;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * handleExceptionInternal : dispatcher servlet이 던지는 예외를 각 예외별 처리를 해주는 ResponseEntityExceptionHandler 내부 오버라이딩 메서드
 * handleMethodArgumentNotValid : @Valid check를 통해 던지는 오류 처리 오버라이딩 메서드
 *
 * 모든 에러 응답은 ApiResponse.fail(message) 형식으로 통일한다.
 * - 비즈니스 예외 : GlobalNimdaException(ErrorCode) → ErrorCode의 status / message 사용
 * - 예상하지 못한 예외 : 내부 메시지를 노출하지 않고 ErrorCode.INTERNAL_SERVER_ERROR 메시지로 응답
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.warn("Spring MVC Exception : {} - {}", statusCode.value(), ex.getMessage());

        ErrorCode errorCode = statusCode.is5xxServerError()
                ? ErrorCode.INTERNAL_SERVER_ERROR
                : ErrorCode.INVALID_INPUT;

        return ResponseEntity
                .status(statusCode)
                .headers(headers)
                .body(ApiResponse.fail(errorCode.getMessage()));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        log.warn("Nimda Validation Exception : {} - {}", ErrorCode.INVALID_INPUT.getErrorNum(), message);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.fail(message));
    }

    @ExceptionHandler(GlobalNimdaException.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalNimdaException(GlobalNimdaException e){
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Nimda Global Exception : {} - {}", errorCode.getErrorNum(), e.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(e.getMessage()));
    }

    /**
     * @PreAuthorize 등 메서드 보안에서 던지는 권한 예외 (Exception 핸들러에 먹혀 500이 되지 않도록 별도 처리)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e){
        log.warn("Nimda Access Denied : {} - {}", ErrorCode.ACCESS_DENIED.getErrorNum(), e.getMessage());

        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e){
        log.warn("Nimda Authentication Exception : {} - {}", ErrorCode.CANNOT_AUTHORIZED.getErrorNum(), e.getMessage());

        return ResponseEntity
                .status(ErrorCode.CANNOT_AUTHORIZED.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.CANNOT_AUTHORIZED.getMessage()));
    }

    /**
     * CheckConfig 등 입력값 검증에서 던지는 예외
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e){
        log.warn("Nimda Invalid Input : {} - {}", ErrorCode.INVALID_INPUT.getErrorNum(), e.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e){
        log.error("Unhandled Exception : ", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
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
