package com.ibank.axwms.domain.worklog.policy;

import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.service.UserService;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 현재 로그인 사용자의 역할을 업무 목록 가시 범위(WorklogVisibilityScope) 로 변환한다.
 * SQL 분기는 알지 못하며, 역할별 정책 결정만 책임진다.
 */
@Component
@RequiredArgsConstructor
public class WorklogVisibilityPolicy {

    private final UserService userService;

    /**
     * principal 의 role 코드에 따라 업무 목록 가시 범위를 결정한다.
     *
     * @param principal 현재 로그인 사용자
     * @return 역할별로 결정된 가시 범위
     * @throws BusinessException AUTH_ACCESS_DENIED principal 또는 role 코드가 비정상일 때
     * @throws BusinessException USER_NOT_FOUND DEPT_HEAD 인데 사용자/부서 문맥을 복원할 수 없을 때
     */
    public WorklogVisibilityScope resolve(CustomUserPrincipal principal) {
        UserRole role = parseRole(principal);

        return switch (role) {
            case DIRECTOR -> new WorklogVisibilityScope.All();
            case DEPT_HEAD -> new WorklogVisibilityScope.Department(
                    userService.getDepartmentIdOrThrow(principal.userId()));
            case TEAM_LEAD, MEMBER -> new WorklogVisibilityScope.MyTeams(principal.userId());
        };
    }

    /** principal.roleCode 가 UserRole 매핑에 실패하면 ownership 분기와 동일한 비즈니스 오류로 정규화한다. */
    private UserRole parseRole(CustomUserPrincipal principal) {
        try {
            return UserRole.valueOf(principal.roleCode());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }
}
