package com.nimda.ctfgame.exception;

import org.springframework.http.HttpStatus;

/**
 * 게임 물건 API 의 실패. 공용 ErrorCode 를 늘리지 않으려고(이벤트가 끝나면 이 패키지만 지우면 되게) 따로 두고,
 * {@link GameItemExceptionHandler} 가 ApiResponse 실패 모양으로 바꾼다.
 */
public class GameItemException extends RuntimeException {

    public enum Kind {
        NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게임 물건입니다."),
        DUPLICATE_KEY(HttpStatus.CONFLICT, "이미 있는 key 입니다."),
        KEY_CHANGED(HttpStatus.BAD_REQUEST, "key 는 바꿀 수 없습니다. 지우고 다시 만들어 주세요."),
        CHALLENGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "연결할 문제를 찾을 수 없습니다.");

        final HttpStatus status;
        final String message;

        Kind(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private final Kind kind;

    public GameItemException(Kind kind) {
        super(kind.message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public HttpStatus status() {
        return kind.status;
    }
}
