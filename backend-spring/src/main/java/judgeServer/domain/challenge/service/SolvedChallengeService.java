package judgeServer.domain.challenge.service;

import com.nimda.cite.user.security.CustomUserDetails;
import judgeServer.domain.challenge.dto.SolvedChallengeResponse;
import judgeServer.domain.challenge.dto.SolvedChallengeRow;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * "우리 팀이 푼 문제" (CTF 사이트 게임 모드). 참가자는 전체 문제 목록 대신 이것만 본다.
 * 현재 대회에서 팀원 누구든 푼 문제. 팀이 없으면(ADMIN·DEV 시험 등) 본인이 푼 문제.
 * 순위 비공개 중에는 참가자에게 문제별 점수를 빼고 준다 (더하면 팀 점수가 드러나므로). ADMIN·DEV 는 늘 점수까지.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolvedChallengeService {

    private final SolveRepository solveRepository;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;

    public List<SolvedChallengeResponse> solvedFor(CustomUserDetails user) {
        Long userId = user.getUser().getId();
        Long contestId = contestService.currentContestId().orElse(null);
        List<SolvedChallengeRow> rows = teamResolver.findTeamId(userId)
                .map(teamId -> solveRepository.findSolvedRowsByTeam(teamId, contestId))
                .orElseGet(() -> solveRepository.findSolvedRowsByUser(userId, contestId));
        boolean hidePoints = !CtfTeamResolver.isStaff(user) && contestService.isScoreboardHiddenNow();
        return rows.stream().map(row -> SolvedChallengeResponse.from(row, hidePoints)).toList();
    }

    /** 이 문제를 우리 팀이 풀었으면 그 기록 (문제 상세의 "해결함" 배너). */
    public Optional<SolvedChallengeResponse> solvedFor(CustomUserDetails user, String code) {
        return solvedFor(user).stream().filter(s -> s.code().equals(code)).findFirst();
    }
}
