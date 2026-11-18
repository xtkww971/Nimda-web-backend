package com.nimda.ctfgame.exception;

import java.util.Map;
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
        CHALLENGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "연결할 문제를 찾을 수 없습니다."),
        // ── 문제 쪽 연결 API (docs/api/CTF 게임 물건 연결 API.md) ──
        /** 경로의 문제 코드가 없다 (연결 API 는 경로 자원이라 404) */
        LINK_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "연결할 문제를 찾을 수 없습니다."),
        INVALID_KEY(HttpStatus.BAD_REQUEST, "key 는 영문 소문자·숫자·- 로 50자 이하"),
        NAME_REQUIRED(HttpStatus.BAD_REQUEST, "새 물건은 이름이 필요합니다."),
        NOT_LINKED(HttpStatus.NOT_FOUND, "이 문제에 연결된 물건이 아닙니다."),
        /** data: {challengeCode: 지금 연결된 문제} */
        LINKED_ELSEWHERE(HttpStatus.CONFLICT, "이미 다른 문제에 연결된 물건입니다.");

        final HttpStatus status;
        final String message;

        Kind(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private final Kind kind;
    private final Map<String, Object> data;

    public GameItemException(Kind kind) {
        this(kind, null);
    }

    /** data 는 실패 응답의 data 로 나간다 (예: 409 에서 어느 문제에 연결돼 있는지) */
    public GameItemException(Kind kind, Map<String, Object> data) {
        super(kind.message);
        this.kind = kind;
        this.data = data;
    }

    public Kind kind() {
        return kind;
    }

    public HttpStatus status() {
        return kind.status;
    }

    public Map<String, Object> data() {
        return data;
    }
}
