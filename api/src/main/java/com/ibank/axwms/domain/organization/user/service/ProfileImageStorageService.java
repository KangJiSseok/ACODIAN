package com.ibank.axwms.domain.organization.user.service;

import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 이미지의 임시 업로드 → DB 저장 → 서빙 위치 복사 흐름을 조율한다.
 * S3 직접 호출은 ObjectStoragePort 로 위임하고, 키 생성/URL 계산 등 업로드 정책만 담당한다.
 */
@Slf4j
@Service
public class ProfileImageStorageService {

    static final String PROFILE_PREFIX = "profile";
    static final String TEMP_PREFIX = "temp";
    private static final String DEFAULT_EXTENSION = "bin";

    private final ObjectStoragePort objectStoragePort;

    public ProfileImageStorageService(
            @Qualifier("profileImageS3ObjectStorageAdapter") ObjectStoragePort objectStoragePort
    ) {
        this.objectStoragePort = objectStoragePort;
    }

    /**
     * @param tempKey  업로드 직후 임시 저장 위치 S3 키 — promote() 의 복사 출처
     * @param finalKey 서빙될 S3 오브젝트 키 — promote() 의 복사 목적지
     * @param finalUrl DB 에 저장되는 공개 접근 URL
     */
    public record TempUploadResult(String tempKey, String finalKey, String finalUrl) {}

    /**
     * 프로필 이미지를 temp 위치에 업로드하고 서빙 URL 을 미리 반환한다.
     * DB 저장 성공 후 promote() 로 temp → 서빙 위치 복사가 이루어지며,
     * DB 저장 실패 시 temp 파일은 스케줄러가 주기적으로 정리한다.
     */
    public TempUploadResult uploadTemp(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }
        String finalKey = generateStorageKey(profileImage.getOriginalFilename());
        String tempKey = TEMP_PREFIX + "/" + finalKey;
        try {
            objectStoragePort.upload(profileImage, tempKey);
            String finalUrl = objectStoragePort.toPublicUrl(finalKey);
            return new TempUploadResult(tempKey, finalKey, finalUrl);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.AUTH_SIGNUP_PROFILE_IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * temp 위치의 프로필 이미지를 서빙 위치로 복사한다.
     * DB 커밋이 확정된 AFTER_COMMIT 단계에서 호출되며, 복사 실패는 log 만 남긴다.
     */
    public boolean promote(String tempKey, String finalKey) {
        if (!StringUtils.hasText(tempKey)) {
            return false;
        }
        try {
            objectStoragePort.copy(tempKey, finalKey);
            return true;
        } catch (RuntimeException exception) {
            log.warn("프로필 이미지 S3 복사 실패 tempKey={}", tempKey, exception);
            return false;
        }
    }

    /**
     * 공개 URL 이 현재 프로필 이미지 저장소가 관리하는 final key 이면 삭제 가능한 storage key 로 복원한다.
     * 외부 URL 이나 temp URL 은 사용자가 보낸 값일 수 있으므로 삭제 대상에서 제외한다.
     */
    public String resolveDeletableProfileImageKey(String profileImageUrl) {
        if (!StringUtils.hasText(profileImageUrl)) {
            return null;
        }
        Optional<String> storageKey = objectStoragePort.toStorageKey(profileImageUrl);
        return storageKey
                .filter(key -> key.startsWith(PROFILE_PREFIX + "/"))
                .orElse(null);
    }

    /**
     * 기존 프로필 이미지 정리는 사용자 수정 성공 이후의 best-effort 후처리다.
     * 삭제 실패가 이미 커밋된 사용자 정보 변경을 롤백하거나 사용자 응답을 실패로 바꾸지 않도록 예외를 흡수한다.
     */
    public void deleteBestEffort(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            objectStoragePort.delete(key);
        } catch (RuntimeException exception) {
            log.warn("기존 프로필 이미지 S3 삭제 실패 key={}", key, exception);
        }
    }

    /** profile/{날짜}-{UUID}.{확장자} 형식의 S3 오브젝트 키를 생성한다. */
    private String generateStorageKey(String originalName) {
        LocalDate today = LocalDate.now();
        return PROFILE_PREFIX + "/%d-%02d-%02d-%s.%s".formatted(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extensionOf(originalName)
        );
    }

    /** 파일 원본 이름에서 확장자를 추출 .pdf 등 */
    private String extensionOf(String originalName) {
        if (originalName == null) {
            return DEFAULT_EXTENSION;
        }
        int dot = originalName.lastIndexOf('.');
        if (dot <= 0 || dot == originalName.length() - 1) {
            return DEFAULT_EXTENSION;
        }
        return originalName.substring(dot + 1).toLowerCase();
    }
}
