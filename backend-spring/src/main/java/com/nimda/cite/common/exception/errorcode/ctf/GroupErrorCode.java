package com.nimda.cite.common.exception.errorcode.ctf;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GroupErrorCode implements ErrorCode {
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GR001", "존재하지 않는 그룹입니다."),
    GROUP_NAME_DUPLICATED(HttpStatus.CONFLICT, "GR002", "이미 사용 중인 그룹 이름입니다."),
    GROUP_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "GR003", "그룹 멤버가 아닙니다."),
    ALREADY_GROUP_MEMBER(HttpStatus.CONFLICT, "GR004", "이미 그룹에 가입되어 있습니다."),
    GROUP_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "GR005", "그룹 정원이 초과되었습니다."),
    GROUP_LEADER_ONLY(HttpStatus.FORBIDDEN, "GR006", "그룹장만 수행할 수 있습니다."),
    LEADER_TRANSFER_FORBIDDEN(HttpStatus.FORBIDDEN, "GR007", "그룹장만 위임할 수 있습니다."),
    LEADER_TRANSFER_TO_SELF(HttpStatus.BAD_REQUEST, "GR008", "본인에게 위임할 수 없습니다."),
    LEADER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "GR009", "그룹장은 위임 후 탈퇴할 수 있습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "GR010", "본인을 강퇴할 수 없습니다."),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "GR011", "존재하지 않는 요청입니다."),
    JOIN_REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "GR012", "이미 처리된 요청입니다."),
    JOIN_REQUEST_ALREADY_PENDING(HttpStatus.CONFLICT, "GR013", "이미 진행 중인 요청이 존재합니다."),
    JOIN_REQUEST_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "GR014", "본인의 요청만 취소할 수 있습니다."),
    JOIN_REQUEST_APPLY_LEADER_ONLY(HttpStatus.FORBIDDEN, "GR015", "그룹장만 신청을 수락/거절할 수 있습니다."),
    JOIN_REQUEST_INVITEE_ONLY(HttpStatus.FORBIDDEN, "GR016", "초대받은 본인만 수락/거절할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String errorNum;
    private final String message;
}
