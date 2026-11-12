package judgeServer.domain.contest.service;

import com.nimda.cite.user.entity.User;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.ChallengeCategory;
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
import judgeServer.domain.groupMember.enums.MemberRole;
import judgeServer.domain.scoreboard.entity.CtfTeamScore;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import judgeServer.domain.solve.entity.Solve;
import judgeServer.domain.solve.repository.SolveRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class ContestSnapshotServiceTest {

    private static final long CONTEST = 9L;
    private static final LocalDateTime T = LocalDateTime.of(2026, 3, 21, 12, 0);

    private final ContestResultRepository resultRepository = mock(ContestResultRepository.class);
    private final ContestResultMemberRepository memberRepository = mock(ContestResultMemberRepository.class);
    private final ContestChallengeResultRepository challengeResultRepository = mock(ContestChallengeResultRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final CtfTeamScoreRepository teamScoreRepository = mock(CtfTeamScoreRepository.class);
    private final SolveRepository solveRepository = mock(SolveRepository.class);
    private final ChallengeRepository challengeRepository = mock(ChallengeRepository.class);
    private final ContestSnapshotService service = new ContestSnapshotService(resultRepository, memberRepository,
            challengeResultRepository, groupRepository, teamScoreRepository, solveRepository, challengeRepository);

    @Test
    @SuppressWarnings("unchecked")
    void writesStandingsMembersAndFirstBlood() {
        User yuna = user(1L, "yuna");
        User taeho = user(2L, "taeho");
        User minsu = user(3L, "minsu");
        Group segfault = team(10L, "Segfault", yuna, MemberRole.LEADER, taeho, MemberRole.MEMBER);
        Group nimda = team(11L, "NIMDA-A", minsu, MemberRole.LEADER);
        Group empty = team(12L, "Byte Me");
        when(groupRepository.findAllWithMembersInContest(CONTEST)).thenReturn(List.of(segfault, nimda, empty));

        // 동점(150)이면 마지막 득점이 이른 팀이 위
        when(teamScoreRepository.findAllById(anyCollection())).thenReturn(List.of(
                score(10L, 150, 2, T.plusMinutes(10)),
                score(11L, 150, 1, T.plusMinutes(5))));

        Challenge web = challenge(100L, "web-01", 100, 0, 10);
        Challenge pwn = challenge(101L, "pwn-01", 200, 0, 10);
        when(challengeRepository.findByIsPublicTrueOrderByIdAsc()).thenReturn(List.of(web, pwn));
        when(challengeRepository.findAllById(anyCollection())).thenReturn(List.of(web));
        when(solveRepository.findByContestIdOrderBySolvedAtAscIdAsc(CONTEST)).thenReturn(List.of(
                solve(100L, 3L, 11L, 150, T.plusMinutes(5)),   // NIMDA-A First Blood
                solve(100L, 1L, 10L, 90, T.plusMinutes(7)),
                solve(101L, 2L, 10L, 60, T.plusMinutes(10)))); // pwn 은 공개 목록에도 있다

        service.write(CONTEST);

        verify(resultRepository).deleteByContestId(CONTEST);
        ArgumentCaptor<List<ContestResult>> results = ArgumentCaptor.forClass(List.class);
        verify(resultRepository).saveAll(results.capture());
        List<ContestResult> standings = results.getValue();
        assertEquals(List.of("NIMDA-A", "Segfault", "Byte Me"), standings.stream().map(ContestResult::getTeamName).toList());
        assertEquals(1, standings.get(0).getTeamRank());
        assertEquals(2, standings.get(1).getTeamRank());
        assertNull(standings.get(2).getTeamRank(), "점수가 없는 팀은 순위가 없다");
        assertEquals(2, standings.get(1).getMemberCount());

        ArgumentCaptor<List<ContestResultMember>> members = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(members.capture());
        Map<String, ContestResultMember> byNick = members.getValue().stream()
                .collect(Collectors.toMap(ContestResultMember::getNickname, Function.identity()));
        assertEquals(90L, byNick.get("yuna").getPoints());
        assertEquals("LEADER", byNick.get("yuna").getRole());
        assertEquals(60L, byNick.get("taeho").getPoints());
        assertEquals(150L, byNick.get("minsu").getPoints());

        ArgumentCaptor<List<ContestChallengeResult>> challenges = ArgumentCaptor.forClass(List.class);
        verify(challengeResultRepository).saveAll(challenges.capture());
        ContestChallengeResult webResult = challenges.getValue().stream()
                .filter(c -> c.getChallengeId().equals(100L)).findFirst().orElseThrow();
        assertEquals(2, webResult.getSolvedTeams());
        assertEquals(80, webResult.getFinalPoint());
        assertEquals("NIMDA-A", webResult.getFirstBloodTeamName());
        assertEquals(2, challenges.getValue().size());
    }

    private static User user(Long id, String nickname) {
        User u = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static Group team(Long id, String name, Object... memberAndRole) {
        Group g = Group.builder().name(name).capacity(5).contestId(CONTEST).build();
        ReflectionTestUtils.setField(g, "id", id);
        for (int i = 0; i < memberAndRole.length; i += 2) {
            g.addMember(GroupMember.builder().group(g).user((User) memberAndRole[i])
                    .role((MemberRole) memberAndRole[i + 1]).build());
        }
        return g;
    }

    // 엔티티는 생성자가 protected라 리플렉션으로 만든다 (Java 24에서는 Mockito로 mock할 수 없다)
    private static CtfTeamScore score(Long teamId, long total, int solves, LocalDateTime last) {
        try {
            var ctor = CtfTeamScore.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            CtfTeamScore s = ctor.newInstance();
            ReflectionTestUtils.setField(s, "teamId", teamId);
            ReflectionTestUtils.setField(s, "totalScore", total);
            ReflectionTestUtils.setField(s, "solveCount", solves);
            ReflectionTestUtils.setField(s, "lastSolvedAt", last);
            return s;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Challenge challenge(Long id, String code, int max, int min, int decrease) {
        return Challenge.builder().id(id).code(code).title(code).category(ChallengeCategory.WEB)
                .maxPoint(max).minPoint(min).decreasePoint(decrease).isPublic(true).build();
    }

    private static Solve solve(Long challengeId, Long userId, Long teamId, int points, LocalDateTime at) {
        return Solve.builder().challengeId(challengeId).userId(userId).teamId(teamId).contestId(CONTEST)
                .awardedPoints(points).solvedAt(at).build();
    }
}
