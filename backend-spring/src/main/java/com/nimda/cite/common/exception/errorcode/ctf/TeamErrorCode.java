package com.nimda.cite.common.exception.errorcode.ctf;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements ErrorCode {
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "TM001", "존재하지 않는 팀입니다."),
    TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TM002", "해당 유저는 이 팀의 멤버가 아닙니다."),
    TEAM_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "TM003", "존재하지 않는 유저입니다."),
    TEAM_USER_NOT_APPROVED(HttpStatus.BAD_REQUEST, "TM004", "승인되지 않은 유저는 팀에 넣을 수 없습니다."),
    TEAM_USER_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "TM005", "이미 다른 팀에 속한 유저가 있습니다."),
    TEAM_NAME_DUPLICATED(HttpStatus.CONFLICT, "TM006", "이미 사용 중인 팀 이름입니다."),
    TEAM_MEMBERS_OVER_CAPACITY(HttpStatus.BAD_REQUEST, "TM007", "멤버 수가 최대 인원을 넘습니다."),
    TEAM_CAPACITY_BELOW_MEMBERS(HttpStatus.BAD_REQUEST, "TM008", "현재 멤버 수보다 최대 인원을 작게 할 수 없습니다."),
    TEAM_FULL(HttpStatus.BAD_REQUEST, "TM009", "팀 정원을 넘습니다."),
    NO_TEAM(HttpStatus.FORBIDDEN, "TM010", "팀에 소속되어 있지 않습니다. 운영진이 팀을 배정하면 문제가 공개됩니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
