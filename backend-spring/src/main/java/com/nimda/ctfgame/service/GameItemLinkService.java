package com.nimda.ctfgame.service;

import com.nimda.ctfgame.dto.AdminGameItemResponse;
import com.nimda.ctfgame.dto.GameItemLinkRequest;
import com.nimda.ctfgame.dto.GameItemLinksResponse;
import com.nimda.ctfgame.dto.GameItemLinksResponse.ChallengeLink;
import com.nimda.ctfgame.dto.GameItemLinksResponse.LinkedItem;
import com.nimda.ctfgame.entity.GameItem;
import com.nimda.ctfgame.exception.GameItemException;
import com.nimda.ctfgame.repository.GameItemRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문제 쪽에서 게임 물건을 연결·해제 (docs/api/CTF 게임 물건 연결 API.md).
 * 규칙: 물건 하나는 문제 하나까지, 문제 하나에는 물건 여러 개. 해제해도 물건은 남는다(장식).
 * 물건 id 를 몰라도 key 만으로 연결하고, 없는 key 면 물건을 만든다.
 */
@Service
@RequiredArgsConstructor
public class GameItemLinkService {

    /** GameItemRequest 의 key 검사와 같다 */
    static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{0,49}$");

    private final GameItemRepository itemRepository;
    private final ChallengeRepository challengeRepository;

    /** 연결 결과. created 면 201 */
    public record LinkResult(AdminGameItemResponse item, boolean created) {
    }

    @Transactional(readOnly = true)
    public GameItemLinksResponse links() {
        List<Challenge> challenges = challengeRepository.findAllByOrderByIdAsc();
        Map<Long, List<GameItem>> byChallenge = itemRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .collect(Collectors.groupingBy(item -> Objects.requireNonNullElse(item.getChallengeId(), 0L)));
        List<ChallengeLink> links = challenges.stream()
                .map(c -> new ChallengeLink(c.getCode(), summaries(byChallenge.getOrDefault(c.getId(), List.of()))))
                .toList();
        // challengeId 가 null(0) 이거나, 가리키는 문제가 없어진 물건은 장식으로 본다
        Map<Long, Challenge> known = challenges.stream().collect(Collectors.toMap(Challenge::getId, Function.identity()));
        List<LinkedItem> unlinked = byChallenge.entrySet().stream()
                .filter(e -> !known.containsKey(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .map(GameItemLinkService::summary)
                .toList();
        return new GameItemLinksResponse(links, unlinked);
    }

    @Transactional(readOnly = true)
    public List<AdminGameItemResponse> itemsOf(String code) {
        Challenge challenge = challenge(code);
        return itemRepository.findByChallengeIdOrderBySortOrderAscIdAsc(challenge.getId()).stream()
                .map(item -> GameItemService.toAdmin(item, challenge))
                .toList();
    }

    @Transactional
    public LinkResult link(String code, String key, GameItemLinkRequest request) {
        requireKey(key);
        Challenge challenge = challenge(code);
        Optional<GameItem> existing = itemRepository.findByItemKey(key);
        if (existing.isEmpty()) {
            if (request.name() == null || request.name().isBlank()) {
                throw new GameItemException(GameItemException.Kind.NAME_REQUIRED);
            }
            GameItem created = itemRepository.save(GameItem.builder()
                    .itemKey(key)
                    .name(request.name().trim())
                    .description(GameItemService.blankToNull(request.description()))
                    .challengeId(challenge.getId())
                    .sortOrder(Objects.requireNonNullElse(request.sortOrder(), 0))
                    .visible(Boolean.TRUE.equals(request.visible()))
                    .build());
            return new LinkResult(GameItemService.toAdmin(created, challenge), true);
        }

        GameItem item = existing.get();
        Long current = item.getChallengeId();
        if (current != null && !current.equals(challenge.getId()) && !Boolean.TRUE.equals(request.moveFromOtherChallenge())) {
            String otherCode = challengeRepository.findById(current).map(Challenge::getCode).orElse(null);
            if (otherCode != null) {   // 가리키던 문제가 없어졌으면 그냥 연결한다
                throw new GameItemException(GameItemException.Kind.LINKED_ELSEWHERE, Map.of("challengeCode", otherCode));
            }
        }
        item.update(
                request.name() == null || request.name().isBlank() ? item.getName() : request.name().trim(),
                request.description() == null ? item.getDescription() : GameItemService.blankToNull(request.description()),
                challenge.getId(),
                request.sortOrder() == null ? item.getSortOrder() : request.sortOrder(),
                request.visible() == null ? Boolean.TRUE.equals(item.getVisible()) : request.visible());
        return new LinkResult(GameItemService.toAdmin(item, challenge), false);
    }

    @Transactional
    public AdminGameItemResponse unlink(String code, String key) {
        requireKey(key);
        Challenge challenge = challenge(code);
        GameItem item = itemRepository.findByItemKey(key)
                .filter(i -> challenge.getId().equals(i.getChallengeId()))
                .orElseThrow(() -> new GameItemException(GameItemException.Kind.NOT_LINKED));
        item.linkTo(null);
        return GameItemService.toAdmin(item, null);
    }

    private Challenge challenge(String code) {
        return challengeRepository.findByCode(code)
                .orElseThrow(() -> new GameItemException(GameItemException.Kind.LINK_CHALLENGE_NOT_FOUND));
    }

    private static void requireKey(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new GameItemException(GameItemException.Kind.INVALID_KEY);
        }
    }

    private static List<LinkedItem> summaries(List<GameItem> items) {
        return items.stream().map(GameItemLinkService::summary).toList();
    }

    private static LinkedItem summary(GameItem item) {
        return new LinkedItem(item.getId(), item.getItemKey(), item.getName(), Boolean.TRUE.equals(item.getVisible()));
    }
}
