package judgeServer.domain.challenge.controller;

import static judgeServer.support.TestUsers.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimda.cite.common.exception.GlobalNimdaException;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.user.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import judgeServer.domain.challenge.dto.ChallengeDetailResponse;
import judgeServer.domain.challenge.dto.ChallengeSummaryResponse;
import judgeServer.domain.challenge.dto.SolvedChallengeResponse;
import judgeServer.domain.challenge.dto.SolvedChallengeRow;
import judgeServer.domain.challenge.dto.SolvedChallengesResponse;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.exposure.ChallengeExposure;
import judgeServer.domain.challenge.exposure.GameLinkedChallenges;
import judgeServer.domain.challenge.enums.IsolationType;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.challenge.service.SolvedChallengeService;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import judgeServer.domain.solve.repository.SolveRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;

/**
 * CTF 사이트 게임 모드 (Figma G07): 참가자는 우리 팀이 푼 문제만, 전체 목록·초안은 ROLE_DEV 만,
 * 순위 비공개 중에는 문제별 점수도 감춘다.
 */
class ChallengeGameModeTest {

    private static final LocalDateTime SOLVED_AT = LocalDateTime.of(2026, 3, 21, 12, 41);

    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final SolveRepository solves = mock(SolveRepository.class);

    private final Challenge open = Challenge.builder().id(1L).code("rev-01").title("패킹된 바이너리").description("설명")
            .category(ChallengeCategory.REVERSING).isPublic(true).build();
    private final Challenge draft = Challenge.builder().id(2L).code("pwn-draft").title("초안").description("x")
            .category(ChallengeCategory.PWN).isPublic(false).build();
    private final SolvedChallengeRow row = new SolvedChallengeRow(1L, "rev-01", "패킹된 바이너리", ChallengeCategory.REVERSING,
            IsolationType.NONE, "challenges/rev-01.zip", 100, SOLVED_AT, "blue1");

    private final Challenge unlinked = Challenge.builder().id(3L).code("misc-01").title("게임에 없음").description("y")
            .category(ChallengeCategory.MISC).isPublic(true).build();

    /** teamId 가 null 이면 팀이 없는 사용자. hidden 이면 순위 비공개 중. 게임 물건 기능은 꺼짐 */
    private ChallengeQueryController controller(Long teamId, boolean hidden) {
        return controller(teamId, hidden, null);
    }

    /** linked: 보이는 게임 물건에 연결된 문제 id. null 이면 게임 물건 기능이 꺼져 있다 (빈 없음) */
    @SuppressWarnings("unchecked")
    private ChallengeQueryController controller(Long teamId, boolean hidden, Set<Long> linked) {
        CtfTeamResolver resolver = new CtfTeamResolver(null, null) {
            @Override
            public Optional<Long> findTeamId(Long userId) {
                return Optional.ofNullable(teamId);
            }
        };
        ContestService contest = new ContestService(null, null, null, null, null) {
            @Override
            public Optional<Long> currentContestId() {
                return Optional.of(9L);
            }

            @Override
            public boolean isScoreboardHiddenNow() {
                return hidden;
            }

            @Override
            public void requireChallengesAccessible(CustomUserDetails user) {
            }
        };
        SolvedChallengeService solved = new SolvedChallengeService(solves, resolver, contest);
        ObjectProvider<GameLinkedChallenges> game = mock(ObjectProvider.class);
        when(game.getIfAvailable()).thenReturn(linked == null ? null : () -> linked);
        ChallengeExposure exposure = new ChallengeExposure(challenges, solved, game);
        return new ChallengeQueryController(challenges, solves, resolver, contest, solved, exposure);
    }

    @SuppressWarnings("unchecked")
    private static <T> T data(Object body) {
        return ((ApiResponse<T>) body).getData();
    }

    private static List<SolvedChallengeResponse> solvedOf(Object body) {
        return ChallengeGameModeTest.<SolvedChallengesResponse>data(body).solved();
    }

    @Test
    void solvedListIsTheTeamsSolvesWithSolverAndTime() {
        when(solves.findSolvedRowsByTeam(3L, 9L)).thenReturn(List.of(row));

        List<SolvedChallengeResponse> list = solvedOf(controller(3L, false).solved(user(7, "ROLE_USER", "ROLE_CTF_USER")).getBody());

        SolvedChallengeResponse s = list.get(0);
        assertEquals("rev-01", s.code());
        assertEquals("blue1", s.solvedBy());
        assertEquals(100, s.awardedPoints());
        assertEquals(ContestService.toKst(SOLVED_AT), s.solvedAt());
        assertTrue(s.hasAttachment());
        verify(solves, never()).findSolvedRowsByUser(anyLong(), any());
    }

