package com.nimda.ctfgame.exception;

import com.nimda.cite.common.response.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
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
    public ResponseEntity<?> handle(GameItemException e) {
        if (e.data() == null) {
            return ApiResponse.<Void>fail(e.getMessage()).toResponse(e.status());
        }
        // ApiResponse.fail 은 data 를 받지 않아 같은 모양({success, message, data})으로 직접 만든다
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", e.getMessage());
        body.put("data", e.data());
        return ResponseEntity.status(e.status()).body(body);
    }
}
