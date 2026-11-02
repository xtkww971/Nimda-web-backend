package com.nimda.cite.domain.point.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.domain.point.dto.BalanceResponse;
import com.nimda.cite.domain.point.dto.ManualBalanceUpdateRequest;
import com.nimda.cite.domain.point.dto.PointDetailResponse;
import com.nimda.cite.domain.point.entity.UserBalance;
import com.nimda.cite.domain.point.service.PointService;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.service.UserService;
import com.nimda.cite.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(UrlNames.pointUrl)
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;
    private final UserService userService;

    // PointController.java 수정

    @GetMapping(UrlNames.getBalanceUrl)
    public ResponseEntity<?> getBalance(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();

        UserBalance balance = pointService.findUserBalance(userId);
        BalanceResponse dto = BalanceResponse.builder()
                .totalAmount(balance.getTotalAmount())
                .updatedAt(balance.getUpdatedAt())
                .build();

        return ApiResponse.ok("계좌 조회에 성공했습니다.", dto).toResponse();
    }

    // 계좌 전체 조회 (관리자 전용)
    @GetMapping(UrlNames.totalBalanceUrl)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> totalBalance(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<BalanceResponse> dto = pointService.findAllUserBalance().stream().map(BalanceResponse::from)
                .toList();
        return ApiResponse.ok("계좌 전체 조회에 성공했습니다.",dto).toResponse();
    }

    // 특정 계좌 디테일 조회
    @GetMapping(UrlNames.viewPointDetailUrl)
    public ResponseEntity<?> viewPointDetail(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();

        List<PointDetailResponse> dto = pointService.findPointDetail(userId).stream().map(
                PointDetailResponse::from).toList();

        return ApiResponse.ok("포인트 내역 조회에 성공했습니다.",dto).toResponse();
    }

    @PostMapping(UrlNames.updateUserBalanceManualUrl)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserBalanceManual(
            @AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody ManualBalanceUpdateRequest req) {

        String studentNum = req.getStudentNum();
        BalanceResponse dto = BalanceResponse.from(
                pointService.updateBalanceManual(studentNum, req.getDescription(), req.getAmount())
                );
        return ApiResponse.ok("마일리지 지급이 완료되었습니다.",dto).toResponse();
    }


    @PostMapping(UrlNames.updateUserBalanceManualBulkUrl)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserBalanceManualBulk(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody List<ManualBalanceUpdateRequest> requests) {

        List<BalanceResponse> results = pointService.updateBalanceManualBulk(requests)
                .stream()
                .map(BalanceResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.ok("마일리지 일괄 지급이 완료되었습니다.", results).toResponse();
    }


    /**
     * 닉네임으로 특정 유저 마일리지 잔액 조회 (공개 프로필용, 인증 불필요)
     * GET /api/cite/point/user/{nickname}
     */
    @GetMapping(UrlNames.getBalanceByNicknameUrl)
    public ResponseEntity<?> getBalanceByNickname(@PathVariable String nickname) {
        User user = userService.getUserByNickname(nickname);
        UserBalance balance = pointService.findUserBalance(user.getId());
        BalanceResponse dto = BalanceResponse.builder()
                .totalAmount(balance.getTotalAmount())
                .updatedAt(balance.getUpdatedAt())
                .build();
        return ApiResponse.ok("계좌 조회에 성공했습니다.", dto).toResponse();
    }

    /**
     * 닉네임으로 특정 유저 마일리지 내역 조회 (공개 프로필용, 인증 불필요)
     * GET /api/cite/point/user/{nickname}/details
     */
    @GetMapping(UrlNames.getPointDetailsByNicknameUrl)
    public ResponseEntity<?> getPointDetailsByNickname(@PathVariable String nickname) {
        User user = userService.getUserByNickname(nickname);
        List<PointDetailResponse> dto = pointService.findPointDetail(user.getId()).stream()
                .map(PointDetailResponse::from).toList();
        return ApiResponse.ok("포인트 내역 조회에 성공했습니다.", dto).toResponse();
    }

}