    @Test
    void withoutATeamStaffSeesOwnSolves() {
        when(solves.findSolvedRowsByUser(1L, 9L)).thenReturn(List.of(row));

        List<SolvedChallengeResponse> list = solvedOf(controller(null, false).solved(user(1, "ROLE_USER", "ROLE_DEV")).getBody());

        assertEquals(1, list.size());
    }

    @Test
    void participantWithoutTeamIsRefused() {
        assertThrows(GlobalNimdaException.class, () -> controller(null, false).solved(user(7, "ROLE_USER", "ROLE_CTF_USER")));
    }

    @Test
    void pointsAreHiddenFromParticipantsWhileRankingIsHidden() {
        when(solves.findSolvedRowsByTeam(3L, 9L)).thenReturn(List.of(row));

        List<SolvedChallengeResponse> participant = solvedOf(controller(3L, true).solved(user(7, "ROLE_USER")).getBody());
        List<SolvedChallengeResponse> admin = solvedOf(controller(3L, true).solved(user(1, "ROLE_USER", "ROLE_ADMIN")).getBody());

        assertNull(participant.get(0).awardedPoints(), "점수를 더하면 팀 점수가 드러나므로 감춘다");
        assertEquals("blue1", participant.get(0).solvedBy(), "해결한 문제 목록 자체는 계속 보인다");
        assertEquals(100, admin.get(0).awardedPoints());
    }

    @Test
    void fullListIsDevOnlyAndIncludesDraftsAndSolveCounts() {
        when(challenges.findAllByOrderByIdAsc()).thenReturn(List.of(open, draft));
        when(solves.countGroupByChallengeIdInContest(9L)).thenReturn(List.of(new SolveRepository.SolveCount() {
            public Long getChallengeId() {
                return 1L;
            }

            public Long getSolves() {
                return 5L;
            }
        }));

        for (String role : List.of("ROLE_CTF_USER", "ROLE_CTF_MASTER", "ROLE_ADMIN")) {
            assertThrows(AccessDeniedException.class, () -> controller(3L, false).list(user(7, "ROLE_USER", role)), role);
        }
        List<ChallengeSummaryResponse> list = data(controller(null, false).list(user(2, "ROLE_USER", "ROLE_DEV")).getBody());
        assertEquals(2, list.size());
        assertEquals(5, list.get(0).solveCount());
        assertEquals(0, list.get(1).solveCount());
        assertFalse(list.get(1).isPublic());
    }

    @Test
    void detailTellsWhetherMyTeamSolvedIt() {
        when(challenges.findByCode("rev-01")).thenReturn(Optional.of(open));
        when(solves.findSolvedRowsByTeam(3L, 9L)).thenReturn(List.of(row));
        when(solves.findSolvedRowsByTeam(4L, 9L)).thenReturn(List.of());

        ChallengeDetailResponse mine = data(controller(3L, false).detail("rev-01", user(7, "ROLE_USER")).getBody());
        ChallengeDetailResponse other = data(controller(4L, false).detail("rev-01", user(8, "ROLE_USER")).getBody());

        assertTrue(mine.solvedByMyTeam());
        assertEquals("blue1", mine.mySolve().solvedBy());
        assertFalse(other.solvedByMyTeam(), "안 푼 팀도 상세는 받는다 (게임의 문제 창)");
        assertNull(other.mySolve());
    }

    @Test
    void draftsAreDevOnlyEvenForAdmin() {
        when(challenges.findByCode("pwn-draft")).thenReturn(Optional.of(draft));
        when(solves.findSolvedRowsByUser(anyLong(), any())).thenReturn(List.of());

        assertThrows(GlobalNimdaException.class, () -> controller(null, false).detail("pwn-draft", user(1, "ROLE_USER", "ROLE_ADMIN")));
        assertThrows(GlobalNimdaException.class, () -> controller(3L, false).detail("pwn-draft", user(7, "ROLE_USER")));
        ChallengeDetailResponse dev = data(controller(null, false).detail("pwn-draft", user(2, "ROLE_USER", "ROLE_DEV")).getBody());
        assertEquals("pwn-draft", dev.code());
    }

