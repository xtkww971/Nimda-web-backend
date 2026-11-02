package com.nimda.cite.user.controller;

import com.nimda.cite.common.s3.S3Service;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.dto.user.UserPublicProfileResponseDTO;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.service.AdminUserService;
import com.nimda.cite.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UrlNames.usersUrl)
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    private final AdminUserService adminUserService;

    private final S3Service s3Service;

    /**
     * 닉네임으로 사용자 조회 (공개 프로필)
     * - 프론트가 응답 body 전체를 프로필로 사용하므로 ApiResponse로 감싸지 않는다.
     *
     * @param nickname 닉네임
     * @return 공개 가능한 사용자 정보
     */
    @GetMapping(UrlNames.getUserByNicknameUrl)
    public ResponseEntity<UserPublicProfileResponseDTO> getUserByNickname(@PathVariable String nickname) {
        User user = userService.getUserByNickname(nickname);

        // 프로필 이미지 S3 키 → Presigned URL 변환
        String profileImage = user.getProfileImage();
        if (profileImage != null && !profileImage.isBlank() && !profileImage.startsWith("http") && s3Service != null) {
            profileImage = s3Service.createPresignedGetUrl(profileImage, 60);
        }

        return ResponseEntity.ok(UserPublicProfileResponseDTO.of(user, profileImage));
    }

}
