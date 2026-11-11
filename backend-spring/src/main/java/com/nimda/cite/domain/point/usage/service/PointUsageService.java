package com.nimda.cite.domain.point.usage.service;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.exception.AlreadyExistException;
import com.nimda.cite.common.exception.InvalidStateException;
import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.domain.board.entity.Board;
import com.nimda.cite.domain.board.enums.BoardStatus;
import com.nimda.cite.domain.board.enums.ShopItemType;
import com.nimda.cite.domain.board.repository.BoardRepository;
import com.nimda.cite.domain.point.entity.PointDetail;
import com.nimda.cite.domain.point.entity.UserBalance;
import com.nimda.cite.domain.point.repositroy.PointDetailRepository;
import com.nimda.cite.domain.point.repositroy.UserBalanceRepository;
import com.nimda.cite.domain.point.usage.dto.PointUsageResponse;
import com.nimda.cite.domain.profiledecoration.entity.ProfileDecoration;
import com.nimda.cite.domain.profiledecoration.ownership.ProfileDecorationOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointUsageService {

    private final BoardRepository boardRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final PointDetailRepository pointDetailRepository;
    private final ProfileDecorationOwnershipService profileDecorationOwnershipService;

    @Transactional
    public PointUsageResponse purchaseBoardItem(Long userId, Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

        validatePurchasable(board);
        validateNotAlreadyOwned(userId, board);
        ShopItemType itemType = resolveItemType(board);

        UserBalance balance = spendBalance(
                userId,
                "아이템 구매: " + board.getTitle(),
                board.getItemPrice()
        );

        return PointUsageResponse.builder()
                .boardId(board.getId())
                .itemName(board.getTitle())
                .price(board.getItemPrice())
                .remainingAmount(balance.getTotalAmount())
                .itemType(itemType.name())
                .profileDecorationKey(grantPurchasedItem(userId, board))
                .build();
    }

    private void validatePurchasable(Board board) {
        if (board.getStatus() != BoardStatus.ACTIVE) {
            throw new InvalidStateException(ErrorCode.ITEM_NOT_PURCHASABLE);
        }
        if (board.getCategory() == null || !Boolean.TRUE.equals(board.getCategory().getShopEnabled())) {
            throw new NotAllowFormException(ErrorCode.NOT_SHOP_ITEM);
        }
        if (board.getItemPrice() == null || board.getItemPrice() <= 0) {
            throw new InvalidStateException(ErrorCode.ITEM_PRICE_NOT_SET);
        }
        if (resolveItemType(board) == ShopItemType.BADGE && board.getProfileDecoration() == null) {
            throw new InvalidStateException(ErrorCode.ITEM_BADGE_NOT_LINKED);
        }
    }

    private void validateNotAlreadyOwned(Long userId, Board board) {
        if (resolveItemType(board) != ShopItemType.BADGE) {
            return;
        }

        ProfileDecoration decoration = board.getProfileDecoration();
        if (profileDecorationOwnershipService.owns(userId, decoration.getId())) {
            throw new AlreadyExistException(ErrorCode.BADGE_ALREADY_OWNED);
        }
    }

    private String grantPurchasedItem(Long userId, Board board) {
        if (resolveItemType(board) != ShopItemType.BADGE) {
            return null;
        }

        ProfileDecoration decoration = board.getProfileDecoration();
        profileDecorationOwnershipService.grant(userId, decoration);
        return decoration.getKey();
    }

    private ShopItemType resolveItemType(Board board) {
        return board.getItemType() == null ? ShopItemType.GENERAL : board.getItemType();
    }

    private UserBalance spendBalance(Long userId, String description, Long pointAmount) {
        if (pointAmount == null || pointAmount <= 0) {
            throw new NotAllowFormException(ErrorCode.INVALID_SPEND_AMOUNT);
        }

        UserBalance balance = userBalanceRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(ErrorCode.BALANCE_NOT_FOUND)
        );

        if (balance.getTotalAmount() < pointAmount) {
            throw new InvalidStateException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        balance.setUpdatedAt(LocalDateTime.now());
        balance.setTotalAmount(balance.getTotalAmount() - pointAmount);

        PointDetail pointDetail = PointDetail.builder()
                .userBalance(balance)
                .amount(-pointAmount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusYears(1L))
                .remainingAmount(balance.getTotalAmount())
                .build();

        pointDetailRepository.save(pointDetail);
        return balance;
    }
}
