package com.ibank.axwms.domain.organization.department.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    @DisplayName("활성 부서 집계와 목록이 있으면 API 응답 DTO로 조립한다")
    void 활성_부서_집계와_목록이_있으면_api_응답_dto로_조립한다() {
        given(departmentRepository.getActiveDepartmentOverview())
                .willReturn(new DepartmentOverviewProjection(2, 2, 2));
        given(departmentRepository.findActiveDepartments())
                .willReturn(List.of(
                        new DepartmentListItemProjection(
                                10L,
                                "물류본부",
                                "전사 물류 운영 총괄",
                                1001L,
                                "박본부",
                                LocalDateTime.of(2026, 4, 1, 9, 0),
                                LocalDateTime.of(2026, 4, 20, 9, 0)
                        ),
                        new DepartmentListItemProjection(
                                11L,
                                "무부장본부",
                                "부서장 없는 활성 부서",
                                null,
                                null,
                                LocalDateTime.of(2026, 4, 2, 9, 0),
                                LocalDateTime.of(2026, 4, 21, 9, 0)
                        )
                ));

        GetDepartmentsApiDto.Response result = departmentService.getDepartments();

        assertThat(result.activeDepartmentCount()).isEqualTo(2);
        assertThat(result.activeTeamCount()).isEqualTo(2);
        assertThat(result.activeUserCount()).isEqualTo(2);
        assertThat(result.departments())
                .extracting(
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentId,
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentName,
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentHeadUserId,
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentHeadUserName
                )
                .containsExactly(
                        Tuple.tuple(10L, "물류본부", 1001L, "박본부"),
                        Tuple.tuple(11L, "무부장본부", null, null)
                );
    }

    @Test
    @DisplayName("활성 팀이 없으면 부서를 INACTIVE 로 변경한다")
    void 활성_팀이_없으면_부서를_inactive로_변경한다() {
        Department department = Department.create("운영지원본부", "설명");
        given(departmentRepository.findById(10L)).willReturn(java.util.Optional.of(department));
        given(teamRepository.existsByDepartmentIdAndStatusCode(10L, TeamStatus.ACTIVE)).willReturn(false);

        departmentService.deleteDepartment(10L);

        assertThat(department.getStatusCode()).isEqualTo(DepartmentStatus.INACTIVE);
    }

    @Test
    @DisplayName("활성 팀이 남아 있으면 DEPARTMENT_HAS_ACTIVE_TEAMS 예외를 던진다")
    void 활성_팀이_남아_있으면_department_has_active_teams_예외를_던진다() {
        Department department = Department.create("개발본부", "설명");
        given(departmentRepository.findById(10L)).willReturn(java.util.Optional.of(department));
        given(teamRepository.existsByDepartmentIdAndStatusCode(10L, TeamStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> departmentService.deleteDepartment(10L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_HAS_ACTIVE_TEAMS);
    }

    @Test
    @DisplayName("이미 inactive 인 부서는 no-op 성공으로 처리한다")
    void 이미_inactive_인_부서는_no_op_성공으로_처리한다() {
        Department department = Department.create("휴면본부", "설명");
        department.changeStatus(DepartmentStatus.INACTIVE);
        given(departmentRepository.findById(10L)).willReturn(java.util.Optional.of(department));

        departmentService.deleteDepartment(10L);

        assertThat(department.getStatusCode()).isEqualTo(DepartmentStatus.INACTIVE);
    }

    @Test
    @DisplayName("부서가 없으면 DEPARTMENT_NOT_FOUND 예외를 던진다")
    void 부서가_없으면_department_not_found_예외를_던진다() {
        assertThatThrownBy(() -> departmentService.deleteDepartment(10L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