    // ── 노출 규칙: 참가자는 게임에 나온 문제 또는 우리 팀이 푼 문제만 (11-18) ──

    @Test
    void totalCountsPublicChallengesShownInTheGame() {
        when(challenges.findAllByOrderByIdAsc()).thenReturn(List.of(open, draft, unlinked));
        when(solves.findSolvedRowsByTeam(3L, 9L)).thenReturn(List.of());

        SolvedChallengesResponse gameOn = data(controller(3L, false, Set.of(1L, 2L)).solved(user(7, "ROLE_USER")).getBody());
        SolvedChallengesResponse gameOff = data(controller(3L, false).solved(user(7, "ROLE_USER")).getBody());

        assertEquals(1, gameOn.total(), "초안(2)은 연결돼 있어도, 게임에 없는 공개 문제(3)도 세지 않는다");
        assertEquals(2, gameOff.total(), "게임 물건 기능이 꺼져 있으면 공개 문제 전부");
    }

    @Test
    void participantsCannotOpenPublicChallengesThatAreNotInTheGame() {
        when(challenges.findByCode("misc-01")).thenReturn(Optional.of(unlinked));
        when(challenges.findByCode("rev-01")).thenReturn(Optional.of(open));
        when(solves.findSolvedRowsByTeam(4L, 9L)).thenReturn(List.of());
        ChallengeQueryController c = controller(4L, false, Set.of(1L));

        for (String role : List.of("ROLE_CTF_USER", "ROLE_CTF_MASTER")) {
            assertThrows(GlobalNimdaException.class, () -> c.detail("misc-01", user(8, "ROLE_USER", role)), role);
        }
        assertEquals("rev-01", ChallengeGameModeTest.<ChallengeDetailResponse>data(
                c.detail("rev-01", user(8, "ROLE_USER", "ROLE_CTF_USER")).getBody()).code());
        assertEquals("misc-01", ChallengeGameModeTest.<ChallengeDetailResponse>data(
                c.detail("misc-01", user(1, "ROLE_USER", "ROLE_ADMIN")).getBody()).code(), "ADMIN 은 공개 문제 전부");
    }

    @Test
    void solvedChallengesStayOpenAfterTheirItemIsHidden() {
        SolvedChallengeRow solvedUnlinked = new SolvedChallengeRow(3L, "misc-01", "게임에 없음", ChallengeCategory.MISC,
                IsolationType.NONE, null, 50, SOLVED_AT, "blue1");
        when(challenges.findByCode("misc-01")).thenReturn(Optional.of(unlinked));
        when(solves.findSolvedRowsByTeam(3L, 9L)).thenReturn(List.of(solvedUnlinked));

        ChallengeDetailResponse d = data(controller(3L, false, Set.of()).detail("misc-01", user(7, "ROLE_USER")).getBody());

        assertTrue(d.solvedByMyTeam());
    }

    @Test
    void linkedDraftsStayHiddenFromParticipants() {
        when(challenges.findByCode("pwn-draft")).thenReturn(Optional.of(draft));
        when(solves.findSolvedRowsByTeam(3L, 9L)).thenReturn(List.of());

        assertThrows(GlobalNimdaException.class,
                () -> controller(3L, false, Set.of(2L)).detail("pwn-draft", user(7, "ROLE_USER", "ROLE_CTF_USER")));
    }

    @Test
    void currentPointsAreHiddenFromParticipantsWhileRankingIsHidden() {
        when(challenges.findByCode("rev-01")).thenReturn(Optional.of(open));
        when(solves.findSolvedRowsByTeam(3L, 9L)).thenReturn(List.of());
        when(solves.findSolvedRowsByUser(anyLong(), any())).thenReturn(List.of());

        ChallengeDetailResponse participant = data(controller(3L, true).detail("rev-01", user(7, "ROLE_USER", "ROLE_CTF_USER")).getBody());
        ChallengeDetailResponse admin = data(controller(null, true).detail("rev-01", user(1, "ROLE_USER", "ROLE_ADMIN")).getBody());
        ChallengeDetailResponse open = data(controller(3L, false).detail("rev-01", user(7, "ROLE_USER", "ROLE_CTF_USER")).getBody());

        assertNull(participant.points(), "max·decrease 와 함께 보면 해결 팀 수가 드러나므로 감춘다");
        assertTrue(admin.points() != null);
        assertTrue(open.points() != null);
    }
}
