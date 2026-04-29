package com.ibank.axwms.domain.organization.user.event;

/**
 * 회원가입 트랜잭션이 커밋되면 temp 위치에 업로드된 프로필 이미지를 최종 위치로 복사한다.
 */
public record ProfileImageCommittedEvent(String tempKey, String finalKey) {
}
