package com.nimda.ctfgame.exception;

import com.nimda.cite.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** com.nimda.ctfgame 의 컨트롤러에만 적용된다. 공용 GlobalExceptionHandler 보다 먼저 본다. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.nimda.ctfgame")
public class GameItemExceptionHandler {

    @ExceptionHandler(GameItemException.class)
    public ResponseEntity<ApiResponse<Void>> handle(GameItemException e) {
        return ApiResponse.<Void>fail(e.getMessage()).toResponse(e.status());
    }
}
