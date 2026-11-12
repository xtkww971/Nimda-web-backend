package judgeServer.domain.contest.service;

import judgeServer.domain.contest.dto.ContestDetailResponse;
import judgeServer.domain.contest.dto.ContestListResponse;
import judgeServer.domain.contest.entity.Contest;
import judgeServer.domain.contest.entity.ContestResult;
import judgeServer.domain.contest.entity.ContestResultMember;
import judgeServer.domain.contest.repository.ContestChallengeResultRepository;
import judgeServer.domain.contest.repository.ContestRepository;
import judgeServer.domain.contest.repository.ContestResultMemberRepository;
import judgeServer.domain.contest.repository.ContestResultRepository;
import judgeServer.domain.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static judgeServer.domain.contest.service.ContestService.toKst;

/** 대회 이력 (디자인: 09 대회 이력, 10 대회 이력 상세). 확정된 스냅샷만 보여준다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestHistoryService {

    private final ContestService contestService;
    private final ContestRepository contestRepository;
    private final ContestResultRepository resultRepository;
    private final ContestResultMemberRepository resultMemberRepository;
    private final ContestChallengeResultRepository challengeResultRepository;
    private final GroupRepository groupRepository;

    /**
     * @param participatedOnly 내가 팀으로 참가한 대회만
     * @param oldestFirst      오래된 순 (기본은 최신순)
     */
    public ContestListResponse list(Long userId, boolean participatedOnly, boolean oldestFirst) {
        ContestListResponse.CurrentContest current = contestService.current()
                .filter(c -> !c.isFinalized())
                .map(c -> new ContestListResponse.CurrentContest(c.getId(), c.getName(), toKst(c.getStartsAt()),
                        toKst(c.getEndsAt()), c.phaseAt(contestService.now()),
                        groupRepository.countInContest(c.getId())))
                .orElse(null);

        List<Contest> ended = new ArrayList<>(contestRepository.findByFinalizedAtIsNotNull());
        Comparator<Contest> latestFirst = Comparator.comparing(Contest::getStartsAt).thenComparing(Contest::getId).reversed();
        ended.sort(oldestFirst ? latestFirst.reversed() : latestFirst);
        if (ended.isEmpty()) {
            return new ContestListResponse(current, List.of());
        }

        List<Long> ids = ended.stream().map(Contest::getId).toList();
        Map<Long, List<ContestResult>> resultsByContest = resultRepository.findByContestIdIn(ids).stream()
                .collect(Collectors.groupingBy(ContestResult::getContestId));
        Map<Long, Long> challengeCounts = challengeResultRepository.countByContestIds(ids).stream()
                .collect(Collectors.toMap(ContestChallengeResultRepository.ContestCount::getContestId,
                        ContestChallengeResultRepository.ContestCount::getCount));
        Map<Long, ContestResultMember> myMemberships = resultMemberRepository.findByUserIdAndContestIdIn(userId, ids)
                .stream().collect(Collectors.toMap(ContestResultMember::getContestId, m -> m));

        List<ContestListResponse.EndedContest> rows = new ArrayList<>();
        for (Contest c : ended) {
            ContestResultMember mine = myMemberships.get(c.getId());
            if (participatedOnly && mine == null) {
                continue;
            }
            List<ContestResult> results = resultsByContest.getOrDefault(c.getId(), List.of());
            ContestResult winner = results.stream()
                    .filter(r -> Integer.valueOf(1).equals(r.getTeamRank())).findFirst().orElse(null);
            ContestResult myTeam = mine == null ? null : results.stream()
                    .filter(r -> r.getTeamId().equals(mine.getTeamId())).findFirst().orElse(null);

            rows.add(new ContestListResponse.EndedContest(
                    c.getId(), c.getName(), toKst(c.getStartsAt()), toKst(c.getEndsAt()),
                    results.size(),
                    results.stream().mapToLong(ContestResult::getMemberCount).sum(),
                    challengeCounts.getOrDefault(c.getId(), 0L),
                    winner == null ? null
                            : new ContestListResponse.Winner(winner.getTeamId(), winner.getTeamName(), winner.getTotalScore()),
                    myTeam == null ? null
                            : new ContestListResponse.MyResult(myTeam.getTeamRank(), myTeam.getTeamId(),
                            myTeam.getTeamName(), myTeam.getTotalScore())));
        }
        return new ContestListResponse(current, rows);
    }

    /** 확정된 대회만 볼 수 있다. 진행 중이거나 확정 전이면 404. */
    public ContestDetailResponse detail(Long contestId, Long userId) {
        Contest c = contestRepository.findById(contestId)
                .filter(Contest::isFinalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결과가 확정된 대회가 아닙니다."));

        List<ContestResult> results = resultRepository.findStandings(contestId);
        List<ContestDetailResponse.Standing> standings = results.stream()
                .map(r -> new ContestDetailResponse.Standing(r.getTeamRank(), r.getTeamId(), r.getTeamName(),
                        r.getMemberCount(), r.getSolveCount(), r.getTotalScore(), r.getLastSolvedAt()))
                .toList();

        List<ContestDetailResponse.ChallengeResult> challenges =
                challengeResultRepository.findByContestIdOrderByMaxPointAscCodeAsc(contestId).stream()
                        .map(ch -> new ContestDetailResponse.ChallengeResult(ch.getChallengeId(), ch.getCode(),
                                ch.getTitle(), ch.getCategory(), ch.getMaxPoint(), ch.getFinalPoint(),
                                ch.getSolvedTeams(), ch.getFirstBloodTeamName(), ch.getFirstBloodAt()))
                        .toList();

        ContestDetailResponse.Stats stats = new ContestDetailResponse.Stats(
                results.size(),
                results.stream().mapToLong(ContestResult::getMemberCount).sum(),
                challenges.size(),
                results.stream().mapToLong(ContestResult::getSolveCount).sum());

        return new ContestDetailResponse(c.getId(), c.getName(), toKst(c.getStartsAt()), toKst(c.getEndsAt()),
                toKst(c.getFinalizedAt()), stats, standings, challenges, myTeam(contestId, userId, results));
    }

    private ContestDetailResponse.MyTeam myTeam(Long contestId, Long userId, List<ContestResult> results) {
        ContestResultMember mine = resultMemberRepository.findByContestIdAndUserId(contestId, userId).orElse(null);
        if (mine == null) {
            return null;
        }
        ContestResult team = results.stream()
                .filter(r -> r.getTeamId().equals(mine.getTeamId())).findFirst().orElse(null);
        if (team == null) {
            return null;
        }
        List<ContestDetailResponse.Member> members = resultMemberRepository
                .findByContestIdAndTeamId(contestId, mine.getTeamId()).stream()
                .sorted(Comparator.comparingLong(ContestResultMember::getPoints).reversed()
                        .thenComparing(m -> "LEADER".equals(m.getRole()) ? 0 : 1)
                        .thenComparing(ContestResultMember::getNickname))
                .map(m -> new ContestDetailResponse.Member(m.getUserId(), m.getNickname(), m.getRole(),
                        m.getSolveCount(), m.getPoints()))
                .toList();
        return new ContestDetailResponse.MyTeam(team.getTeamId(), team.getTeamName(), team.getTeamRank(),
                results.size(), team.getTotalScore(), team.getSolveCount(), members);
    }
}
