package judgeServer.domain.group.enums;

/**
 * groups 테이블은 스터디 그룹과 CTF 팀이 함께 쓴다.
 * 스터디 그룹 API(/api/groups)는 STUDY만, CTF 팀 API와 대회 쿼리는 CTF_TEAM만 다룬다.
 */
public enum GroupType {
    /** 스터디 그룹. 생성한 관리자가 그룹장이 되고 가입 신청·초대로 멤버가 바뀐다. */
    STUDY,
    /** CTF 팀. 운영자가 팀 빌딩으로 만들고 대회(contest_id)에 묶인다. */
    CTF_TEAM
}
