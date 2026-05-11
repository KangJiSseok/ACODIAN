package com.ibank.axwms.global.logging;

import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * SecurityContext 의 다양한 principal 표현 중 프로젝트 JWT principal 만 로그 요약 대상으로 허용한다.
 */
@Component
public class TracePrincipalResolver {

    /**
     * 로그에 남길 수 있는 최소 인증 요약만 담아 email/token 같은 식별 확장값이 섞이지 않도록 한다.
     */
    public record PrincipalSummary(Long userId, String roleCode) {
    }

    /**
     * 인증 완료된 CustomUserPrincipal 만 userId/roleCode 로 축약하고 anonymous·문자열 principal 은 기록 대상에서 제외한다.
     *
     * @param authentication SecurityContext 에 저장된 현재 인증 객체
     * @return 로그에 안전하게 남길 수 있는 사용자 요약
     */
    public Optional<PrincipalSummary> resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(new PrincipalSummary(principal.userId(), principal.roleCode()));
    }
}
