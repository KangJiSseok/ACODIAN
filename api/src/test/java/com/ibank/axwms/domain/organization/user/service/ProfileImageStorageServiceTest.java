package com.ibank.axwms.domain.organization.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService.FinalUploadResult;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProfileImageStorageServiceTest {

    @Mock
    private ObjectStoragePort objectStoragePort;

    @InjectMocks
    private ProfileImageStorageService profileImageStorageService;

    @Test
    @DisplayName("final 프로필 이미지를 직접 업로드하고 공개 URL 을 반환한다")
    void final_프로필_이미지를_직접_업로드하고_공개_URL을_반환한다() {
        MockMultipartFile profileImage = new MockMultipartFile(
                "profile_image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "image".getBytes(StandardCharsets.UTF_8)
        );
        given(objectStoragePort.toPublicUrl(org.mockito.ArgumentMatchers.startsWith("profile/")))
                .willReturn("https://cdn.axwms.com/profile/new.png");

        FinalUploadResult result = profileImageStorageService.uploadFinal(profileImage);

        assertThat(result.finalKey()).startsWith("profile/");
        assertThat(result.finalKey()).endsWith(".png");
        assertThat(result.finalUrl()).isEqualTo("https://cdn.axwms.com/profile/new.png");
        then(objectStoragePort).should().upload(org.mockito.ArgumentMatchers.eq(profileImage), org.mockito.ArgumentMatchers.startsWith("profile/"));
    }

    @Test
    @DisplayName("final 프로필 이미지가 비어 있으면 업로드하지 않는다")
    void final_프로필_이미지가_비어_있으면_업로드하지_않는다() {
        MockMultipartFile profileImage = new MockMultipartFile(
                "profile_image",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        FinalUploadResult result = profileImageStorageService.uploadFinal(profileImage);

        assertThat(result).isNull();
        then(objectStoragePort).should(org.mockito.Mockito.never()).upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("final 프로필 이미지 업로드 실패는 self 전용 ErrorCode 로 변환한다")
    void final_프로필_이미지_업로드_실패는_self_전용_ErrorCode로_변환한다() {
        MockMultipartFile profileImage = new MockMultipartFile(
                "profile_image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "image".getBytes(StandardCharsets.UTF_8)
        );
        org.mockito.BDDMockito.willThrow(new IllegalStateException("upload failed"))
                .given(objectStoragePort)
                .upload(org.mockito.ArgumentMatchers.eq(profileImage), org.mockito.ArgumentMatchers.startsWith("profile/"));

        assertThatThrownBy(() -> profileImageStorageService.uploadFinal(profileImage))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("프로필 이미지 공개 URL 이면 삭제 가능한 storage key 로 복원한다")
    void 프로필_이미지_공개_URL이면_삭제_가능한_storage_key로_복원한다() {
        given(objectStoragePort.toStorageKey("https://cdn.axwms.com/profile/old.png"))
                .willReturn(Optional.of("profile/old.png"));

        String result = profileImageStorageService.resolveDeletableProfileImageKey("https://cdn.axwms.com/profile/old.png");

        assertThat(result).isEqualTo("profile/old.png");
    }

    @Test
    @DisplayName("프로필 이미지 final key 가 아니면 삭제 대상으로 복원하지 않는다")
    void 프로필_이미지_final_key가_아니면_삭제_대상으로_복원하지_않는다() {
        given(objectStoragePort.toStorageKey("https://cdn.axwms.com/temp/profile/old.png"))
                .willReturn(Optional.of("temp/profile/old.png"));

        String result = profileImageStorageService.resolveDeletableProfileImageKey("https://cdn.axwms.com/temp/profile/old.png");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("기존 프로필 이미지 삭제 실패는 best-effort 로 흡수한다")
    void 기존_프로필_이미지_삭제_실패는_best_effort로_흡수한다() {
        given(objectStoragePort.toStorageKey("https://cdn.axwms.com/profile/old.png"))
                .willReturn(Optional.of("profile/old.png"));
        org.mockito.BDDMockito.willThrow(new IllegalStateException("delete failed"))
                .given(objectStoragePort)
                .delete("profile/old.png");
        String oldKey = profileImageStorageService.resolveDeletableProfileImageKey("https://cdn.axwms.com/profile/old.png");

        assertThatNoException().isThrownBy(() -> profileImageStorageService.deleteBestEffort(oldKey));
        then(objectStoragePort).should().delete("profile/old.png");
    }
}
