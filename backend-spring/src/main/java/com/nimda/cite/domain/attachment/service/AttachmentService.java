package com.nimda.cite.domain.attachment.service;

import com.nimda.cite.common.exception.ExternalServiceException;
import com.nimda.cite.common.exception.InvalidStateException;
import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.NotIncludeRoleException;
import com.nimda.cite.common.exception.errorcode.domain.AttachmentErrorCode;
import com.nimda.cite.config.CheckConfig;
import com.nimda.cite.domain.attachment.dto.AttachmentResponseDto;
import com.nimda.cite.domain.attachment.entity.Attachment;
import com.nimda.cite.domain.attachment.repository.AttachmentRepository;
import com.nimda.cite.domain.attachment.store.FileStore;
import com.nimda.cite.domain.attachment.store.S3FileStore;
import com.nimda.cite.common.s3.S3Service;
import com.nimda.cite.domain.board.constants.CategoryConstants;
import com.nimda.cite.common.image.ImageSanitizer;
import com.nimda.cite.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 이미지 (svg 제외 — 내부 스크립트 삽입으로 XSS 가능)
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            // 문서
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            // 텍스트
            "txt", "md", "csv",
            // 압축
            "zip", "7z", "rar",
            // 미디어
            "mp4", "avi", "mov", "mp3", "wav"
    );

    public static boolean isAllowedExtension(String ext) {
        if (ext == null || ext.isBlank()) return false;
        return ALLOWED_EXTENSIONS.contains(ext.toLowerCase());
    }

    private final AttachmentRepository attachmentRepository;
    private final FileStore fileStore;
    private final S3Service s3Service;

    /**
     * [핵심 기능 1] 파일 업로드 및 DB 기록
     * - 유저 정보, 카테고리, 게시글 정보를 모두 받아 연결함
     */
    public AttachmentResponseDto uploadFile(CustomUserDetails userDetails, MultipartFile file, Long boardId, Long categoryId) {
        CheckConfig.npeCheck(file, "file");
        CheckConfig.npeAndNegativeCheck(categoryId, "categoryId");
        CheckConfig.npeCheck(userDetails, "userDetails");

        // boardId는 게시글 작성 전 업로드에서는 없을 수 있으므로 값이 있을 때만 검증
        if (boardId != null) {
            CheckConfig.npeAndNegativeCheck(boardId, "boardId");
        }
        if (file.isEmpty()) {
            throw new NotAllowFormException(AttachmentErrorCode.UPLOAD_FILE_NOT_FOUND);
        }

        // 0. userId 가져오기
        Long userId = userDetails.getUser().getId();
        CheckConfig.npeAndNegativeCheck(userId, "userId");

        // 1. 원본 파일명 및 확장자 추출
        String originName = file.getOriginalFilename();
        String ext = extractExt(originName);

        // 2. 허용된 확장자 검증 (화이트리스트, 확장자가 없으면 빈 문자열이므로 여기서 걸러짐)
        if(!isAllowedExtension(ext)){
            throw new NotAllowFormException(AttachmentErrorCode.FILE_FORM_NOT_ALLOW);
        }

        // 3. 이미지 파일이면 재인코딩하여 메타데이터/삽입 코드 파괴
        long fileSize = file.getSize();

        if (ImageSanitizer.isImageExtension(ext)) {
            try {
                byte[] sanitized = ImageSanitizer.reEncode(file, ext);
                ext = ImageSanitizer.getOutputExtension(ext);
                fileSize = sanitized.length;

                String storedName = UUID.randomUUID().toString() + "." + ext;
                String filepath = fileStore.storeBytes(sanitized, storedName);

                Attachment attachment = Attachment.create(
                        originName, storedName, filepath, ext, fileSize,
                        boardId, categoryId, userId
                );
                return AttachmentResponseDto.from(attachmentRepository.save(attachment));
            } catch (IOException e) {
                throw new ExternalServiceException(AttachmentErrorCode.IMAGE_PROCESSING_FAILED);
            }
        }

        // 4. 비이미지 파일: 기존 로직
        String storedName = UUID.randomUUID().toString() + "." + ext;
        String filepath = fileStore.storeFile(file, storedName);

        Attachment attachment = Attachment.create(
                originName, storedName, filepath, ext, fileSize,
                boardId, categoryId, userId
        );

        return AttachmentResponseDto.from(attachmentRepository.save(attachment));
    }

    /**
     * 엔티티 조회 (다운로드 등에서 메타정보 필요 시 사용)
     */
    @Transactional(readOnly = true)
    public AttachmentResponseDto getAttachment(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);

        return AttachmentResponseDto.from(attachment);
    }

    /**
     * [핵심 기능 2] 파일 상세 조회 (카테고리별 뷰어 분기)
     * - 갤러리 카테고리(예: 2번)는 웹에서 바로 보이게(inline) 설정
     */
    @Transactional(readOnly = true)
    public AttachmentResponseDto getFile(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);

        // 카테고리 ID를 활용한 정책 분기 (상수로 관리해 ID 변경 시 한 곳만 수정)
        String disposition = "attachment"; // 기본값: 다운로드
        if (CategoryConstants.GALLERY_ID.equals(attachment.getCategoryId())) {
            disposition = "inline"; // 갤러리 등은 바로보기
        }
        // PDF는 내장 JavaScript 실행 방지를 위해 항상 다운로드 강제
        if ("pdf".equalsIgnoreCase(attachment.getExtension())) {
            disposition = "attachment";
        }

        return AttachmentResponseDto.from(attachment, disposition);
    }

    /**
     * [핵심 기능 3] 내 파일 모아보기 (userId 활용)
     * - 특정 유저가 올린 모든 파일을 리스트로 반환
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> getMyFileList(Long userId) {
        return attachmentRepository.findByUserId(userId).stream()
                .map(attachment -> AttachmentResponseDto.from(attachment, "attachment"))
                .collect(Collectors.toList());
    }

    /**
     * [핵심 기능 4] 유저별 선택 삭제 (보안 및 물리 파일 삭제)
     */
    public void deleteUserFiles(Long userId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new NotAllowFormException(AttachmentErrorCode.FILE_IDS_REQUIRED);
        }

        List<Attachment> files = attachmentRepository.findAllById(fileIds);

        for (Attachment file : files) {
            // 보안 검증: 요청 유저가 파일 소유자인지 확인
            if (!file.getUserId().equals(userId)) {
                throw new NotIncludeRoleException(AttachmentErrorCode.FILE_ROLE_NOT_EXIST);
            }

            deletePhysicalFile(file);

            // 2. DB 레코드 삭제
            attachmentRepository.delete(file);
        }
    }

    /**
     * 확장자 추출 헬퍼 메서드
     * - 파일명이 없거나 점이 없으면 빈 문자열 반환 (NPE 방지)
     */
    private String extractExt(String originalFileName) {
        CheckConfig.npeAndBlankCheck(originalFileName, "originalFileName");

        int pos = originalFileName.lastIndexOf(".");
        if (pos < 0 || pos == originalFileName.length() - 1) {
            return "";
        }
        return originalFileName.substring(pos + 1);
    }

    /**
     * S3 Presigned 업로드 후, key/메타정보만으로 첨부파일을 등록한다.
     * - 파일 본문은 이미 S3에 존재한다고 가정한다.
     */
    public Long registerFromS3(String key,
                               String originFilename,
                               Long fileSize,
                               Long boardId,
                               Long categoryId,
                               Long userId) {
        if (key == null || key.isBlank()) {
            throw new NotAllowFormException(AttachmentErrorCode.S3_KEY_INNEED);
        }
        // key에서 실제 저장 파일명 부분만 추출 (예: boards/files/uuid_name.png -> uuid_name.png)
        String storedFilename = extractStoredFilenameFromKey(key);

        // 확장자는 원본 파일명 기준으로 우선 추출, 없으면 storedFilename 기준
        String nameForExt = originFilename != null && !originFilename.isBlank() ? originFilename : storedFilename;
        String ext = extractExt(nameForExt);
        if (!isAllowedExtension(ext)) {
            throw new NotAllowFormException(AttachmentErrorCode.FILE_FORM_NOT_ALLOW);
        }

        String safeOrigin = (originFilename == null || originFilename.isBlank()) ? storedFilename : originFilename;

        Attachment attachment = Attachment.create(
                safeOrigin,
                storedFilename,
                key,               // filepath에는 S3 key를 그대로 저장해 둔다.
                ext,
                fileSize,
                boardId,
                categoryId,
                userId
        );

        return attachmentRepository.save(attachment).getId();
    }

    /**
     * 글 저장 직후: presigned로 등록된 첨부( boardId 미설정 )를 해당 게시글에 연결.
     */
    public void linkAttachmentsToBoard(List<Long> attachmentIds, Long boardId, Long categoryId, Long userId) {
        // 첨부 없이 작성하는 게시글도 있으므로 빈 목록은 예외가 아니라 연결할 대상이 없는 것으로 처리
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        CheckConfig.npeAndNegativeCheck(boardId, "boardId");
        CheckConfig.npeAndNegativeCheck(categoryId, "categoryId");
        CheckConfig.npeAndNegativeCheck(userId, "userId");

        for (Long aid : attachmentIds) {
            if (aid == null) {
                continue;
            }
            Attachment a = attachmentRepository.findById(aid)
                    .orElseThrow(() -> new NotFoundException(AttachmentErrorCode.ATTACHMENT_NOT_FOUND));
            if (!a.getUserId().equals(userId)) {
                throw new NotIncludeRoleException(AttachmentErrorCode.ATTACHMENT_LINK_FORBIDDEN);
            }
            if (a.getBoardId() != null && !a.getBoardId().equals(boardId)) {
                throw new InvalidStateException(AttachmentErrorCode.ATTACHMENT_ALREADY_LINKED);
            }
            if (categoryId != null && a.getCategoryId() != null && !a.getCategoryId().equals(categoryId)) {
                throw new NotAllowFormException(AttachmentErrorCode.ATTACHMENT_CATEGORY_MISMATCH);
            }
            a.linkToBoard(boardId);
        }
    }

    /**
     * 글 수정: 최종 첨부 ID 목록과 동기화. 목록에서 빠진 첨부는 S3+DB 삭제.
     *
     * @param attachmentIds null 이면 첨부 변경 없음
     */
    public void syncAttachmentsForBoard(Long boardId, List<Long> attachmentIds, Long categoryId, Long userId) {
        if (attachmentIds == null) {
            return;
        }
        List<Attachment> current = attachmentRepository.findByBoardIdOrderByIdAsc(boardId);
        Set<Long> wanted = new HashSet<>();
        for (Long id : attachmentIds) {
            if (id != null) {
                wanted.add(id);
            }
        }

        for (Attachment a : current) {
            if (!wanted.contains(a.getId())) {
                if (!a.getUserId().equals(userId)) {
                    throw new NotIncludeRoleException(AttachmentErrorCode.ATTACHMENT_REMOVE_FORBIDDEN);
                }
                deletePhysicalFile(a);
                attachmentRepository.delete(a);
            }
        }

        for (Long aid : wanted) {
            Attachment a = attachmentRepository.findById(aid)
                    .orElseThrow(() -> new NotFoundException(AttachmentErrorCode.ATTACHMENT_NOT_FOUND));
            if (!a.getUserId().equals(userId)) {
                throw new NotIncludeRoleException(AttachmentErrorCode.ATTACHMENT_LINK_FORBIDDEN);
            }
            if (a.getBoardId() != null && !a.getBoardId().equals(boardId)) {
                throw new InvalidStateException(AttachmentErrorCode.ATTACHMENT_ALREADY_LINKED);
            }
            if (categoryId != null && a.getCategoryId() != null && !a.getCategoryId().equals(categoryId)) {
                throw new NotAllowFormException(AttachmentErrorCode.ATTACHMENT_CATEGORY_MISMATCH);
            }
            if (a.getBoardId() == null) {
                a.linkToBoard(boardId);
            }
        }
    }

    /**
     * 게시글 상세 응답용 — 해당 글에 연결된 첨부 목록
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> listAttachmentsForBoard(Long boardId) {
        if (boardId == null) {
            return Collections.emptyList();
        }
        return attachmentRepository.findByBoardIdOrderByIdAsc(boardId).stream()
                .map(att -> {
                    String disp = CategoryConstants.GALLERY_ID.equals(att.getCategoryId()) ? "inline" : "attachment";
                    return AttachmentResponseDto.from(att, disp);
                })
                .collect(Collectors.toList());
    }

    public Long resolveThumbnailAttachmentId(List<Long> attachmentIds, Long requestedThumbnailAttachmentId) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return null;
        }

        List<Attachment> imageAttachments = attachmentIds.stream()
                .filter(id -> id != null)
                .map(id -> attachmentRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException(AttachmentErrorCode.ATTACHMENT_NOT_FOUND)))
                .filter(this::isImage)
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();

        if (imageAttachments.isEmpty()) {
            return null;
        }

        if (requestedThumbnailAttachmentId == null) {
            return imageAttachments.get(0).getId();
        }

        return imageAttachments.stream()
                .filter(attachment -> attachment.getId().equals(requestedThumbnailAttachmentId))
                .findFirst()
                .orElseThrow(() -> new NotAllowFormException(AttachmentErrorCode.INVALID_THUMBNAIL))
                .getId();
    }

    private boolean isImage(Attachment attachment) {
        String extension = attachment.getExtension();
        if (extension == null || extension.isBlank()) {
            String filename = attachment.getOriginFilename();
            extension = filename != null && filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1)
                    : "";
        }

        return List.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
                .contains(extension.toLowerCase());
    }

    /**
     * 게시글 삭제 시 — 연결된 모든 첨부 물리·DB 삭제
     */
    public void deleteAttachmentsForBoard(Long boardId) {
        if (boardId == null) {
            return;
        }
        List<Attachment> list = attachmentRepository.findByBoardId(boardId);
        for (Attachment a : list) {
            deletePhysicalFile(a);
            attachmentRepository.delete(a);
        }
    }

    /**
     * 로컬 리소스를 열 수 없을 때 사용할 S3 Presigned GET URL 생성
     * - PDF는 브라우저 내 스크립트 실행 방지를 위해 Content-Disposition: attachment 강제
     */
    @Transactional(readOnly = true)
    public String createPresignedDownloadUrl(AttachmentResponseDto attachment) {
        if (s3Service == null || attachment.getFilepath() == null) {
            throw new NotFoundException(AttachmentErrorCode.FILE_NOT_FOUND);
        }

        String disposition = "pdf".equalsIgnoreCase(attachment.getExtension()) ? "attachment" : null;
        String presignedUrl = s3Service.createPresignedGetUrl(attachment.getFilepath(), 10, disposition);
        if (presignedUrl == null) {
            throw new NotFoundException(AttachmentErrorCode.FILE_NOT_FOUND);
        }
        return presignedUrl;
    }

    /**
     * [S3] 업로드용 Presigned URL 발급
     * - S3 저장소를 사용하지 않는 환경에서는 사용할 수 없다.
     */
    @Transactional(readOnly = true)
    public S3Service.PresignedUpload createPresignedUpload(String type, String fileName) {
        if (!(fileStore instanceof S3FileStore s3FileStore)) {
            throw new ExternalServiceException(AttachmentErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
        return s3FileStore.getPresignedUpload(type, fileName);
    }

    private Attachment findAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException(AttachmentErrorCode.ATTACHMENT_NOT_FOUND));
    }

    private void deletePhysicalFile(Attachment file) {
        String key = file.getFilepath();
        if (key != null && !key.isBlank()) {
            fileStore.deleteFile(key);
        } else {
            fileStore.deleteFile(file.getStoredFilename());
        }
    }

    private String extractStoredFilenameFromKey(String key) {
        int idx = key.lastIndexOf("/");
        if (idx < 0 || idx == key.length() - 1) {
            return key;
        }
        return key.substring(idx + 1);
    }
}
