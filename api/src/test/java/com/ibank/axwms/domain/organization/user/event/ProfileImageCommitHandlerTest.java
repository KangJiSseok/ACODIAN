package com.ibank.axwms.domain.organization.user.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileImageCommitHandlerTest {

    @Mock
    private ProfileImageStorageService profileImageStorageService;

    @InjectMocks
    private ProfileImageCommitHandler profileImageCommitHandler;

    @Test
    @DisplayName("프로필 이미지 promote 성공 후 기존 이미지 key 가 있으면 삭제를 요청한다")
    void 프로필_이미지_promote_성공_후_기존_이미지_key가_있으면_삭제를_요청한다() {
        ProfileImageCommittedEvent event = new ProfileImageCommittedEvent(
                "temp/profile/new.png",
                "profile/new.png",
                "profile/old.png"
        );
        given(profileImageStorageService.promote("temp/profile/new.png", "profile/new.png")).willReturn(true);

        profileImageCommitHandler.onCommit(event);

        then(profileImageStorageService).should().deleteBestEffort("profile/old.png");
    }

    @Test
    @DisplayName("프로필 이미지 promote 실패 시 기존 이미지를 삭제하지 않는다")
    void 프로필_이미지_promote_실패_시_기존_이미지를_삭제하지_않는다() {
        ProfileImageCommittedEvent event = new ProfileImageCommittedEvent(
                "temp/profile/new.png",
                "profile/new.png",
                "profile/old.png"
        );
        given(profileImageStorageService.promote("temp/profile/new.png", "profile/new.png")).willReturn(false);

        profileImageCommitHandler.onCommit(event);

        then(profileImageStorageService).should(never()).deleteBestEffort("profile/old.png");
    }
}
