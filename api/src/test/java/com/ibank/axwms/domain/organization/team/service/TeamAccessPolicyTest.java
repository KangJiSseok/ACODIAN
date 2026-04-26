package com.ibank.axwms.domain.organization.team.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamAccessPolicyTest {

    private final TeamAccessPolicy teamAccessPolicy = new TeamAccessPolicy();

    @Test
    @DisplayName("DIRECTOR 는 쓰기 권한 검증을 통과한다")
    void DIRECTOR_는_쓰기_권한_검증을_통과한다() {
        assertThatCode(() -> teamAccessPolicy.assertWritable(new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MEMBER 는 쓰기 권한 검증에서 접근 거부된다")
    void MEMBER_는_쓰기_권한_검증에서_접근_거부된다() {
        assertThatThrownBy(() -> teamAccessPolicy.assertWritable(new CustomUserPrincipal(1L, "member@ibank.com", "MEMBER")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 가 자기 부서면 부서 ownership 검증을 통과한다")
    void DEPT_HEAD_가_자기_부서면_부서_ownership_검증을_통과한다() {
        assertThatCode(() -> teamAccessPolicy.assertDepartmentOwnership(
                new CustomUserPrincipal(2L, "head@ibank.com", "DEPT_HEAD"),
                10L,
                10L
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TEAM_LEAD 가 다른 팀을 읽으려 하면 팀 ownership 검증에서 접근 거부된다")
    void TEAM_LEAD_가_다른_팀을_읽으려_하면_접근_거부된다() {
        assertThatThrownBy(() -> teamAccessPolicy.assertTeamOwnership(
                new CustomUserPrincipal(3L, "lead@ibank.com", "TEAM_LEAD"),
                21L,
                22L
        )).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 도 다른 팀이면 팀 ownership helper 에서 바로 통과하지 않는다")
    void DEPT_HEAD_도_다른_팀이면_팀_ownership_helper_에서_거부된다() {
        assertThatThrownBy(() -> teamAccessPolicy.assertTeamOwnership(
                new CustomUserPrincipal(4L, "head@ibank.com", "DEPT_HEAD"),
                21L,
                22L
        )).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DIRECTOR 는 팀 ownership helper 를 통과한다")
    void DIRECTOR_는_팀_ownership_helper_를_통과한다() {
        assertThatCode(() -> teamAccessPolicy.assertTeamOwnership(
                new CustomUserPrincipal(5L, "director@ibank.com", "DIRECTOR"),
                21L,
                22L
        )).doesNotThrowAnyException();
    }
}
