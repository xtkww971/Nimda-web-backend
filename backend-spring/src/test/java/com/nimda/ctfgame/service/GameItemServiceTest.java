package com.nimda.ctfgame.service;

import com.nimda.ctfgame.repository.GameItemRepository;
import com.nimda.ctfgame.exception.GameItemException;
import com.nimda.ctfgame.entity.GameItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimda.ctfgame.dto.AdminGameItemResponse;
import com.nimda.ctfgame.dto.GameItemRequest;
import com.nimda.ctfgame.dto.GameItemResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import org.junit.jupiter.api.Test;

/** 소유(팀 정답)에 따라 이야기를 보여 주는 규칙과 관리 API 의 검증. 정답 집합은 람다로 바꿔 끼운다. */
class GameItemServiceTest {

    private final GameItemRepository items = mock(GameItemRepository.class);
    private final ChallengeRepository challenges = mock(ChallengeRepository.class);

    private final Challenge solvedChallenge = Challenge.builder().id(1L).code("rev-01").title("회장님 최애 게임").isPublic(true).build();
    private final Challenge openChallenge = Challenge.builder().id(2L).code("web-01").title("동방 출입문").isPublic(true).build();
    private final Challenge draftChallenge = Challenge.builder().id(3L).code("pwn-draft").title("초안").isPublic(false).build();

    private GameItem item(String key, Long challengeId, String description) {
        return GameItem.builder().itemKey(key).name(key + " 이름").description(description).challengeId(challengeId)
                .sortOrder(0).visible(true).build();
    }

    private GameItemService service(Set<Long> solved) {
        return new GameItemService(items, challenges, userId -> solved);
    }

    @Test
    void storyIsSentOnlyWhenTheTeamSolvedTheChallenge() {
        when(items.findByVisibleTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                item("old-arcade", 1L, "회장의 게임팩 이야기"), item("door", 2L, "문 이야기")));
        when(challenges.findAllById(anyIterable())).thenReturn(List.of(solvedChallenge, openChallenge));

        List<GameItemResponse> result = service(Set.of(1L)).listFor(7L, false);

        GameItemResponse owned = result.get(0), locked = result.get(1);
        assertTrue(owned.owned());
        assertEquals("회장의 게임팩 이야기", owned.description());
        assertEquals("rev-01", owned.challengeCode());
        assertFalse(locked.owned());
        assertNull(locked.description(), "아직 못 푼 물건의 이야기는 보내지 않는다");
        assertEquals("web-01", locked.challengeCode(), "못 풀어도 문제는 열 수 있어야 한다");
        assertEquals("door 이름", locked.name());
    }

    @Test
    void draftChallengeCodeIsHiddenFromParticipantsButShownToStaff() {
        when(items.findByVisibleTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(item("secret", 3L, "x")));
        when(challenges.findAllById(anyIterable())).thenReturn(List.of(draftChallenge));

        assertNull(service(Set.of()).listFor(7L, false).get(0).challengeCode());
        assertEquals("pwn-draft", service(Set.of()).listFor(7L, true).get(0).challengeCode());
    }

    @Test
    void itemWithoutChallengeIsNeverOwned() {
        when(items.findByVisibleTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(item("poster", null, "포스터 이야기")));

        GameItemResponse poster = service(Set.of(1L, 2L)).listFor(7L, false).get(0);

        assertFalse(poster.owned());
        assertNull(poster.description());
        assertNull(poster.challengeCode());
        verify(challenges, never()).findAllById(anyIterable());
    }

    @Test
    void createLinksChallengeByCode() {
        when(items.existsByItemKey("old-arcade")).thenReturn(false);
        when(challenges.findByCode("rev-01")).thenReturn(Optional.of(solvedChallenge));
        when(items.save(any(GameItem.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminGameItemResponse created = service(Set.of()).create(new GameItemRequest(
                "old-arcade", " 낡은 게임기 ", "이야기", "rev-01", null, true));

        assertEquals("낡은 게임기", created.name());
        assertEquals("rev-01", created.challengeCode());
        assertEquals(0, created.sortOrder());
        assertTrue(created.visible());
    }

    @Test
    void createRejectsDuplicateKeyAndUnknownChallenge() {
        when(items.existsByItemKey("dup")).thenReturn(true);
        GameItemException duplicate = assertThrows(GameItemException.class, () -> service(Set.of()).create(
                new GameItemRequest("dup", "이름", null, null, 0, false)));
        assertEquals(GameItemException.Kind.DUPLICATE_KEY, duplicate.kind());

        when(items.existsByItemKey("new")).thenReturn(false);
        when(challenges.findByCode("nope")).thenReturn(Optional.empty());
        GameItemException unknown = assertThrows(GameItemException.class, () -> service(Set.of()).create(
                new GameItemRequest("new", "이름", null, "nope", 0, false)));
        assertEquals(GameItemException.Kind.CHALLENGE_NOT_FOUND, unknown.kind());
        verify(items, never()).save(any());
    }

    @Test
    void updateKeepsTheKeyAndCanUnlinkTheChallenge() {
        GameItem existing = item("old-arcade", 1L, "이야기");
        when(items.findById(5L)).thenReturn(Optional.of(existing));

        GameItemException changed = assertThrows(GameItemException.class, () -> service(Set.of()).update(5L,
                new GameItemRequest("renamed", "이름", null, null, 0, true)));
        assertEquals(GameItemException.Kind.KEY_CHANGED, changed.kind());

        AdminGameItemResponse updated = service(Set.of()).update(5L,
                new GameItemRequest("old-arcade", "새 이름", "  ", "", 3, false));
        assertNull(updated.challengeCode());
        assertNull(updated.description(), "빈 이야기는 null 로 저장한다");
        assertEquals(3, updated.sortOrder());
        assertFalse(updated.visible());
    }

    @Test
    void adminListIncludesHiddenAndUnlinkedItems() {
        GameItem hidden = GameItem.builder().itemKey("box").name("상자").visible(false).build();
        when(items.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(hidden, item("poster", null, "포스터")));

        List<AdminGameItemResponse> all = service(Set.of()).listAll();

        assertEquals(2, all.size());
        assertFalse(all.get(0).visible());
        assertNull(all.get(1).challengeCode());
        assertEquals("포스터", all.get(1).description(), "관리자는 이야기를 늘 본다");
    }

    @Test
    void missingItemIs404Kind() {
        when(items.findById(9L)).thenReturn(Optional.empty());
        GameItemException missing = assertThrows(GameItemException.class, () -> service(Set.of()).delete(9L));
        assertEquals(GameItemException.Kind.NOT_FOUND, missing.kind());
    }
}
