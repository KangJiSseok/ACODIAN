package com.ibank.axwms.domain.organization.team.service;

import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TeamAccessPolicy {

    /** 읽기 계열 team API 가 최소한 인증된 조직 사용자에게만 열리도록 보장한다. */
    public void assertReadable(CustomUserPrincipal principal) {
        requirePrincipal(principal);
        roleOf(principal);
    }

    /** 생성/수정/상태변경/삭제 계열은 부서장 이상만 허용한다. */
    public void assertWritable(CustomUserPrincipal principal) {
        UserRole role = roleOf(requirePrincipal(principal));
        if (role != UserRole.DIRECTOR && role != UserRole.DEPT_HEAD) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    /** 부서 ownership 검증이 필요한 호출에서 대상 부서 접근 가능 여부를 확인한다. */
    public void assertDepartmentOwnership(CustomUserPrincipal principal,
                                          Long principalDepartmentId,
                                          Long targetDepartmentId) {
        UserRole role = roleOf(requirePrincipal(principal));
        if (role == UserRole.DIRECTOR) {
            return;
        }
        if (!Objects.equals(principalDepartmentId, targetDepartmentId)) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    /** 팀 ownership 검증이 필요한 호출에서 대상 팀 접근 가능 여부를 확인한다. */
    public void assertTeamOwnership(CustomUserPrincipal principal,
                                    Long principalTeamId,
                                    Long targetTeamId) {
        UserRole role = roleOf(requirePrincipal(principal));
        if (role == UserRole.DIRECTOR) {
            return;
        }
        if (!Objects.equals(principalTeamId, targetTeamId)) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    private CustomUserPrincipal requirePrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return principal;
    }

    private UserRole roleOf(CustomUserPrincipal principal) {
        try {
            return UserRole.valueOf(principal.roleCode());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }
}
