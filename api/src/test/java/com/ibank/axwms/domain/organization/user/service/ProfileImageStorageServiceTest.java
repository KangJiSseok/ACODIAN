package com.ibank.axwms.domain.organization.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileImageStorageServiceTest {

    @Mock
    private ObjectStoragePort objectStoragePort;

    @InjectMocks
    private ProfileImageStorageService profileImageStorageService;

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
