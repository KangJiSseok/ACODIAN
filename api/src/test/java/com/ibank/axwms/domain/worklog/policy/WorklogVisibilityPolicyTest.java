package com.ibank.axwms.domain.worklog.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ibank.axwms.domain.organization.user.service.UserService;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorklogVisibilityPolicyTest {

    private static final Long USER_ID = 42L;
    private static final Long DEPARTMENT_ID = 7L;
    private static final String EMAIL = "user@ibank.com";

    @Mock
    private UserService userService;

    @InjectMocks
    private WorklogVisibilityPolicy worklogVisibilityPolicy;

    @Nested
    @DisplayName("역할별 Scope 매핑")
    class RoleToScope {

        @Test
        @DisplayName("DIRECTOR 이면 All scope 를 반환하고 부서 조회를 호출하지 않는다")
        void DIRECTOR_이면_All_scope_를_반환한다() {
            CustomUserPrincipal principal = new CustomUserPrincipal(USER_ID, EMAIL, "DIRECTOR");

            WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);

            assertThat(scope).isInstanceOf(WorklogVisibilityScope.All.class);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("DEPT_HEAD 이면 본인 부서 id 를 담은 Department scope 를 반환한다")
        void DEPT_HEAD_이면_Department_scope_를_반환한다() {
            CustomUserPrincipal principal = new CustomUserPrincipal(USER_ID, EMAIL, "DEPT_HEAD");
            given(userService.getDepartmentIdOrThrow(USER_ID)).willReturn(DEPARTMENT_ID);

            WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);

            assertThat(scope).isEqualTo(new WorklogVisibilityScope.Department(DEPARTMENT_ID));
            verify(userService).getDepartmentIdOrThrow(USER_ID);
        }

        @Test
        @DisplayName("TEAM_LEAD 이면 본인 사용자 id 를 담은 MyTeams scope 를 반환한다")
        void TEAM_LEAD_이면_MyTeams_scope_를_반환한다() {
            CustomUserPrincipal principal = new CustomUserPrincipal(USER_ID, EMAIL, "TEAM_LEAD");

            WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);

            assertThat(scope).isEqualTo(new WorklogVisibilityScope.MyTeams(USER_ID));
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("MEMBER 도 TEAM_LEAD 와 동일하게 본인 팀 기반 MyTeams scope 를 반환한다")
        void MEMBER_도_MyTeams_scope_를_반환한다() {
            CustomUserPrincipal principal = new CustomUserPrincipal(USER_ID, EMAIL, "MEMBER");

            WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);

            assertThat(scope).isEqualTo(new WorklogVisibilityScope.MyTeams(USER_ID));
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("비정상 입력")
    class InvalidInput {

        @Test
        @DisplayName("UserRole 에 매핑되지 않는 role 코드면 AUTH_ACCESS_DENIED 예외를 던진다")
        void 알_수_없는_role_코드면_AUTH_ACCESS_DENIED_를_던진다() {
            CustomUserPrincipal principal = new CustomUserPrincipal(USER_ID, EMAIL, "GUEST");

            assertThatThrownBy(() -> worklogVisibilityPolicy.resolve(principal))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("DEPT_HEAD 인데 부서 문맥을 복원하지 못하면 UserService 의 USER_NOT_FOUND 예외가 그대로 전파된다")
        void DEPT_HEAD_인데_부서_복원_실패면_USER_NOT_FOUND_가_전파된다() {
            CustomUserPrincipal principal = new CustomUserPrincipal(USER_ID, EMAIL, "DEPT_HEAD");
            given(userService.getDepartmentIdOrThrow(USER_ID))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> worklogVisibilityPolicy.resolve(principal))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("DIRECTOR 와 TEAM_LEAD 분기에서는 부서 조회를 호출하지 않는다")
        void DIRECTOR_와_TEAM_LEAD_분기에서는_부서_조회를_호출하지_않는다() {
            CustomUserPrincipal director = new CustomUserPrincipal(USER_ID, EMAIL, "DIRECTOR");
            CustomUserPrincipal teamLead = new CustomUserPrincipal(USER_ID, EMAIL, "TEAM_LEAD");

            worklogVisibilityPolicy.resolve(director);
            worklogVisibilityPolicy.resolve(teamLead);

            verify(userService, never()).getDepartmentIdOrThrow(USER_ID);
        }
    }
}
