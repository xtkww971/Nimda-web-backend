package com.nimda.cite.domain.point.service;

import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.errorcode.domain.PointErrorCode;
import com.nimda.cite.common.exception.errorcode.user.UserErrorCode;
import com.nimda.cite.domain.point.dto.ManualBalanceUpdateRequest;
import com.nimda.cite.domain.point.entity.PointDetail;
import com.nimda.cite.domain.point.entity.UserBalance;
import com.nimda.cite.domain.point.enums.PointTypes;
import com.nimda.cite.domain.point.repositroy.PointDetailRepository;
import com.nimda.cite.domain.point.repositroy.UserBalanceRepository;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserBalanceRepository userBalanceRepository;
    private final PointDetailRepository pointDetailRepository;
    private final UserRepository userRepository;

    // 계좌 삭제 시 cascade를 직접 하는 것이 좀 더 나은 선택임
    // softDelete 를 할 수 있기 때문
    @Transactional
    public void deleteBalance(Long userId) {

    }

    // 회원가입 시 계좌 생성
    @Transactional
    public void createBalance(Long userId) {
        Optional<UserBalance> balance = userBalanceRepository.findById(userId);

        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND)
        );

        Optional<UserBalance> userBalance = userBalanceRepository.findById(userId);

        if(userBalance.isEmpty()) {
            UserBalance newBalance = UserBalance.builder()
                    .user(user)
                    .updatedAt(LocalDateTime.now())
                    .totalAmount(0L)
                    .build();
        }
    }

    // 자동 적립 - 출석, 알고리즘 풀이, 스터디 참여
    @Transactional
    public UserBalance updateBalanceAuto(Long userId, PointTypes type) {
        // 최초 출석 시 계좌가 존재하지 않기 때문에 생성해야 함
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND)
        );
        UserBalance balance = userBalanceRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(PointErrorCode.BALANCE_NOT_FOUND)
        );
        balance.setTotalAmount(balance.getTotalAmount() + type.getDefaultAmount());

        // 계좌 내역 생성
        PointDetail pointDetail = PointDetail.builder()
                .userBalance(balance)
                .amount(type.getDefaultAmount())
                .description(type.getDescription())
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusYears(1L))
                .remainingAmount(balance.getTotalAmount())
                .build();
        pointDetailRepository.save(pointDetail);
        return balance;
    }

    @Transactional
    public UserBalance updateBalanceManual(String studentNum ,String description, Long pointAmount) {
        User user = userRepository.findByStudentNum(studentNum).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND)
        );


        UserBalance balance = userBalanceRepository.findById(user.getId()).orElseThrow(
                () -> new NotFoundException(PointErrorCode.BALANCE_NOT_FOUND_PENDING)
        );

        balance.setUpdatedAt(LocalDateTime.now());
        balance.setTotalAmount(balance.getTotalAmount()+pointAmount);

        // Detail 생성
        PointDetail pointDetail = PointDetail.builder()
                .userBalance(balance)
                .amount(pointAmount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusYears(1L))
                .remainingAmount(balance.getTotalAmount())
                .build();

        pointDetailRepository.save(pointDetail);
        return balance;
    }

    @Transactional
    public List<UserBalance> updateBalanceManualBulk(List<ManualBalanceUpdateRequest> requests) {
        List<UserBalance> results = new ArrayList<>();

        for (ManualBalanceUpdateRequest req : requests) {
            UserBalance balance = updateBalanceManual(
                    req.getStudentNum(), req.getDescription(), req.getAmount()
            );
            results.add(balance);
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<UserBalance> findAllUserBalance() {
        return userBalanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserBalance findUserBalance(Long userId) {
        return userBalanceRepository.findById(userId)
                .orElseThrow(
                        () -> new NotFoundException(PointErrorCode.BALANCE_NOT_FOUND)
                );
    }

    @Transactional
    public List<PointDetail> findPointDetail(Long userId) {
        return pointDetailRepository.findByUserBalanceIdOrderByCreatedAtDesc(userId);
    }
}
