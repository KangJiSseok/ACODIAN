package com.ibank.axwms.domain.organization.department.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.E2eTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DepartmentControllerE2eTest extends E2eTestSupport {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    private Long logisticsDepartmentId;
    private Long operationsDepartmentId;
    private Long dormantDepartmentId;
    private Long headlessDepartmentId;

    @BeforeEach
    void setUpData() {
        clearDatabase();
        seedDepartments();
    }

    @Test
    @DisplayName("인증 없이 활성 부서 목록을 조회하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_활성_부서_목록을_조회하면_auth_unauthorized_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")));
    }

    @Test
    @DisplayName("DIRECTOR 권한이 아니면 활성 부서 목록 조회를 AUTH_ACCESS_DENIED 로 거부한다")
    void director_권한이_아니면_활성_부서_목록_조회를_auth_access_denied로_거부한다() throws Exception {
        mockMvc.perform(apiGet("/departments")
                        .with(user("member@ibank.com").roles("MEMBER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("DIRECTOR 가 활성 부서 목록을 조회하면 집계와 부서장 이름을 함께 반환한다")
    void director_가_활성_부서_목록을_조회하면_집계와_부서장_이름을_함께_반환한다() throws Exception {
        mockMvc.perform(apiGet("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.activeDepartmentCount", is(3)))
                .andExpect(jsonPath("$.data.activeTeamCount", is(2)))
                .andExpect(jsonPath("$.data.activeUserCount", is(1)))
                .andExpect(jsonPath("$.data.departments", hasSize(3)))
                .andExpect(jsonPath("$.data.departments[0].departmentName", is("개발본부")))
                .andExpect(jsonPath("$.data.departments[2].departmentName", is("비상대응본부")))
                .andExpect(jsonPath("$.data.departments[2].departmentHeadUserId", nullValue()));
    }

    @Test
    @DisplayName("활성 팀이 남아 있으면 DEPARTMENT_HAS_ACTIVE_TEAMS 응답을 반환한다")
    void 활성_팀이_남아_있으면_department_has_active_teams_응답을_반환한다() throws Exception {
        mockMvc.perform(apiDelete("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_HAS_ACTIVE_TEAMS")));
    }

    @Test
    @DisplayName("활성 팀이 모두 비활성이면 부서를 INACTIVE 로 변경한다")
    void 활성_팀이_모두_비활성이면_부서를_inactive로_변경한다() throws Exception {
        mockMvc.perform(apiDelete("/departments/" + operationsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(operationsDepartmentId).orElseThrow();
        assertThat(updated.getStatusCode()).isEqualTo(DepartmentStatus.INACTIVE);
    }

    @Test
    @DisplayName("팀이 없는 활성 부서는 삭제를 허용한다")
    void 팀이_없는_활성_부서는_삭제를_허용한다() throws Exception {
        mockMvc.perform(apiDelete("/departments/" + headlessDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(headlessDepartmentId).orElseThrow();
        assertThat(updated.getStatusCode()).isEqualTo(DepartmentStatus.INACTIVE);
    }

    @Test
    @DisplayName("이미 inactive 인 부서는 no-op 성공을 반환한다")
    void 이미_inactive_인_부서는_no_op_성공을_반환한다() throws Exception {
        mockMvc.perform(apiDelete("/departments/" + dormantDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(dormantDepartmentId).orElseThrow();
        assertThat(updated.getStatusCode()).isEqualTo(DepartmentStatus.INACTIVE);
    }

    @Test
    @DisplayName("존재하지 않는 부서는 DEPARTMENT_NOT_FOUND 응답을 반환한다")
    void 존재하지_않는_부서는_department_not_found_응답을_반환한다() throws Exception {
        mockMvc.perform(apiDelete("/departments/999999")
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_NOT_FOUND")));
    }

    /** FK 제약을 피하기 위해 부서장의 head reference 를 먼저 해제한 뒤 테스트 데이터를 비운다. */
    private void clearDatabase() {
        java.util.List<Department> departments = departmentRepository.findAll();
        departments.forEach(department -> department.assignHeadUserId(null));
        departmentRepository.saveAll(departments);
        departmentRepository.flush();

        userTeamRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    /** GET /departments 스펙과 DELETE 시나리오를 함께 검증할 수 있는 최소 테스트 데이터를 구성한다. */
    private void seedDepartments() {
        Department logisticsDepartment = departmentRepository.save(createDepartment("개발본부", DepartmentStatus.ACTIVE));
        Department operationsDepartment = departmentRepository.save(createDepartment("운영지원본부", DepartmentStatus.ACTIVE));
        Department inactiveDepartment = departmentRepository.save(createDepartment("휴면본부", DepartmentStatus.INACTIVE));
        Department headlessDepartment = departmentRepository.save(createDepartment("비상대응본부", DepartmentStatus.ACTIVE));
        logisticsDepartmentId = logisticsDepartment.getId();
        operationsDepartmentId = operationsDepartment.getId();
        dormantDepartmentId = inactiveDepartment.getId();
        headlessDepartmentId = headlessDepartment.getId();

        User departmentHead = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "박본부",
                "director-head-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DIRECTOR
        ));
        logisticsDepartment.assignHeadUserId(departmentHead.getId());
        departmentRepository.save(logisticsDepartment);

        Team logisticsActiveTeam = teamRepository.save(createTeam(logisticsDepartment.getId(), "플랫폼개발팀", TeamStatus.ACTIVE));
        Team logisticsSecondActiveTeam = teamRepository.save(createTeam(logisticsDepartment.getId(), "아키텍처TF", TeamStatus.ACTIVE));
        Team operationsInactiveTeam = teamRepository.save(createTeam(operationsDepartment.getId(), "운영지원팀", TeamStatus.INACTIVE));
        Team operationsSecondInactiveTeam = teamRepository.save(createTeam(operationsDepartment.getId(), "운영정산TF", TeamStatus.INACTIVE));

        User activeMemberOne = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "활성사용자1",
                "active-one-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User activeMemberTwo = userRepository.save(createUser(
                operationsDepartment.getId(),
                "활성사용자2",
                "active-two-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User inactiveTeamOnlyMember = userRepository.save(createUser(
                operationsDepartment.getId(),
                "비활성팀전용사용자",
                "inactive-team-only-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));

        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), logisticsActiveTeam.getId(), false, "담당", "주담당", true));
        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), logisticsSecondActiveTeam.getId(), false, "협업", "겸임", false));
        userTeamRepository.save(UserTeam.create(activeMemberTwo.getId(), operationsInactiveTeam.getId(), true, "리드", "주담당", true));
        userTeamRepository.save(UserTeam.create(inactiveTeamOnlyMember.getId(), operationsSecondInactiveTeam.getId(), false, "담당", "겸임", false));
    }

    /** 테스트용 부서 엔티티를 상태까지 포함해 생성한다. */
    private Department createDepartment(String departmentName, DepartmentStatus status) {
        Department department = Department.create(
                departmentName,
                departmentName + " 설명"
        );
        department.changeStatus(status);
        return department;
    }

    /** 테스트용 팀 엔티티를 부서와 상태 기준으로 생성한다. */
    private Team createTeam(Long departmentId, String teamName, TeamStatus status) {
        return Team.create(
                departmentId,
                teamName,
                status,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                null
        );
    }

    /** 테스트용 사용자를 소속 부서/재직 상태/권한 기준으로 생성한다. */
    private User createUser(Long departmentId,
                            String userName,
                            String email,
                            EmploymentStatus employmentStatus,
                            UserRole role) {
        return User.create(
                departmentId,
                userName,
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                role,
                employmentStatus,
                "사원",
                null,
                LocalDate.of(2025, 1, 1)
        );
    }
}
