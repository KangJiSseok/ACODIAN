package com.ibank.axwms.domain.organization.department.controller;

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
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)));
    }

    @Test
    @DisplayName("DIRECTOR 권한이 아니면 활성 부서 목록 조회를 AUTH_ACCESS_DENIED 로 거부한다")
    void director_권한이_아니면_활성_부서_목록_조회를_auth_access_denied로_거부한다() throws Exception {
        mockMvc.perform(apiGet("/departments")
                        .with(user("member@ibank.com").roles("MEMBER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)));
    }

    @Test
    @DisplayName("DIRECTOR 가 활성 부서 목록을 조회하면 집계와 부서장 이름을 함께 반환한다")
    void director_가_활성_부서_목록을_조회하면_집계와_부서장_이름을_함께_반환한다() throws Exception {
        mockMvc.perform(apiGet("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.activeDepartmentCount", is(2)))
                .andExpect(jsonPath("$.data.activeTeamCount", is(2)))
                .andExpect(jsonPath("$.data.activeUserCount", is(2)))
                .andExpect(jsonPath("$.data.departments", hasSize(2)))
                .andExpect(jsonPath("$.data.departments[0].departmentName", is("물류본부")))
                .andExpect(jsonPath("$.data.departments[0].departmentHeadUserName", is("박본부")))
                .andExpect(jsonPath("$.data.departments[1].departmentName", is("무부장본부")))
                .andExpect(jsonPath("$.data.departments[1].departmentHeadUserId", nullValue()))
                .andExpect(jsonPath("$.data.departments[1].departmentHeadUserName", nullValue()));
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

    /** GET /departments 스펙의 집계 조건을 검증할 수 있는 최소 테스트 데이터를 구성한다. */
    private void seedDepartments() {
        Department logisticsDepartment = departmentRepository.save(createDepartment("물류본부", DepartmentStatus.ACTIVE));
        Department emptyHeadDepartment = departmentRepository.save(createDepartment("무부장본부", DepartmentStatus.ACTIVE));
        Department inactiveDepartment = departmentRepository.save(createDepartment("휴면본부", DepartmentStatus.INACTIVE));

        User departmentHead = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "박본부",
                "director-head-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DIRECTOR
        ));
        logisticsDepartment.assignHeadUserId(departmentHead.getId());
        departmentRepository.save(logisticsDepartment);

        Team logisticsActiveTeam = teamRepository.save(createTeam(logisticsDepartment.getId(), "물류혁신TF", TeamStatus.ACTIVE));
        Team emptyHeadActiveTeam = teamRepository.save(createTeam(emptyHeadDepartment.getId(), "운영지원TF", TeamStatus.ACTIVE));
        Team logisticsInactiveTeam = teamRepository.save(createTeam(logisticsDepartment.getId(), "휴면팀", TeamStatus.INACTIVE));
        Team inactiveDepartmentActiveTeam = teamRepository.save(createTeam(inactiveDepartment.getId(), "휴면본부활성팀", TeamStatus.ACTIVE));

        User activeMemberOne = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "활성사용자1",
                "active-one-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User activeMemberTwo = userRepository.save(createUser(
                emptyHeadDepartment.getId(),
                "활성사용자2",
                "active-two-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User leaveMember = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "휴직사용자",
                "leave-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.LEAVE,
                UserRole.MEMBER
        ));
        User inactiveTeamOnlyMember = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "비활성팀전용사용자",
                "inactive-team-only-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User inactiveDepartmentTeamMember = userRepository.save(createUser(
                inactiveDepartment.getId(),
                "비활성부서팀사용자",
                "inactive-department-team-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));

        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), logisticsActiveTeam.getId(), false, "담당", "주담당", true));
        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), emptyHeadActiveTeam.getId(), false, "협업", "겸임", false));
        userTeamRepository.save(UserTeam.create(activeMemberTwo.getId(), emptyHeadActiveTeam.getId(), true, "리드", "주담당", true));
        userTeamRepository.save(UserTeam.create(leaveMember.getId(), logisticsActiveTeam.getId(), false, "담당", "주담당", true));
        userTeamRepository.save(UserTeam.create(inactiveTeamOnlyMember.getId(), logisticsInactiveTeam.getId(), false, "담당", "주담당", true));
        userTeamRepository.save(UserTeam.create(inactiveDepartmentTeamMember.getId(), inactiveDepartmentActiveTeam.getId(), false, "담당", "주담당", true));
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
