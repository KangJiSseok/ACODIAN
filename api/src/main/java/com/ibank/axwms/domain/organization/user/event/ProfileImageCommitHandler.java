package com.ibank.axwms.domain.organization.user.event;

import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원가입 트랜잭션 커밋 후 temp 위치의 프로필 이미지를 final 위치로 복사한다.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageCommitHandler {

    private final ProfileImageStorageService profileImageStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(ProfileImageCommittedEvent event) {
        profileImageStorageService.promote(event.tempKey(), event.finalKey());
    }
}
