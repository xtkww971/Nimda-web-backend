package judgeServer.domain.challenge.download;

import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.NotIncludeRoleException;
import com.nimda.cite.common.exception.UnauthorizedException;
import com.nimda.cite.common.exception.errorcode.ctf.ChallengeErrorCode;
import com.nimda.cite.common.exception.errorcode.user.AuthErrorCode;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.challenge.service.ChallengeRules;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import redis.mq.message.CtfResultMessage;
import redis.mq.message.RequestStatus;
import redis.mq.producer.RedisCtfRequestProducer;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(UrlNames.challengeUrl)
@RequiredArgsConstructor
public class ChallengeDownloadController {

    // 발급 대기 시간
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(3);
    // todo 폴링 대신 freeze 방식으로 변환 생각
    private static final long POLL_INTERVAL_MS = 100;

    private final ChallengeRepository challengeRepository;
    private final RedisCtfRequestProducer producer;
    private final ChallengeDownloadStore resultStore;
    private final ChallengeDownloadUrlCache urlCache;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;

    // 문제 코드로 다운로드 링크 요청
    @PostMapping(UrlNames.requestChallengeDownloadUrl)
    public ResponseEntity<?> request(@PathVariable String code,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        // 팀전이라 팀이 없는 참가자는 문제 파일을 받을 수 없다 (관리자·DEV 제외).
        teamResolver.requireTeamOrStaff(user);
        contestService.requireChallengesAccessible(user);
        Challenge challenge = challengeRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        // 비공개 문제(초안)는 관리자와 DEV만. 코드만 알면 받아갈 수 있으면 안 된다.
        if (!Boolean.TRUE.equals(challenge.getIsPublic()) && !canAccessDrafts(user)) {
            throw new NotFoundException(ChallengeErrorCode.CHALLENGE_NOT_FOUND);
        }
        // 다운로드는 정적 문제만. Judge는 category로 할 일을 정해서, WEB·PWN 문제로 발행하면
        // 다운로드 링크 대신 컨테이너를 띄운다(누를 때마다 새 uuid라 하나씩 쌓인다).
        if (challenge.getCategory().usesInstance()) {
            throw new NotAllowFormException(ChallengeErrorCode.DOWNLOAD_NOT_SUPPORTED);
        }
        if (challenge.getAttachmentKey() == null || challenge.getAttachmentKey().isBlank()) {
            throw new NotFoundException(ChallengeErrorCode.ATTACHMENT_NOT_FOUND);
        }

        Long userId = requireUserId(user);
        // 규칙 이전에 저장된 잘못된 조합(정적 문제인데 DYNAMIC 등)은 발행하지 않는다.
        ChallengeRules.validate(challenge.getCategory(), challenge.getIsolationType(), challenge.getFlagType());

        // redis 캐싱 확인
        Optional<ChallengeDownloadUrlCache.CachedUrl> cached = urlCache.find(code);
        if (cached.isPresent()) {
            return ApiResponse.ok(Map.of(
                    "status", RequestStatus.READY.name(),
                    "url", cached.get().url(),
                    "expiresAt", cached.get().expiresAt() != null ? cached.get().expiresAt() : "",
                    "cached", true)).toResponse();
        }

        // uuid는 여기서 만든다. 발행 전에 이 값으로 소유자 매핑이 먼저 기록돼야 한다.
        String requestId = producer.request(challenge, userId, java.util.UUID.randomUUID().toString());

        Optional<CtfResultMessage> result = awaitResult(requestId);
        if (result.isEmpty()) {
            return ApiResponse.ok(Map.of("status", "PENDING", "requestId", requestId))
                    .toResponse(HttpStatus.ACCEPTED);
        }
        return respond(result.get(), requestId);
    }

    /** 발급 결과를 조회한다 (아직이면 PENDING). */
    @GetMapping(UrlNames.getChallengeDownloadStatusUrl)
    public ResponseEntity<?> status(@PathVariable String requestId,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        CtfResultMessage result = resultStore.find(requestId).orElse(null);
        if (result == null) {
            return ApiResponse.ok(Map.of("status", "PENDING", "requestId", requestId)).toResponse();
        }
        checkOwner(requestId, user);
        return respond(result, requestId);
    }

    // redis 결과 대기
    private Optional<CtfResultMessage> awaitResult(String requestId) {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        while (true) {
            Optional<CtfResultMessage> found = resultStore.find(requestId);
            if (found.isPresent()) {
                return found;
            }
            if (System.nanoTime() >= deadline) {
                return Optional.empty();
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    private ResponseEntity<?> respond(CtfResultMessage result, String requestId) {
        if (result.getStatus() != RequestStatus.READY || result.getDownloadUrl() == null) {
            // 발급 실패는 CTF 서버가 준 사유를 그대로 전한다 (첨부파일 없음, 허용되지 않는 경로 등).
            String reason = result.getMessage() != null ? result.getMessage() : "다운로드 링크를 발급하지 못했습니다.";
            return ApiResponse.fail(reason).toResponse(HttpStatus.SERVICE_UNAVAILABLE);
        }
        return ApiResponse.ok(Map.of(
                "status", result.getStatus().name(),
                "requestId", requestId,
                "url", result.getDownloadUrl(),
                "expiresAt", result.getExpiresAt() != null ? result.getExpiresAt() : "",
                "cached", false)).toResponse();
    }

    /**
     * 결과 메시지에는 더 이상 userId가 없다(식별자가 uuid 하나로 줄었다). 그래서 발행할 때
     * 남겨둔 uuid → 사용자 매핑으로 확인한다. 매핑이 만료됐으면 확인할 근거가 없으므로 막는다 —
     * 확인 못 하는 요청을 통과시키면 uuid를 아는 누구나 남의 링크를 받아갈 수 있다.
     */
    private void checkOwner(String requestId, CustomUserDetails user) {
        Long owner = producer.findUserId(requestId);
        if (user == null || owner == null || !owner.equals(user.getUser().getId())) {
            throw new NotIncludeRoleException(ChallengeErrorCode.DOWNLOAD_FORBIDDEN);
        }
    }

    private Long requireUserId(CustomUserDetails user) {
        if (user == null) {
            throw new UnauthorizedException(AuthErrorCode.CANNOT_AUTHORIZED);
        }
        return user.getUser().getId();
    }

    private boolean canAccessDrafts(CustomUserDetails user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_DEV".equals(role));
    }
}
