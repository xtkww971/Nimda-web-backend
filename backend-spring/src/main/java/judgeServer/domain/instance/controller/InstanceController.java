package judgeServer.domain.instance.controller;

import com.nimda.cite.common.exception.GlobalNimdaException;
import com.nimda.cite.common.exception.UnauthorizedException;
import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.exposure.ChallengeExposure;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.instance.service.CtfServerStatus;
import judgeServer.domain.instance.service.InstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 동적 인스턴스 접근 컨트롤러.
 *
 * <pre>
 *   POST /api/ctf/instance/{code}              생성 요청(살아 있으면 재사용) → uuid, status
 *   GET  /api/ctf/instance/{code}              이 문제로 내가 띄운 인스턴스 (문제 페이지에서 씀)
 *   GET  /api/ctf/instance/status/{uuid}       생성 직후 폴링
 * </pre>
 *
 * 문제 코드와 uuid는 둘 다 한 조각 경로라 같은 자리에 둘 수 없다(매핑 충돌). 그래서
 * uuid 조회는 {@code /status/} 아래로 내린다.
 *
 * 실제 접속은 응답의 accessUrl(inst-{uuid}.{도메인})로 하고, SubdomainInstanceProxyFilter가 컨테이너로 넘긴다.
 */
@RestController
@RequestMapping(UrlNames.instanceUrl)
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final CtfServerStatus serverStatus;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;
    private final ChallengeExposure exposure;

    // 컨테이너 생성 요청
    @PostMapping(UrlNames.createInstanceUrl)
    public ResponseEntity<?> create(@PathVariable String code,
                                    @AuthenticationPrincipal CustomUserDetails user,
                                    HttpServletRequest request) {
        Long userId = currentUserId(user);
        Challenge challenge = findChallenge(code, user);

        // WEB·PWN만 인스턴스를 띄운다. Judge의 분류(instanceCategories)와 같은 기준이다.
        if (!challenge.getCategory().usesInstance()) {
            throw new GlobalNimdaException(ErrorCode.INSTANCE_NOT_SUPPORTED);
        }

        // 버튼을 숨기는 것만으로는 API 직접 호출을 못 막는다. 꺼진 서버로 보낸 요청은 아무도 안 읽어서
        // PENDING에 멈추므로, 넣기 전에 거절한다 (CTF 레포 ADR-0022).
        if (!serverStatus.isAvailable()) {
            throw new GlobalNimdaException(ErrorCode.CTF_SERVER_UNAVAILABLE);
        }

        String uuid = instanceService.createInstance(challenge, userId);
        if (uuid == null) {
            // WEB·PWN인데 isolationType이 NONE인 경우. 문제 설정이 잘못된 것이다.
            throw new GlobalNimdaException(ErrorCode.INSTANCE_NOT_SUPPORTED);
        }
        return ApiResponse.ok(view(uuid, request)).toResponse();
    }

    /**
     * 이 문제에 대해 내가 띄운 인스턴스 정보. 문제 페이지를 열 때마다 호출하면 되고,
     * 새로고침하거나 다른 기기에서 들어와도 uuid를 몰라도 이어서 볼 수 있다.
     *
     * <ul>
     *   <li>요청한 적 없음 → {@code hasInstance:false}</li>
     *   <li>있음 → {@code hasInstance:true} + 상태(PENDING/READY/FAILED/EXPIRED)</li>
     * </ul>
     */
    @GetMapping(UrlNames.getMyInstanceUrl)
    public ResponseEntity<?> myInstance(@PathVariable String code,
                                        @AuthenticationPrincipal CustomUserDetails user,
                                        HttpServletRequest request) {
        Long userId = currentUserId(user);
        Challenge challenge = findChallenge(code, user);

        String uuid = instanceService.findMyUuid(userId, challenge.getId()).orElse(null);
        if (uuid == null) {
            return ApiResponse.ok(Map.of("hasInstance", false)).toResponse();
        }

        Map<String, Object> view = view(uuid, request);
        view.put("hasInstance", true);
        return ApiResponse.ok(view).toResponse();
    }

    // 인스턴스 상태 조회 api (생성 직후 폴링)
    @GetMapping(UrlNames.getInstanceStatusUrl)
    public ResponseEntity<?> status(@PathVariable String uuid,
                                    @AuthenticationPrincipal CustomUserDetails user,
                                    HttpServletRequest request) {
        // 결과가 아직 없어도(PENDING) 소유자부터 확인한다. uuid만 알면 남의 상태를 볼 수 있으면 안 된다.
        if (!instanceService.isOwner(uuid, currentUserId(user))) {
            throw new GlobalNimdaException(ErrorCode.INSTANCE_FORBIDDEN);
        }
        return ApiResponse.ok(view(uuid, request)).toResponse();
    }

    // ------------------------------helper--------

    private Map<String, Object> view(String uuid, HttpServletRequest request) {
        return instanceService.view(uuid, request.getScheme(), request.getServerPort());
    }

    // 비공개 문제(초안)는 관리자와 DEV만. 코드만 알면 인스턴스를 띄울 수 있으면 안 된다.
    // 팀전이라 팀이 없는 참가자는 인스턴스를 띄우거나 볼 수 없다 (관리자·DEV 제외).
    private Challenge findChallenge(String code, CustomUserDetails user) {
        teamResolver.requireTeamOrStaff(user);
        contestService.requireChallengesAccessible(user);
        // 노출 규칙 (게임 모드): DEV 전부, ADMIN 공개 문제, 참가자는 공개 + 게임에 나온 문제 또는 우리 팀이 푼 문제.
        // 규칙 밖은 없는 문제와 같은 404. 코드만 알면 받아갈 수 있으면 안 된다.
        Challenge challenge = exposure.requireOpen(code, user);
        return challenge;
    }

    private Long currentUserId(CustomUserDetails user) {
        if (user == null) {
            throw new UnauthorizedException(ErrorCode.CANNOT_AUTHORIZED);
        }
        return user.getUser().getId();
    }

}
