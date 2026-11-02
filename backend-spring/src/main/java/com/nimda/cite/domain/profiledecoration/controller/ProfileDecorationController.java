package com.nimda.cite.domain.profiledecoration.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.domain.profiledecoration.dto.ProfileDecorationCreateRequest;
import com.nimda.cite.domain.profiledecoration.dto.ProfileDecorationDto;
import com.nimda.cite.domain.profiledecoration.dto.ProfileDecorationGrantRequestDTO;
import com.nimda.cite.domain.profiledecoration.entity.ProfileDecoration;
import com.nimda.cite.domain.profiledecoration.ownership.ProfileDecorationOwnershipService;
import com.nimda.cite.domain.profiledecoration.service.ProfileDecorationService;
import com.nimda.cite.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UrlNames.profileDecorationBaseUrl)
@RequiredArgsConstructor
public class ProfileDecorationController {

    private final ProfileDecorationService service;
    private final ProfileDecorationOwnershipService ownershipService;


    @GetMapping(UrlNames.getActiveDecorationsUrl)
    public ResponseEntity<?> getActiveDecorations() {
        return ApiResponse.ok(
                service.getActiveDecorations().stream()
                        .map(ProfileDecorationDto::from)
                        .toList()
        ).toResponse();
    }

    @GetMapping(UrlNames.getDecorationImageUrl)
    public ResponseEntity<Void> getDecorationImage(@PathVariable String key) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, service.getDecorationImageUrl(key))
                .build();
    }

    @GetMapping(UrlNames.getMyDecorationsUrl)
    public ResponseEntity<?> getMyDecorations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = CustomUserDetails.getLoginUser(userDetails).getId();

        return ApiResponse.ok(
                ownershipService.getOwnedDecorations(userId).stream()
                        .map(ProfileDecorationDto::from)
                        .toList()
        ).toResponse();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(UrlNames.getAdminDecorationsUrl)
    public ResponseEntity<?> getAdminDecorations() {
        return ApiResponse.ok(
                service.getAllDecorations().stream()
                        .filter(ProfileDecoration::isActive)
                        .map(ProfileDecorationDto::from)
                        .toList()
        ).toResponse();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(UrlNames.createDecorationUrl)
    public ResponseEntity<?> createDecoration(@RequestBody ProfileDecorationCreateRequest request) {
        ProfileDecoration decoration = service.create(request);
        return ApiResponse.ok("프로필 배지가 등록되었습니다.", ProfileDecorationDto.from(decoration)).toResponse();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(UrlNames.grantDecorationUrl)
    public ResponseEntity<?> grantDecoration(@RequestBody ProfileDecorationGrantRequestDTO request) {
        ProfileDecoration decoration = service.getById(request.getDecorationId());
        ownershipService.grantByStudentNum(request.getStudentNum(), decoration);
        return ApiResponse.ok("프로필 배지가 지급되었습니다.").toResponse();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(UrlNames.revokeDecorationUrl)
    public ResponseEntity<?> revokeDecoration(@RequestBody ProfileDecorationGrantRequestDTO request) {
        ProfileDecoration decoration = service.getById(request.getDecorationId());
        ownershipService.revokeByStudentNum(request.getStudentNum(), decoration);
        return ApiResponse.ok("프로필 배지가 회수되었습니다.").toResponse();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(UrlNames.deleteDecorationUrl)
    public ResponseEntity<?> deleteDecoration(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("프로필 배지가 삭제되었습니다.").toResponse();
    }
}
