package judgeServer.domain.instance.Contoller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.instance.Service.InstanceService;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
@RequestMapping("/api/ctf/instance")
@RequiredArgsConstructor
public class InstanceController {

    private final ChallengeRepository challengeRepository;
    private final InstanceService instanceService;

    // 컨테이너 생성 요청
    @PostMapping("/{code}")
    public ResponseEntity<?> create(@PathVariable String code,
                                    @AuthenticationPrincipal CustomUserDetails user,
                                    HttpServletRequest request) {
        Long userId = currentUserId(user);
        Challenge challenge = findChallenge(code, user);

        // WEB·PWN만 인스턴스를 띄운다. Judge의 분류(instanceCategories)와 같은 기준이다.
        if (!challenge.getCategory().usesInstance()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 문제는 인스턴스 기능을 지원하지 않습니다.");
        }

        String uuid = instanceService.createInstance(challenge, userId);
        if (uuid == null) {
            // WEB·PWN인데 isolationType이 NONE인 경우. 문제 설정이 잘못된 것이다.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 문제는 인스턴스 기능을 지원하지 않습니다.");
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
    @GetMapping("/{code}")
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
    @GetMapping("/status/{uuid}")
    public ResponseEntity<?> status(@PathVariable String uuid,
                                    @AuthenticationPrincipal CustomUserDetails user,
                                    HttpServletRequest request) {
        // 결과가 아직 없어도(PENDING) 소유자부터 확인한다. uuid만 알면 남의 상태를 볼 수 있으면 안 된다.
        if (!instanceService.isOwner(uuid, currentUserId(user))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 인스턴스가 아닙니다.");
        }
        return ApiResponse.ok(view(uuid, request)).toResponse();
    }

    // ------------------------------helper--------

    private Map<String, Object> view(String uuid, HttpServletRequest request) {
        return instanceService.view(uuid, request.getScheme(), request.getServerPort());
    }

    // 비공개 문제(초안)는 관리자만. 코드만 알면 인스턴스를 띄울 수 있으면 안 된다.
    private Challenge findChallenge(String code, CustomUserDetails user) {
        Challenge challenge = challengeRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다."));
        if (!Boolean.TRUE.equals(challenge.getIsPublic()) && !isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다.");
        }
        return challenge;
    }

    private Long currentUserId(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return user.getUser().getId();
    }

    private boolean isAdmin(CustomUserDetails user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
