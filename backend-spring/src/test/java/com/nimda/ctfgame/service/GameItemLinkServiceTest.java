package com.nimda.ctfgame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimda.ctfgame.dto.GameItemLinkRequest;
import com.nimda.ctfgame.dto.GameItemLinksResponse;
import com.nimda.ctfgame.entity.GameItem;
import com.nimda.ctfgame.exception.GameItemException;
import com.nimda.ctfgame.repository.GameItemRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 문제 쪽 연결 API 규칙 (docs/api/CTF 게임 물건 연결 API.md 1장). */
class GameItemLinkServiceTest {

    private final GameItemRepository items = mock(GameItemRepository.class);
    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final GameItemLinkService service = new GameItemLinkService(items, challenges);

    private final Challenge rev = Challenge.builder().id(1L).code("rev-01").title("패킹된 바이너리").isPublic(true).build();
    private final Challenge web = Challenge.builder().id(2L).code("web-01").title("첫 번째 웹 문제").isPublic(false).build();

    private static GameItem item(long id, String key, Long challengeId, boolean visible) {
        GameItem item = GameItem.builder().itemKey(key).name(key + " 이름").description("이야기").challengeId(challengeId)
                .sortOrder(0).visible(visible).build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private static GameItemLinkRequest req(String name, Boolean visible, Boolean move) {
        return new GameItemLinkRequest(name, null, visible, null, move);
    }

    {
        when(challenges.findByCode("rev-01")).thenReturn(Optional.of(rev));
        when(challenges.findByCode("web-01")).thenReturn(Optional.of(web));
        when(challenges.findById(1L)).thenReturn(Optional.of(rev));
        when(challenges.findById(2L)).thenReturn(Optional.of(web));
        when(items.save(any(GameItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void unknownKeyCreatesTheItemLinkedToTheChallenge() {
        when(items.findByItemKey("old-arcade")).thenReturn(Optional.empty());

        GameItemLinkService.LinkResult r = service.link("rev-01", "old-arcade", req(" 낡은 게임기 ", true, null));

        assertTrue(r.created());
        assertEquals("old-arcade", r.item().key());
        assertEquals("낡은 게임기", r.item().name());
        assertEquals("rev-01", r.item().challengeCode());
        assertTrue(r.item().visible());
    }

    @Test
    void newItemNeedsAName() {
        when(items.findByItemKey("old-arcade")).thenReturn(Optional.empty());
        GameItemException e = assertThrows(GameItemException.class, () -> service.link("rev-01", "old-arcade", req(" ", null, null)));
        assertEquals(GameItemException.Kind.NAME_REQUIRED, e.kind());
        verify(items, never()).save(any());
    }

    @Test
    void existingUnlinkedItemIsLinkedAndBlankFieldsAreKept() {
        GameItem poster = item(3, "lobby-poster", null, true);
        when(items.findByItemKey("lobby-poster")).thenReturn(Optional.of(poster));

        GameItemLinkService.LinkResult r = service.link("rev-01", "lobby-poster", req(null, null, null));

        assertFalse(r.created());
        assertEquals(1L, poster.getChallengeId());
        assertEquals("lobby-poster 이름", poster.getName(), "비운 이름은 그대로");
        assertEquals("이야기", poster.getDescription(), "비운 이야기는 그대로");
        assertTrue(poster.getVisible(), "비운 공개 여부는 그대로");
    }

    @Test
    void itemLinkedToAnotherChallengeIsRefusedUnlessMoved() {
        GameItem arcade = item(1, "old-arcade", 2L, true);
        when(items.findByItemKey("old-arcade")).thenReturn(Optional.of(arcade));

        GameItemException e = assertThrows(GameItemException.class, () -> service.link("rev-01", "old-arcade", req(null, null, null)));
        assertEquals(GameItemException.Kind.LINKED_ELSEWHERE, e.kind());
        assertEquals(Map.of("challengeCode", "web-01"), e.data(), "어느 문제에 연결돼 있는지 알려 준다");
        assertEquals(2L, arcade.getChallengeId());

        service.link("rev-01", "old-arcade", req(null, null, true));
        assertEquals(1L, arcade.getChallengeId(), "확인하면 옮긴다");
    }

    @Test
    void relinkingToTheSameChallengeIsFine() {
        GameItem arcade = item(1, "old-arcade", 1L, false);
        when(items.findByItemKey("old-arcade")).thenReturn(Optional.of(arcade));

        service.link("rev-01", "old-arcade", req("새 이름", true, null));

        assertEquals("새 이름", arcade.getName());
        assertTrue(arcade.getVisible());
    }

    @Test
    void unlinkKeepsTheItem() {
        GameItem arcade = item(1, "old-arcade", 1L, true);
        when(items.findByItemKey("old-arcade")).thenReturn(Optional.of(arcade));

        assertNull(service.unlink("rev-01", "old-arcade").challengeCode());
        assertNull(arcade.getChallengeId());
        verify(items, never()).delete(any());

        GameItemException e = assertThrows(GameItemException.class, () -> service.unlink("web-01", "old-arcade"));
        assertEquals(GameItemException.Kind.NOT_LINKED, e.kind());
    }

    @Test
    void badKeyAndUnknownChallengeAreRejected() {
        assertEquals(GameItemException.Kind.INVALID_KEY,
                assertThrows(GameItemException.class, () -> service.link("rev-01", "Bad Key", req("x", null, null))).kind());
        when(challenges.findByCode("nope")).thenReturn(Optional.empty());
        GameItemException missing = assertThrows(GameItemException.class, () -> service.itemsOf("nope"));
        assertEquals(GameItemException.Kind.LINK_CHALLENGE_NOT_FOUND, missing.kind());
        assertEquals(404, missing.status().value());
    }

    @Test
    void linksListEveryChallengeAndTheDecorations() {
        when(challenges.findAllByOrderByIdAsc()).thenReturn(List.of(rev, web));
        when(items.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                item(1, "old-arcade", 1L, true), item(2, "arcade-2", 1L, false), item(3, "lobby-poster", null, true),
                item(4, "orphan", 99L, true)));

        GameItemLinksResponse r = service.links();

        assertEquals(List.of("rev-01", "web-01"), r.links().stream().map(GameItemLinksResponse.ChallengeLink::challengeCode).toList());
        assertEquals(List.of("old-arcade", "arcade-2"), r.links().get(0).items().stream().map(GameItemLinksResponse.LinkedItem::key).toList());
        assertTrue(r.links().get(1).items().isEmpty(), "초안 문제도 빈 목록으로 나온다");
        assertEquals(List.of("lobby-poster", "orphan"), r.unlinkedItems().stream().map(GameItemLinksResponse.LinkedItem::key).toList(),
                "연결 없음 + 없어진 문제를 가리키는 물건은 장식");
    }
}
