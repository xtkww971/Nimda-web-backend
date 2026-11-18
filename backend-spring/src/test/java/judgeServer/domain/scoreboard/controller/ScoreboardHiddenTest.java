package judgeServer.domain.scoreboard.controller;

import static judgeServer.support.TestUsers.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimda.cite.common.response.ApiResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import judgeServer.domain.contest.entity.Contest;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.scoreboard.dto.MyTeamScoreResponse;
import judgeServer.domain.scoreboard.dto.TeamScoreboardPageResponse;
import judgeServer.domain.scoreboard.service.ScoreboardService;
import org.junit.jupiter.api.Test;

/** 순위 비공개: 종료 N분 전 ~ 종료 동안 참가자는 순위·점수를 하나도 받지 못하고, ADMIN·DEV 는 언제나 전체를 받는다. */
class ScoreboardHiddenTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 3, 21, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 3, 22, 18, 0);

    private final TeamScoreboardPageResponse page = new TeamScoreboardPageResponse(List.of(), 0, 50, 0, 0, 0);
    private final MyTeamScoreResponse myTeam = new MyTeamScoreResponse(3L, "Segfault", 4L, 12, 300, 3, 3, null, List.of(), List.of());

    private ScoreboardController controller(boolean freeze, LocalDateTime now) {
        Contest contest = Contest.builder().id(1L).name("NIMDACON").startsAt(START).endsAt(END)
                .cleanupInstancesOnEnd(true).challengesVisibleAfterEnd(true)
                .freezeScoreboard(freeze).freezeMinutesBeforeEnd(120).createdAt(START).updatedAt(START).build();
        ContestService contestService = new ContestService(null, null, null, null, null) {
            @Override
            public Optional<Contest> current() {
                return Optional.of(contest);
            }

            @Override
            public LocalDateTime now() {
                return now;
            }
        };
        ScoreboardService scoreboardService = new ScoreboardService(null, null, null, null, null) {
            @Override
            public TeamScoreboardPageResponse getScoreboard(int p, int size) {
                return page;
            }

            @Override
            public Optional<MyTeamScoreResponse> getMyTeam(Long userId) {
                return Optional.of(myTeam);
            }
        };
        return new ScoreboardController(scoreboardService, contestService);
    }

    @SuppressWarnings("unchecked")
    private static Object data(Object responseBody) {
        return ((ApiResponse<Object>) responseBody).getData();
    }

    @Test
    void participantSeesNothingWhileHidden() {
        ScoreboardController c = controller(true, END.minusMinutes(30));

        Map<String, Object> board = (Map<String, Object>) data(c.scoreboard(0, 50, user(7, "ROLE_USER", "ROLE_CTF_USER")).getBody());
        Map<String, Object> mine = (Map<String, Object>) data(c.myTeam(user(7, "ROLE_USER", "ROLE_CTF_USER")).getBody());

        for (Map<String, Object> body : List.of(board, mine)) {
            assertEquals(true, body.get("hidden"));
            assertEquals(ContestService.toKst(END), body.get("revealAt"));
            assertEquals(ContestService.toKst(END.minusMinutes(120)), body.get("hiddenFrom"));
            assertEquals(3, body.size(), "순위·점수·팀 정보가 섞여 나가지 않는다: " + body);
        }
    }

    @Test
    void staffAlwaysSeesEverything() {
        ScoreboardController c = controller(true, END.minusMinutes(30));

        assertEquals(page, data(c.scoreboard(0, 50, user(1, "ROLE_USER", "ROLE_ADMIN")).getBody()));
        assertEquals(page, data(c.scoreboard(0, 50, user(2, "ROLE_USER", "ROLE_DEV")).getBody()));
        assertEquals(myTeam, ((Map<?, ?>) data(c.myTeam(user(2, "ROLE_USER", "ROLE_DEV")).getBody())).get("team"));
    }

    @Test
    void ctfMasterIsAParticipantForRanking() {
        ScoreboardController c = controller(true, END.minusMinutes(30));
        Object body = data(c.scoreboard(0, 50, user(5, "ROLE_USER", "ROLE_CTF_MASTER")).getBody());
        assertTrue(body instanceof Map<?, ?> m && Boolean.TRUE.equals(m.get("hidden")));
    }

    @Test
    void visibleBeforeHidingAfterEndAndWhenTurnedOff() {
        assertEquals(page, data(controller(true, END.minusMinutes(121)).scoreboard(0, 50, user(7, "ROLE_USER")).getBody()));
        assertEquals(page, data(controller(true, END).scoreboard(0, 50, user(7, "ROLE_USER")).getBody()), "종료와 함께 공개");
        assertEquals(page, data(controller(false, END.minusMinutes(30)).scoreboard(0, 50, user(7, "ROLE_USER")).getBody()));
        Object mine = data(controller(false, END.minusMinutes(30)).myTeam(user(7, "ROLE_USER")).getBody());
        assertFalse(((Map<?, ?>) mine).containsKey("hidden"));
    }
}
