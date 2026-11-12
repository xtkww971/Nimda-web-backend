package judgeServer.domain.solve.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.solve.dto.SubmissionVerdict;
import judgeServer.domain.solve.dto.SubmitFlagRequest;
import judgeServer.domain.solve.service.FlagSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 플래그 제출.
 *
 * <pre>
 *   POST /api/ctf/challenge/{challengeId}          -> 제출. 3초 안에 채점되면 200, 아니면 202
 *   GET  /api/ctf/challenge/submission/{id}        -> 202를 받았을 때 결과 조회
 * </pre>
 */
@RestController
@RequestMapping("/api/ctf/challenge")
@RequiredArgsConstructor
public class FlagSubmissionController {

    private final FlagSubmissionService submissionService;

    @PostMapping(value = "/{challengeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submit(@PathVariable Long challengeId,
                                    @Valid @RequestBody SubmitFlagRequest request,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        FlagSubmissionService.Submission submission = submissionService.submit(
                challengeId, user.getUser().getId(), canAccessDrafts(user), request.flag());

        return respond(submission, HttpStatus.ACCEPTED);
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<?> result(@PathVariable String submissionId,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return respond(submissionService.find(submissionId, user.getUser().getId()), HttpStatus.OK);
    }

    private ResponseEntity<?> respond(FlagSubmissionService.Submission submission, HttpStatus pendingStatus) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("submissionId", submission.submissionId());

        if (submission.verdict().isEmpty()) {
            body.put("status", "PENDING");
            return ApiResponse.ok(body).toResponse(pendingStatus);
        }

        SubmissionVerdict verdict = submission.verdict().get();
        switch (verdict.status()) {
            case NOT_FOUND -> {
                return ApiResponse.fail("존재하지 않는 문제입니다.").toResponse(HttpStatus.NOT_FOUND);
            }
            case NO_TEAM -> {
                return ApiResponse.fail(CtfTeamResolver.NO_TEAM_MESSAGE).toResponse(HttpStatus.FORBIDDEN);
            }
            case UNSUPPORTED -> {
                return ApiResponse.fail("DYNAMIC 플래그 제출은 아직 지원하지 않습니다.").toResponse(HttpStatus.BAD_REQUEST);
            }
            case ERROR -> {
                return ApiResponse.fail("채점에 실패했습니다. 잠시 후 다시 시도해 주세요.")
                        .toResponse(HttpStatus.SERVICE_UNAVAILABLE);
            }
            default -> {
            }
        }
        body.put("status", verdict.status().name());
        body.put("awardedPoints", verdict.awardedPoints());
        return ApiResponse.ok(body).toResponse();
    }

    private boolean canAccessDrafts(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_DEV".equals(role));
    }
}
