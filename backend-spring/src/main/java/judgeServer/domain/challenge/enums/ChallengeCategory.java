package judgeServer.domain.challenge.enums;

/**
 * 문제 분류. Judge Server는 이 값 하나로 무엇을 할지 정한다(provisioner task.go의 instanceCategories).
 *
 * <pre>
 *   WEB, PWN                            인스턴스를 띄운다 (WEB은 HTTP 프록시, PWN은 TCP 포트)
 *   REVERSING, FORENSICS, CRYPTO, MISC  첨부파일 다운로드 링크만 발급한다
 * </pre>
 *
 * 다운로드 요청과 인스턴스 요청은 같은 모양으로 나가므로, 다운로드 API가 WEB·PWN 문제로 발행하면
 * Judge는 컨테이너를 만든다. 경로마다 {@link #usesInstance()}로 먼저 거른다.
 */
public enum ChallengeCategory {
    // FlagType / IsolationType
    // STATIC, NONE
    REVERSING,
    // STATIC, NONE
    FORENSICS,
    // STATIC, NONE
    CRYPTO,

    // STATIC(SHARED·PER_USER) / DYNAMIC(PER_USER만), SHARED / PER_USER
    WEB,
    // STATIC / DYNAMIC, PER_USER만 (포트를 밖에 열어서 SHARED는 Judge가 거부한다)
    PWN,
    // STATIC, NONE
    MISC;

    /** 인스턴스(컨테이너)를 띄우는 문제인지. Judge의 instanceCategories와 같은 값이어야 한다. */
    public boolean usesInstance() {
        return this == WEB || this == PWN;
    }
}
