package com.nimda.cite.domain.profiledecoration.service;

import com.nimda.cite.common.exception.GlobalNimdaException;
import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.s3.S3Service;
import com.nimda.cite.domain.profiledecoration.repository.ProfileDecorationRepository;
import com.nimda.cite.domain.profiledecoration.dto.ProfileDecorationCreateRequest;
import com.nimda.cite.domain.profiledecoration.entity.ProfileDecoration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileDecorationService {

    private final ProfileDecorationRepository repository;

    @Autowired(required = false)
    private S3Service s3Service;

    @Transactional(readOnly = true)
    public List<ProfileDecoration> getActiveDecorations() {
        return repository.findByActiveTrueOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public List<ProfileDecoration> getAllDecorations() {
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public ProfileDecoration getByKey(String key) {
        return repository.findByKey(key)
                .orElseThrow(() -> new GlobalNimdaException(ErrorCode.DECORATION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ProfileDecoration getById(Long id) {
        if (id == null) {
            throw new GlobalNimdaException(ErrorCode.DECORATION_ID_REQUIRED);
        }
        return repository.findById(id)
                .orElseThrow(() -> new GlobalNimdaException(ErrorCode.BADGE_NOT_FOUND));
    }

    /**
     * 배지 이미지 URL 조회
     * - 절대 경로/외부 URL이면 그대로, S3 키면 Presigned GET URL로 변환한다.
     */
    @Transactional(readOnly = true)
    public String getDecorationImageUrl(String key) {
        String filePath = getByKey(key).getFilePath();

        if (filePath.startsWith("/") || filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }

        if (s3Service == null) {
            throw new GlobalNimdaException(ErrorCode.IMAGE_SERVICE_UNAVAILABLE);
        }

        return s3Service.createPresignedGetUrl(filePath, 60, "inline");
    }

    @Transactional
    public ProfileDecoration create(ProfileDecorationCreateRequest request) {
        String key = normalizeKey(request.getKey());
        String label = request.getLabel() == null ? "" : request.getLabel().trim();
        String filePath = request.getFilePath() == null ? "" : request.getFilePath().trim();
        String requiredRole = null;
        boolean purchaseRequired = true;

        if (key.isBlank()) {
            throw new GlobalNimdaException(ErrorCode.BADGE_KEY_REQUIRED);
        }
        if (!key.matches("^[a-z0-9][a-z0-9-]{1,98}$")) {
            throw new GlobalNimdaException(ErrorCode.BADGE_KEY_INVALID);
        }
        if (label.isBlank()) {
            throw new GlobalNimdaException(ErrorCode.BADGE_LABEL_REQUIRED);
        }
        if (filePath.isBlank()) {
            throw new GlobalNimdaException(ErrorCode.BADGE_IMAGE_REQUIRED);
        }
        ProfileDecoration existing = repository.findByKey(key).orElse(null);
        if (existing != null) {
            if (existing.isActive()) {
                throw new GlobalNimdaException(ErrorCode.BADGE_KEY_ALREADY_EXIST);
            }
            existing.update(label, filePath, requiredRole, true);
            existing.setPurchaseRequired(purchaseRequired);
            return existing;
        }

        ProfileDecoration decoration = new ProfileDecoration(key, label, filePath);
        decoration.update(label, filePath, requiredRole, true);
        decoration.setPurchaseRequired(purchaseRequired);
        return repository.save(decoration);
    }

    @Transactional
    public void delete(Long id) {
        ProfileDecoration decoration = repository.findById(id)
                .orElseThrow(() -> new GlobalNimdaException(ErrorCode.BADGE_NOT_FOUND));
        decoration.deactivate();
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return null;
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
