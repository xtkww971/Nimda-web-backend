package com.nimda.ctfgame.service;

import com.nimda.ctfgame.repository.GameItemRepository;
import com.nimda.ctfgame.exception.GameItemException;
import com.nimda.ctfgame.entity.GameItem;
import com.nimda.ctfgame.dto.AdminGameItemResponse;
import com.nimda.ctfgame.dto.GameItemRequest;
import com.nimda.ctfgame.dto.GameItemResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 물건 조회·관리. 소유는 저장하지 않고 정답 기록으로 계산한다 ({@link SolvedChallenges}).
 * 이야기(description)는 가진 팀에게만 보낸다.
 */
@Service
@RequiredArgsConstructor
public class GameItemService {

    private final GameItemRepository itemRepository;
    private final ChallengeRepository challengeRepository;
    private final SolvedChallenges solvedChallenges;

    /** 참가자(게임)용: 보이는 물건과 이 사용자 팀의 소유 여부. staff 는 공개 전 문제의 코드도 받는다. */
    @Transactional(readOnly = true)
    public List<GameItemResponse> listFor(Long userId, boolean staff) {
        List<GameItem> items = itemRepository.findByVisibleTrueOrderBySortOrderAscIdAsc();
        Map<Long, Challenge> challenges = challengesOf(items);
        Set<Long> solved = solvedChallenges.of(userId);
        return items.stream().map(item -> {
            Challenge challenge = item.getChallengeId() == null ? null : challenges.get(item.getChallengeId());
            boolean openable = challenge != null && (staff || Boolean.TRUE.equals(challenge.getIsPublic()));
            boolean owned = challenge != null && solved.contains(challenge.getId());
            return new GameItemResponse(item.getItemKey(), item.getName(), item.getZone(), item.getX(), item.getY(),
                    item.getSortOrder(),
                    openable ? challenge.getCode() : null,
                    openable ? challenge.getTitle() : null,
                    owned,
                    owned ? item.getDescription() : null);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminGameItemResponse> listAll() {
        List<GameItem> items = itemRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, Challenge> challenges = challengesOf(items);
        // Map.of() 는 get(null) 에서 예외를 던지므로 연결 없는 물건은 따로 본다
        return items.stream()
                .map(item -> toAdmin(item, item.getChallengeId() == null ? null : challenges.get(item.getChallengeId())))
                .toList();
    }

    @Transactional
    public AdminGameItemResponse create(GameItemRequest request) {
        if (itemRepository.existsByItemKey(request.key())) {
            throw new GameItemException(GameItemException.Kind.DUPLICATE_KEY);
        }
        Challenge challenge = findChallenge(request.challengeCode());
        GameItem saved = itemRepository.save(GameItem.builder()
                .itemKey(request.key())
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .challengeId(challenge == null ? null : challenge.getId())
                .zone(request.zone())
                .x(request.x())
                .y(request.y())
                .sortOrder(Objects.requireNonNullElse(request.sortOrder(), 0))
                .visible(Boolean.TRUE.equals(request.visible()))
                .build());
        return toAdmin(saved, challenge);
    }

    @Transactional
    public AdminGameItemResponse update(Long id, GameItemRequest request) {
        GameItem item = itemRepository.findById(id).orElseThrow(() -> new GameItemException(GameItemException.Kind.NOT_FOUND));
        if (!item.getItemKey().equals(request.key())) {
            throw new GameItemException(GameItemException.Kind.KEY_CHANGED);
        }
        Challenge challenge = findChallenge(request.challengeCode());
        item.update(request.name().trim(), blankToNull(request.description()), challenge == null ? null : challenge.getId(),
                request.zone(), request.x(), request.y(),
                Objects.requireNonNullElse(request.sortOrder(), 0), Boolean.TRUE.equals(request.visible()));
        return toAdmin(item, challenge);
    }

    @Transactional
    public void delete(Long id) {
        GameItem item = itemRepository.findById(id).orElseThrow(() -> new GameItemException(GameItemException.Kind.NOT_FOUND));
        itemRepository.delete(item);
    }

    private Challenge findChallenge(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return challengeRepository.findByCode(code.trim())
                .orElseThrow(() -> new GameItemException(GameItemException.Kind.CHALLENGE_NOT_FOUND));
    }

    private Map<Long, Challenge> challengesOf(Collection<GameItem> items) {
        Set<Long> ids = items.stream().map(GameItem::getChallengeId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return challengeRepository.findAllById(ids).stream().collect(Collectors.toMap(Challenge::getId, Function.identity()));
    }

    private static AdminGameItemResponse toAdmin(GameItem item, Challenge challenge) {
        return new AdminGameItemResponse(item.getId(), item.getItemKey(), item.getName(), item.getDescription(),
                challenge == null ? null : challenge.getCode(), challenge == null ? null : challenge.getTitle(),
                item.getZone(), item.getX(), item.getY(), item.getSortOrder(), Boolean.TRUE.equals(item.getVisible()),
                item.getUpdatedAt());
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
