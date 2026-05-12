package com.ibank.axwms.global.security;

/**
 * JWT access token claim 에서 복원한 최소 인증 주체 정보.
 * 보호된 API 는 DB 재조회 없이도 사용자 식별자와 기본 권한 문맥을 바로 사용할 수 있다.
 */
public record CustomUserPrincipal(
        Long userId,
        String email,
        String roleCode
) {
}
