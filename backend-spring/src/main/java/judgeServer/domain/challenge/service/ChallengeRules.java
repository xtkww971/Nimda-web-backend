package judgeServer.domain.challenge.service;

import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.enums.FlagType;
import judgeServer.domain.challenge.enums.IsolationType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 문제 유형(category)·격리 방식(isolationType)·플래그 방식(flagType)의 허용 조합.
 * Judge Server가 받는 조합만 저장·발행하게 한다(provisioner task.go, protocol stream.go).
 *
 * <pre>
 *   category                              isolationType        flagType
 *   WEB                                   SHARED, PER_USER     SHARED면 STATIC만
 *   PWN                                   PER_USER             STATIC, DYNAMIC
 *   REVERSING, FORENSICS, CRYPTO, MISC    NONE                 STATIC
 * </pre>
 *
 * 등록할 때 막고, 이미 저장된 잘못된 행은 발행 직전에 한 번 더 막는다. 안 막으면 Judge가 FAILED를
 * 돌려주거나(PWN+SHARED), 다운로드인데 컨테이너가 뜨거나, 발행 중 예외로 500이 난다(SHARED+DYNAMIC).
 */
public final class ChallengeRules {

    private ChallengeRules() {
    }

    /** 허용되지 않는 조합이면 400. */
    public static void validate(ChallengeCategory category, IsolationType isolation, FlagType flagType) {
        String reason = reason(category, isolation, flagType);
        if (reason != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }
    }

    private static String reason(ChallengeCategory category, IsolationType isolation, FlagType flagType) {
        if (!category.usesInstance()) {
            // 정적 문제는 다운로드만 한다. 컨테이너가 없으니 참가자별 플래그도 줄 수 없다.
            if (isolation != IsolationType.NONE) {
                return category + " 문제는 인스턴스를 쓰지 않습니다(isolationType=NONE).";
            }
            if (flagType != FlagType.STATIC) {
                return category + " 문제는 STATIC 플래그만 쓸 수 있습니다.";
            }
            return null;
        }

        if (isolation == IsolationType.NONE) {
            return category + " 문제는 인스턴스가 필요합니다(isolationType=SHARED 또는 PER_USER).";
        }
        // PWN은 포트를 밖에 연다. SHARED로 뜨면 한 명이 셸을 따서 모두의 문제를 망친다(Judge가 거부한다).
        if (category == ChallengeCategory.PWN && isolation != IsolationType.PER_USER) {
            return "PWN 문제는 PER_USER만 쓸 수 있습니다.";
        }
        // 여럿이 한 컨테이너를 쓰니 참가자별 플래그를 심을 수 없다.
        if (isolation == IsolationType.SHARED && flagType == FlagType.DYNAMIC) {
            return "SHARED 문제는 DYNAMIC 플래그를 쓸 수 없습니다.";
        }
        return null;
    }
}
