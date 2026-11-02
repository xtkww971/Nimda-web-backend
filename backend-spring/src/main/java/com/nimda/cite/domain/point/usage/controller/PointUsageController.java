package com.nimda.cite.domain.point.usage.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.domain.point.usage.dto.PointUsageResponse;
import com.nimda.cite.domain.point.usage.service.PointUsageService;
import com.nimda.cite.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlNames.pointUsageBoardUrl)
@RequiredArgsConstructor
public class PointUsageController {

    private final PointUsageService pointUsageService;

    @PostMapping(UrlNames.purchaseUrl)
    public ResponseEntity<?> purchase(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();

        PointUsageResponse response = pointUsageService.purchaseBoardItem(userId, id);
        return ApiResponse.ok("구매가 완료되었습니다.", response).toResponse();
    }
}
