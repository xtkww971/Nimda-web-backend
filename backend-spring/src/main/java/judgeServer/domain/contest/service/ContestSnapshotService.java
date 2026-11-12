package judgeServer.domain.contest.service;

import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.contest.entity.ContestChallengeResult;
import judgeServer.domain.contest.entity.ContestResult;
import judgeServer.domain.contest.entity.ContestResultMember;
import judgeServer.domain.contest.repository.ContestChallengeResultRepository;
import judgeServer.domain.contest.repository.ContestResultMemberRepository;
import judgeServer.domain.contest.repository.ContestResultRepository;
import judgeServer.domain.group.entity.Group;
import judgeServer.domain.group.repository.GroupRepository;
import judgeServer.domain.groupMember.entity.GroupMember;
import judgeServer.domain.scoreboard.entity.CtfTeamScore;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import judgeServer.domain.solve.entity.Solve;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 대회 최종 결과 스냅샷을 만들고 지운다. 호출하는 쪽({@link ContestService})이 대회 행을 잠근 트랜잭션 안에서 부른다.
 *
 * <p>스냅샷은 원본(팀·멤버·문제)을 복사해 두므로, 이후 팀이 지워지거나 멤버·닉네임이 바뀌어도 과거 결과는 그대로다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ContestSnapshotService {

    private final ContestResultRepository resultRepository;
    private final ContestResultMemberRepository resultMemberRepository;
    private final ContestChallengeResultRepository challengeResultRepository;
    private final GroupRepository groupRepository;
    private final CtfTeamScoreRepository teamScoreRepository;
    private final SolveRepository solveRepository;
    private final ChallengeRepository challengeRepository;

    public void clear(Long contestId) {
        resultRepository.deleteByContestId(contestId);
        resultMemberRepository.deleteByContestId(contestId);
        challengeResultRepository.deleteByContestId(contestId);
    }

    /** 기존 스냅샷을 지우고 지금 상태로 다시 만든다. */
    public void write(Long contestId) {
        clear(contestId);

        List<Group> teams = groupRepository.findAllWithMembersInContest(contestId);
        Map<Long, CtfTeamScore> scores = teamScoreRepository.findAllById(teams.stream().map(Group::getId).toList())
                .stream().collect(Collectors.toMap(CtfTeamScore::getTeamId, Function.identity()));
        List<Solve> solves = solveRepository.findByContestIdOrderBySolvedAtAscIdAsc(contestId);

        writeStandings(contestId, teams, scores);
        writeMembers(contestId, teams, solves);
        writeChallenges(contestId, teams, solves);
    }

    // 라이브 스코어보드와 같은 기준: 점수 내림차순 → 마지막 득점 이른 순 → 팀 id. 점수 0인 팀은 순위 없음
    private void writeStandings(Long contestId, List<Group> teams, Map<Long, CtfTeamScore> scores) {
        Comparator<Group> order = Comparator
                .comparingLong((Group g) -> score(scores.get(g.getId()))).reversed()
                .thenComparing(g -> lastSolvedAt(scores.get(g.getId())), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Group::getId);

        List<ContestResult> results = new ArrayList<>();
        int rank = 0;
        for (Group team : teams.stream().sorted(order).toList()) {
            CtfTeamScore s = scores.get(team.getId());
            long total = score(s);
            results.add(ContestResult.builder()
                    .contestId(contestId)
                    .teamId(team.getId())
                    .teamRank(total > 0 ? ++rank : null)
                    .teamName(team.getName())
                    .memberCount(team.getMembers().size())
                    .solveCount(s == null ? 0 : s.getSolveCount())
                    .totalScore(total)
                    .lastSolvedAt(lastSolvedAt(s))
                    .build());
        }
        resultRepository.saveAll(results);
    }

    // 종료 시점 멤버만 남긴다. 기여는 그 대회에서 그 팀으로 낸 정답 기준
    private void writeMembers(Long contestId, List<Group> teams, List<Solve> solves) {
        Map<String, long[]> contribution = new HashMap<>(); // "teamId:userId" -> [solveCount, points]
        for (Solve s : solves) {
            long[] c = contribution.computeIfAbsent(s.getTeamId() + ":" + s.getUserId(), k -> new long[2]);
            c[0]++;
            c[1] += s.getAwardedPoints();
        }

        List<ContestResultMember> members = new ArrayList<>();
        for (Group team : teams) {
            for (GroupMember m : team.getMembers()) {
                long[] c = contribution.getOrDefault(team.getId() + ":" + m.getUser().getId(), new long[2]);
                members.add(ContestResultMember.builder()
                        .contestId(contestId)
                        .teamId(team.getId())
                        .userId(m.getUser().getId())
                        .nickname(m.getUser().getNickname())
                        .role(m.getRole().name())
                        .solveCount((int) c[0])
                        .points(c[1])
                        .build());
            }
        }
        resultMemberRepository.saveAll(members);
    }

    // 대회 문제 = 종료 시점에 공개된 문제 + 이 대회에서 정답이 나온 문제
    private void writeChallenges(Long contestId, List<Group> teams, List<Solve> solves) {
        Map<Long, String> teamNames = teams.stream().collect(Collectors.toMap(Group::getId, Group::getName));
        Map<Long, Integer> solvedTeams = new HashMap<>();
        Map<Long, Solve> firstBlood = new HashMap<>();
        for (Solve s : solves) { // 해결 시각 오름차순이라 처음 본 기록이 First Blood
            solvedTeams.merge(s.getChallengeId(), 1, Integer::sum);
            firstBlood.putIfAbsent(s.getChallengeId(), s);
        }

        Map<Long, Challenge> challenges = new LinkedHashMap<>();
        challengeRepository.findByIsPublicTrueOrderByIdAsc().forEach(c -> challenges.put(c.getId(), c));
        challengeRepository.findAllById(solvedTeams.keySet()).forEach(c -> challenges.putIfAbsent(c.getId(), c));

        List<ContestChallengeResult> rows = new ArrayList<>();
        for (Challenge c : challenges.values()) {
            int solved = solvedTeams.getOrDefault(c.getId(), 0);
            Solve first = firstBlood.get(c.getId());
            rows.add(ContestChallengeResult.builder()
                    .contestId(contestId)
                    .challengeId(c.getId())
                    .code(c.getCode())
                    .title(c.getTitle())
                    .category(c.getCategory().name())
                    .maxPoint(c.getMaxPoint())
                    .finalPoint(c.currentPoints(solved))
                    .solvedTeams(solved)
                    .firstBloodTeamId(first == null ? null : first.getTeamId())
                    .firstBloodTeamName(first == null ? null : teamNames.get(first.getTeamId()))
                    .firstBloodAt(first == null ? null : first.getSolvedAt())
                    .build());
        }
        challengeResultRepository.saveAll(rows);
    }

    private static long score(CtfTeamScore s) {
        return s == null ? 0 : s.getTotalScore();
    }

    private static LocalDateTime lastSolvedAt(CtfTeamScore s) {
        return s == null ? null : s.getLastSolvedAt();
    }
}
