package com.ibank.axwms.domain.organization.department.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.dto.CreateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.dto.UpdateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

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
    @DisplayName("DEPT_HEAD 사용자를 부서장으로 지정하면 이름과 설명과 부서장을 수정한다")
    void dept_head_사용자를_부서장으로_지정하면_이름과_설명과_부서장을_수정한다() {
        Department department = Department.create("물류본부", "기존 설명");
        department.assignHeadUserId(1001L);
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("플랫폼전략본부", "새 설명", 2001L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(userRepository.findById(2001L)).willReturn(java.util.Optional.of(createUser(10L, 2001L, UserRole.DEPT_HEAD)));

        departmentService.updateDepartment(10L, request);

        assertThat(department.getDepartmentName()).isEqualTo("플랫폼전략본부");
        assertThat(department.getDescription()).isEqualTo("새 설명");
        assertThat(department.getDepartmentHeadUserId()).isEqualTo(2001L);
    }

    @Test
    @DisplayName("DIRECTOR 사용자를 부서장으로 지정하면 수정에 성공한다")
    void director_사용자를_부서장으로_지정하면_수정에_성공한다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("물류본부", "새 설명", 3001L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(userRepository.findById(3001L)).willReturn(java.util.Optional.of(createUser(10L, 3001L, UserRole.DIRECTOR)));

        departmentService.updateDepartment(10L, request);

        assertThat(department.getDepartmentHeadUserId()).isEqualTo(3001L);
    }

    @Test
    @DisplayName("departmentHeadUserId 가 null 이면 기존 부서장을 해제한다")
    void departmentHeadUserId_가_null_이면_기존_부서장을_해제한다() {
        Department department = Department.create("물류본부", "기존 설명");
        department.assignHeadUserId(1001L);
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("플랫폼전략본부", "새 설명", null);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));

        departmentService.updateDepartment(10L, request);

        assertThat(department.getDepartmentName()).isEqualTo("플랫폼전략본부");
        assertThat(department.getDescription()).isEqualTo("새 설명");
        assertThat(department.getDepartmentHeadUserId()).isNull();
    }

    @Test
    @DisplayName("inactive 부서를 수정하면 DEPARTMENT_NOT_FOUND 예외를 던진다")
    void inactive_부서를_수정하면_department_not_found_예외를_던진다() {
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("플랫폼전략본부", "새 설명", null);

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("자기 자신을 제외한 부서명 중복이면 예외를 던진다")
    void 자기_자신을_제외한_부서명_중복이면_예외를_던진다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("운영지원본부", "새 설명", null);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(departmentRepository.existsByDepartmentNameAndIdNot("운영지원본부", 10L)).willReturn(true);

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_DUPLICATE_NAME);
    }

    @Test
    @DisplayName("부서장 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 부서장_사용자가_없으면_user_not_found_예외를_던진다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("물류본부", "새 설명", 999L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("TEAM_LEAD 는 부서장 역할로 허용되지 않는다")
    void team_lead_는_부서장_역할로_허용되지_않는다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("물류본부", "새 설명", 1001L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(userRepository.findById(1001L)).willReturn(java.util.Optional.of(createUser(10L, 1001L, UserRole.TEAM_LEAD)));

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_HEAD_ROLE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("MEMBER 는 부서장 역할로 허용되지 않는다")
    void member_는_부서장_역할로_허용되지_않는다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("물류본부", "새 설명", 1001L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(userRepository.findById(1001L)).willReturn(java.util.Optional.of(createUser(10L, 1001L, UserRole.MEMBER)));

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_HEAD_ROLE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("비허용 역할 사용자는 다른 부서의 부서장이어도 역할 제한 예외를 우선 반환한다")
    void 비허용_역할_사용자는_다른_부서의_부서장이어도_역할_제한_예외를_우선_반환한다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("물류본부", "새 설명", 1001L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(userRepository.findById(1001L)).willReturn(java.util.Optional.of(createUser(20L, 1001L, UserRole.MEMBER)));

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_HEAD_ROLE_NOT_ALLOWED);
        then(departmentRepository).should(never()).existsByDepartmentHeadUserIdAndIdNot(1001L, 10L);
    }

    @Test
    @DisplayName("허용 역할 사용자가 다른 부서의 부서장과 충돌하면 중복 예외를 던진다")
    void 허용_역할_사용자가_다른_부서의_부서장과_충돌하면_중복_예외를_던진다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("물류본부", "새 설명", 1001L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(userRepository.findById(1001L)).willReturn(java.util.Optional.of(createUser(10L, 1001L, UserRole.DIRECTOR)));
        given(departmentRepository.existsByDepartmentHeadUserIdAndIdNot(1001L, 10L)).willReturn(true);

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_DUPLICATE_HEAD_USER);
    }

    @Test
    @DisplayName("허용 역할 사용자라도 다른 부서 소속이면 부서장으로 지정할 수 없다")
    void 허용_역할_사용자라도_다른_부서_소속이면_부서장으로_지정할_수_없다() {
        Department department = Department.create("물류본부", "기존 설명");
        UpdateDepartmentApiDto.Request request = new UpdateDepartmentApiDto.Request("물류본부", "새 설명", 1001L);
        given(departmentRepository.findByIdAndStatusCode(10L, DepartmentStatus.ACTIVE)).willReturn(java.util.Optional.of(department));
        given(userRepository.findById(1001L)).willReturn(java.util.Optional.of(createUser(20L, 1001L, UserRole.DEPT_HEAD)));

        assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_HEAD_USER_DEPARTMENT_MISMATCH);
        then(departmentRepository).should(never()).existsByDepartmentHeadUserIdAndIdNot(1001L, 10L);
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

    @Test
    @DisplayName("부서장 ID 가 없으면 부서장 없는 새 부서를 저장한다")
    void 부서장_id가_없으면_부서장_없는_새_부서를_저장한다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "전사 전략", null);
        given(departmentRepository.save(any(Department.class))).willAnswer(invocation -> invocation.getArgument(0));

        departmentService.createDepartment(request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        then(departmentRepository).should().save(captor.capture());
        Department saved = captor.getValue();
        assertThat(saved.getDepartmentName()).isEqualTo("플랫폼전략본부");
        assertThat(saved.getDescription()).isEqualTo("전사 전략");
        assertThat(saved.getDepartmentHeadUserId()).isNull();
    }

    @Test
    @DisplayName("부서장 사용자가 DEPT_HEAD 이면 새 부서를 저장한다")
    void 부서장_사용자가_dept_head이면_새_부서를_저장한다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "전사 전략", 101L);
        User headUser = User.create(10L, "윤후보", "candidate@ibank.com", "hash", UserRole.DEPT_HEAD, EmploymentStatus.ACTIVE, "차장", "부서장 후보", java.time.LocalDate.of(2025, 1, 1), null, null);
        given(userRepository.findById(101L)).willReturn(java.util.Optional.of(headUser));
        given(departmentRepository.save(any(Department.class))).willAnswer(invocation -> invocation.getArgument(0));

        departmentService.createDepartment(request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        then(departmentRepository).should().save(captor.capture());
        Department saved = captor.getValue();
        assertThat(saved.getDepartmentName()).isEqualTo("플랫폼전략본부");
        assertThat(saved.getDescription()).isEqualTo("전사 전략");
        assertThat(saved.getDepartmentHeadUserId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("부서장 사용자가 DIRECTOR 이면 새 부서를 저장한다")
    void 부서장_사용자가_director이면_새_부서를_저장한다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "전사 전략", 101L);
        User headUser = User.create(10L, "박본부", "director@ibank.com", "hash", UserRole.DIRECTOR, EmploymentStatus.ACTIVE, "상무", "본부장", java.time.LocalDate.of(2025, 1, 1), null, null);
        given(userRepository.findById(101L)).willReturn(java.util.Optional.of(headUser));
        given(departmentRepository.save(any(Department.class))).willAnswer(invocation -> invocation.getArgument(0));

        departmentService.createDepartment(request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        then(departmentRepository).should().save(captor.capture());
        Department saved = captor.getValue();
        assertThat(saved.getDepartmentHeadUserId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("부서명이 중복되면 예외를 던진다")
    void 부서명이_중복되면_예외를_던진다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("개발본부", "설명", null);
        given(departmentRepository.existsByDepartmentName("개발본부")).willReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_DUPLICATE_NAME);
    }

    @Test
    @DisplayName("등록용 부서장 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 등록용_부서장_사용자가_없으면_user_not_found_예외를_던진다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "설명", 999L);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("부서장 사용자가 TEAM_LEAD 이면 역할 예외를 던진다")
    void 부서장_사용자가_team_lead이면_역할_예외를_던진다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "설명", 101L);
        User headUser = User.create(10L, "김리드", "teamlead@ibank.com", "hash", UserRole.TEAM_LEAD, EmploymentStatus.ACTIVE, "과장", "팀장", java.time.LocalDate.of(2025, 1, 1), null, null);
        given(userRepository.findById(101L)).willReturn(java.util.Optional.of(headUser));

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_INVALID_HEAD_USER_ROLE);
    }

    @Test
    @DisplayName("부서장 사용자가 MEMBER 이면 역할 예외를 던진다")
    void 부서장_사용자가_member이면_역할_예외를_던진다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "설명", 101L);
        User headUser = User.create(10L, "김사원", "member@ibank.com", "hash", UserRole.MEMBER, EmploymentStatus.ACTIVE, "사원", null, java.time.LocalDate.of(2025, 1, 1), null, null);
        given(userRepository.findById(101L)).willReturn(java.util.Optional.of(headUser));

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_INVALID_HEAD_USER_ROLE);
    }

    @Test
    @DisplayName("이미 다른 부서의 head 인 사용자를 지정하면 예외를 던진다")
    void 이미_다른_부서의_head_인_사용자를_지정하면_예외를_던진다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "설명", 101L);
        User headUser = User.create(10L, "윤후보", "candidate@ibank.com", "hash", UserRole.DEPT_HEAD, EmploymentStatus.ACTIVE, "차장", "부서장 후보", java.time.LocalDate.of(2025, 1, 1), null, null);
        given(userRepository.findById(101L)).willReturn(java.util.Optional.of(headUser));
        given(departmentRepository.existsByDepartmentHeadUserId(101L)).willReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_DUPLICATE_HEAD_USER);
    }

    @Test
    @DisplayName("역할이 허용되지 않으면 head 중복보다 역할 예외를 우선한다")
    void 역할이_허용되지_않으면_head_중복보다_역할_예외를_우선한다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "설명", 101L);
        User headUser = User.create(10L, "김리드", "teamlead@ibank.com", "hash", UserRole.TEAM_LEAD, EmploymentStatus.ACTIVE, "과장", "팀장", java.time.LocalDate.of(2025, 1, 1), null, null);
        given(userRepository.findById(101L)).willReturn(java.util.Optional.of(headUser));

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_INVALID_HEAD_USER_ROLE);
        then(departmentRepository).should(never()).existsByDepartmentHeadUserId(101L);
    }

    private User createUser(Long departmentId, Long userId, UserRole roleCode) {
        return User.create(
                departmentId,
                "테스트사용자",
                "user-" + userId + "@ibank.com",
                "$2a$10$abcdefghijklmnopqrstuv",
                roleCode,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }
}
