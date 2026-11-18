package com.nimda.ctfgame.dto;

/**
 * 게임이 받는 물건 하나. 게임은 zone·x·y 에 물건을 그리고, 상호작용하면 challengeCode 로 문제 팝업(GET /api/ctf/challenges/{code})을 연다.
 *
 * @param challengeCode 연결 문제 코드. 문제가 없거나 아직 공개 전이면 null (관리자·DEV 에게는 공개 전 문제도 보낸다)
 * @param owned         팀이 연결 문제를 풀었는지
 * @param description   이야기. owned 일 때만 값이 있고, 아니면 null (내용을 미리 보지 못하게 서버가 보내지 않는다)
 */
public record GameItemResponse(
        String key,
        String name,
        int zone,
        int x,
        int y,
        int sortOrder,
        String challengeCode,
        String challengeTitle,
        boolean owned,
        String description) {
}
