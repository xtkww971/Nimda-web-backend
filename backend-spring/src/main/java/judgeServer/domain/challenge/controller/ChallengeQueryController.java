package judgeServer.domain.challenge.controller;

import com.nimda.cite.common.exception.GlobalNimdaException;
import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.challenge.dto.ChallengeDetailResponse;
import judgeServer.domain.challenge.dto.ChallengeSummaryResponse;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 참가자가 문제를 보는 조회 API (읽기 전용).
 *
 * <pre>
 *   GET /api/ctf/challenges         -> 공개된 문제 목록
 *   GET /api/ctf/challenges/{code}  -> 문제 상세
 * </pre>
 *
 * <p>등록/삭제/공개전환은 관리자와 DEV용 {@link ChallengeController}(단수 경로 {@code /api/ctf/challenge})에
 * 있다. 조회를 복수 경로로 분리한 것은 시큐리티 규칙 때문이다. 단수 경로에는 "POST/DELETE는
 * 관리자와 DEV만"이 걸려 있어서, 참가자용 조회를 같은 경로에 두면 규칙이 서로 얽힌다.
 *
 * <p>비공개(초안) 문제는 출제 권한이 있는 관리자와 DEV에게만 보인다. 코드만 알면 열람되는 일이
 * 없도록, 없는 문제와 같은 404로 답한다.
 *
 * <p>팀전이라 팀에 소속되지 않은 참가자에게는 목록·상세 모두 403으로 답한다 (관리자·DEV 제외).
 */
@RestController
@RequestMapping(UrlNames.challengesUrl)
@RequiredArgsConstructor
public class ChallengeQueryController {

    private final ChallengeRepository challengeRepository;
    private final SolveRepository solveRepository;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;

    /** 문제 목록. 관리자와 DEV는 초안까지 함께 본다. */
    @GetMapping(UrlNames.getChallengesUrl)
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails user) {
        teamResolver.requireTeamOrStaff(user);
        contestService.requireChallengesAccessible(user);
        List<Challenge> challenges = canViewDrafts(user)
                ? challengeRepository.findAllByOrderByIdAsc()
                : challengeRepository.findByIsPublicTrueOrderByIdAsc();

        Map<Long, Long> solveCounts = solveRepository.countGroupByChallengeIdInContest(contestService.currentContestId().orElse(null)).stream()
                .collect(Collectors.toMap(SolveRepository.SolveCount::getChallengeId,
                        SolveRepository.SolveCount::getSolves));

        return ApiResponse.ok(challenges.stream()
                        .map(c -> ChallengeSummaryResponse.from(c, solveCounts.getOrDefault(c.getId(), 0L)))
                        .toList())
                .toResponse();
    }

    /** 문제 상세. */
    @GetMapping(UrlNames.getChallengeDetailUrl)
    public ResponseEntity<?> detail(@PathVariable String code,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        teamResolver.requireTeamOrStaff(user);
        contestService.requireChallengesAccessible(user);
        Challenge challenge = challengeRepository.findByCode(code)
                .orElseThrow(() -> new GlobalNimdaException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (!Boolean.TRUE.equals(challenge.getIsPublic()) && !canViewDrafts(user)) {
            throw new GlobalNimdaException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        return ApiResponse.ok(ChallengeDetailResponse.from(challenge,
                solveRepository.countInContest(challenge.getId(), contestService.currentContestId().orElse(null)))).toResponse();
    }

    private boolean canViewDrafts(CustomUserDetails user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_DEV".equals(role));
    }
}
