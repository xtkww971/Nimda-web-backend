package com.nimda.cite.domain.board.service;

import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.NotIncludeRoleException;
import com.nimda.cite.common.exception.errorcode.domain.AttachmentErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 게시판 첨부파일(로컬 업로드 디렉토리) 다운로드 서비스
 *
 * [보안]
 * - Path Traversal 공격 방지 (../ 등)
 * - 파일 존재 여부 확인
 * - 업로드 디렉토리 외부 접근 차단
 */
@Service
public class FileDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadService.class);

    // 업로드 디렉토리 경로 (BoardService와 동일)
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/board-uploads";

    /**
     * 다운로드할 파일 조회 (보안 검증 포함)
     *
     * @param fileName 다운로드할 파일명 (UUID_원본파일명 형식)
     * @return 업로드 디렉토리 내부의 읽기 가능한 파일
     */
    public File getDownloadFile(String fileName) {
        logger.debug("업로드 디렉토리: {}", UPLOAD_DIR);

        // ========== [보안 검증 #1] ==========
        // Path Traversal 공격 방지: 상대 경로(../)나 절대 경로(/) 포함 여부 확인
        // 단, 파일명에는 언더스코어(_)가 포함될 수 있으므로 제외
        if (fileName.contains("..") || fileName.startsWith("/") || fileName.startsWith("\\")) {
            logger.warn("Path Traversal 시도 감지: {}", fileName);
            throw new NotAllowFormException(AttachmentErrorCode.INVALID_FILE_NAME);
        }

        // ========== [파일 경로 구성] ==========
        // 업로드 디렉토리와 파일명을 결합하여 전체 경로 생성
        Path filePath = Paths.get(UPLOAD_DIR, fileName).normalize();
        File file = filePath.toFile();

        // ========== [보안 검증 #2] ==========
        // 실제 파일 경로가 업로드 디렉토리 내에 있는지 확인 (경로 탈출 방지)
        Path uploadDirPath = Paths.get(UPLOAD_DIR).normalize();
        if (!filePath.startsWith(uploadDirPath)) {
            logger.warn("업로드 디렉토리 외부 접근 시도: {}", filePath);
            throw new NotIncludeRoleException(AttachmentErrorCode.INVALID_FILE_PATH);
        }

        // ========== [파일 존재 확인] ==========
        if (!file.exists() || !file.isFile()) {
            logger.warn("파일을 찾을 수 없음: {}", filePath);
            throw new NotFoundException(AttachmentErrorCode.FILE_NOT_FOUND);
        }

        // ========== [파일 읽기 권한 확인] ==========
        if (!file.canRead()) {
            logger.warn("파일 읽기 권한 없음: {}", filePath);
            throw new NotIncludeRoleException(AttachmentErrorCode.FILE_READ_FORBIDDEN);
        }

        return file;
    }
}
