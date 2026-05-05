package com.ibank.axwms.domain.organization.user.event;

/**
 * 사용자 저장 트랜잭션이 커밋되면 temp 위치에 업로드된 프로필 이미지를 최종 위치로 복사하고, 교체 전 이미지가 있으면 정리한다.
 */
public record ProfileImageCommittedEvent(String tempKey, String finalKey, String oldKey) {

    public ProfileImageCommittedEvent(String tempKey, String finalKey) {
        this(tempKey, finalKey, null);
    }
}
