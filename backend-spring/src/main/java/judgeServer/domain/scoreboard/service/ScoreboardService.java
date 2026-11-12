package judgeServer.domain.scoreboard.service;

import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.groupMember.entity.GroupMember;
import judgeServer.domain.groupMember.repository.GroupMemberRepository;
import judgeServer.domain.scoreboard.dto.*;
import judgeServer.domain.scoreboard.entity.CtfTeamScore;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 팀 점수판·내 팀 점수 조회. 점수 반영은 {@link judgeServer.domain.solve.service.SolveRecorder}가 한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreboardService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int RECENT_SOLVES_LIMIT = 10;

    private final CtfTeamScoreRepository teamScoreRepository;
    private final SolveRepository solveRepository;
    private final GroupMemberRepository memberRepository;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;

    /** 현재 대회에서 점수가 있는 팀을 순위 순서대로. 정렬이 유일하므로 순위는 페이지 위치로 정한다. */
    public TeamScoreboardPageResponse getScoreboard(int page, int size) {
        Long contestId = contestService.currentContestId().orElse(null);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<TeamScoreboardRow> rows = teamScoreRepository.findRanking(contestId, PageRequest.of(safePage, safeSize));
        long firstRank = (long) safePage * safeSize + 1;

        List<TeamScoreboardRow> content = rows.getContent();
        List<TeamScoreboardEntryResponse> entries = new ArrayList<>(content.size());
        for (int i = 0; i < content.size(); i++) {
            entries.add(TeamScoreboardEntryResponse.of(firstRank + i, content.get(i)));
        }
        return new TeamScoreboardPageResponse(entries, safePage, safeSize, rows.getTotalElements(),
                rows.getTotalPages(), teamScoreRepository.countInContest(contestId));
    }

    /** 내 팀 점수·순위·멤버별 기여·최근 해결. 팀이 없으면 비어 있다. */
    public Optional<MyTeamScoreResponse> getMyTeam(Long userId) {
        Optional<Long> teamId = teamResolver.findTeamId(userId);
        if (teamId.isEmpty()) {
            return Optional.empty();
        }
        Long id = teamId.get();
        Long contestId = contestService.currentContestId().orElse(null);

        List<GroupMember> members = memberRepository.findByGroupId(id);
        if (members.isEmpty()) {
            return Optional.empty();
        }
        String teamName = members.get(0).getGroup().getName();

        CtfTeamScore score = teamScoreRepository.findById(id).orElse(null);
        long totalScore = score == null ? 0 : score.getTotalScore();
        int solveCount = score == null ? 0 : score.getSolveCount();
        Long rank = totalScore > 0
                ? teamScoreRepository.countAhead(contestId, id, score.getTotalScore(), score.getLastSolvedAt()) + 1
                : null;

        Map<Long, SolveRepository.MemberContribution> contributions = solveRepository.findContributionsByTeamId(id)
                .stream()
                .collect(Collectors.toMap(SolveRepository.MemberContribution::getUserId, Function.identity()));

        List<MyTeamScoreResponse.Contribution> memberViews = members.stream()
                .map(m -> {
                    SolveRepository.MemberContribution c = contributions.get(m.getUser().getId());
                    return new MyTeamScoreResponse.Contribution(
                            m.getUser().getId(), m.getUser().getNickname(), m.getRole(),
                            c == null ? 0 : c.getSolveCount(),
                            c == null || c.getPoints() == null ? 0 : c.getPoints());
                })
                // 기여 점수 높은 순, 같으면 팀장 먼저
                .sorted(Comparator.comparingLong(MyTeamScoreResponse.Contribution::points).reversed()
                        .thenComparing(c -> c.role().ordinal()))
                .toList();

        return Optional.of(new MyTeamScoreResponse(
                id, teamName, rank, teamScoreRepository.countInContest(contestId), totalScore, solveCount, members.size(),
                score == null ? null : score.getLastSolvedAt(),
                memberViews,
                solveRepository.findRecentByTeamId(id, PageRequest.of(0, RECENT_SOLVES_LIMIT))));
    }
}
