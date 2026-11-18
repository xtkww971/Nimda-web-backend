package judgeServer.domain.challenge.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.challenge.dto.ChallengeDetailResponse;
import judgeServer.domain.challenge.dto.ChallengeSummaryResponse;
import judgeServer.domain.challenge.dto.SolvedChallengesResponse;
import judgeServer.domain.challenge.exposure.ChallengeExposure;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.challenge.service.SolvedChallengeService;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 문제 조회 API (읽기 전용). CTF 사이트는 "게임 모드"다: 참가자는 문제를 게임(game.nimda.kr) 맵에서 찾고,
 * 사이트에서는 우리 팀이 푼 문제만 다시 본다 (Figma "CTF 대시보드 MMO Game v1", G07).
 *
 * <pre>
 *   GET /api/ctf/challenges          -> 전체 문제 목록 (초안 포함, 해결 팀 수). ROLE_DEV 만
 *   GET /api/ctf/challenges/solved   -> 우리 팀이 푼 문제 (팀원 누구든, 현재 대회). 참가자
 *   GET /api/ctf/challenges/{code}   -> 문제 상세 + 우리 팀 해결 여부. 게임이 아직 안 푼 문제를 열 때도 쓴다
 * </pre>
 *
 * <p>등록/삭제/공개전환은 DEV 용 {@link ChallengeController}(단수 경로 {@code /api/ctf/challenge})에 있다.
 *
 * <p>상세는 {@link ChallengeExposure} 의 노출 규칙을 따른다: DEV 전부, ADMIN 공개 문제, 참가자는 공개 + 게임에 나온 문제
 * 또는 우리 팀이 푼 문제. 코드만 알면 열람되는 일이 없도록, 규칙 밖은 없는 문제와 같은 404로 답한다.
 *
 * <p>팀전이라 팀에 소속되지 않은 참가자에게는 해결 목록·상세 모두 403으로 답한다 (관리자·DEV 제외).
 */
@RestController
@RequestMapping(UrlNames.challengesUrl)
@RequiredArgsConstructor
public class ChallengeQueryController {

    private final ChallengeRepository challengeRepository;
    private final SolveRepository solveRepository;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;
    private final SolvedChallengeService solvedChallengeService;
    private final ChallengeExposure exposure;

    /** 전체 문제 목록 (문제 관리 화면). 참가자에게 전체 목록·개수·코드를 보여 주지 않으므로 DEV 만. */
    @GetMapping(UrlNames.getChallengesUrl)
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails user) {
        if (!isDev(user)) {
            throw new AccessDeniedException("전체 문제 목록은 ROLE_DEV 만 볼 수 있습니다.");
        }
        List<Challenge> challenges = challengeRepository.findAllByOrderByIdAsc();

        Map<Long, Long> solveCounts = solveRepository.countGroupByChallengeIdInContest(contestService.currentContestId().orElse(null)).stream()
                .collect(Collectors.toMap(SolveRepository.SolveCount::getChallengeId,
                        SolveRepository.SolveCount::getSolves));

        return ApiResponse.ok(challenges.stream()
                        .map(c -> ChallengeSummaryResponse.from(c, solveCounts.getOrDefault(c.getId(), 0L)))
                        .toList())
                .toResponse();
    }

    /** 우리 팀이 푼 문제 + 찾을 수 있는 문제 수. 푼 순서대로. 순위 비공개 중에는 참가자에게 문제별 점수가 null. */
    @GetMapping(UrlNames.getSolvedChallengesUrl)
    public ResponseEntity<?> solved(@AuthenticationPrincipal CustomUserDetails user) {
        teamResolver.requireTeamOrStaff(user);
        contestService.requireChallengesAccessible(user);
        return ApiResponse.ok(new SolvedChallengesResponse(exposure.participantTotal(),
                solvedChallengeService.solvedFor(user))).toResponse();
    }

    /** 문제 상세. 게임의 문제 창이 안 푼 문제를 열 때도 쓰므로, 게임에 나온 문제는 참가자에게 막지 않는다. */
    @GetMapping(UrlNames.getChallengeDetailUrl)
    public ResponseEntity<?> detail(@PathVariable String code,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        teamResolver.requireTeamOrStaff(user);
        contestService.requireChallengesAccessible(user);
        Challenge challenge = exposure.requireOpen(code, user);
        return ApiResponse.ok(ChallengeDetailResponse.from(challenge,
                solveRepository.countInContest(challenge.getId(), contestService.currentContestId().orElse(null)),
                solvedChallengeService.solvedFor(user, challenge.getCode()).orElse(null),
                !CtfTeamResolver.isStaff(user) && contestService.isScoreboardHiddenNow())).toResponse();
    }

    /** 문제 전체 목록·초안은 ROLE_DEV 만 (G07: 등록·수정·삭제·공개 전환·전체 목록은 DEV). */
    static boolean isDev(CustomUserDetails user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DEV"::equals);
    }
}
