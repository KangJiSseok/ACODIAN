package com.ibank.axwms.domain.organization.team.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.TeamAdmin;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamAdminRepository;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.testsupport.E2eTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

class TeamControllerE2eTest extends E2eTestSupport {

    private static final String RAW_PASSWORD = "password1!";

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamAdminRepository teamAdminRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorklogRepository worklogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long visibleTeamId;
    private Long adminOnlyTeamId;
    private Long deniedTeamId;
    private Long deletedTeamId;
    private Long deptHeadAdminUserId;
    private String callerEmail;

    @BeforeEach
    void setUpData() {
        clearDatabase();
        seedTeams();
    }

    @Test
    @DisplayName("로그인한 사용자가 ACTIVE membership 팀 상세를 조회하면 공통 응답 래핑과 DEPT_HEAD 관리자를 반환한다")
    void 로그인한_사용자가_active_membership_팀_상세를_조회하면_공통_응답_래핑과_DEPT_HEAD_관리자를_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/" + visibleTeamId)
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.teamId", is(visibleTeamId.intValue())))
                .andExpect(jsonPath("$.data.teamName", is("상세조회팀")))
                .andExpect(jsonPath("$.data.statusCode", is("ACTIVE")))
                .andExpect(jsonPath("$.data.description", is("상세조회팀 설명")))
                .andExpect(jsonPath("$.data.teamLeaderName", is("호출자")))
                .andExpect(jsonPath("$.data.startDate", is("2026-04-01")))
                .andExpect(jsonPath("$.data.expectedEndDate", is("2026-12-31")))
                .andExpect(jsonPath("$.data.deptHeadAdminUserId", is(deptHeadAdminUserId.intValue())))
                .andExpect(jsonPath("$.data.deptHeadAdminUsername", is("사업부장관리자")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("로그인한 사용자가 admin grant 팀 상세를 조회하면 membership 없이도 성공한다")
    void 로그인한_사용자가_admin_grant_팀_상세를_조회하면_membership_없이도_성공한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/" + adminOnlyTeamId)
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.teamId", is(adminOnlyTeamId.intValue())))
                .andExpect(jsonPath("$.data.teamName", is("관리전용상세팀")))
                .andExpect(jsonPath("$.data.teamLeaderId", nullValue()))
                .andExpect(jsonPath("$.data.deptHeadAdminUserId", nullValue()))
                .andExpect(jsonPath("$.data.deptHeadAdminUsername", nullValue()));
    }

    @Test
    @DisplayName("로그인한 사용자가 팀 사용자 목록을 조회하면 ACTIVE 팀원을 리더 우선으로 반환한다")
    void 로그인한_사용자가_팀_사용자_목록을_조회하면_ACTIVE_팀원을_리더_우선으로_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/" + visibleTeamId + "/users")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items[0].isLeader", is(true)))
                .andExpect(jsonPath("$.data.items[0].userName", is("호출자")))
                .andExpect(jsonPath("$.data.items[0].positionName", is("사원")))
                .andExpect(jsonPath("$.data.items[0].teamRole", is("리더")))
                .andExpect(jsonPath("$.data.items[1].isLeader", is(false)))
                .andExpect(jsonPath("$.data.items[1].userName", is("구성원")))
                .andExpect(jsonPath("$.data.items[1].positionName", is("사원")))
                .andExpect(jsonPath("$.data.items[1].teamRole", is("구성원")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("visible scope 밖 팀 사용자 목록을 조회하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void visible_scope_밖_팀_사용자_목록을_조회하면_auth_access_denied_응답을_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/" + deniedTeamId + "/users")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("없는 팀이나 soft-delete 팀 사용자 목록을 조회하면 TEAM_NOT_FOUND 응답을 반환한다")
    void 없는_팀이나_soft_delete_팀_사용자_목록을_조회하면_team_not_found_응답을_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/999999/users")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("TEAM_NOT_FOUND")));

        mockMvc.perform(apiGet("/teams/" + deletedTeamId + "/users")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("TEAM_NOT_FOUND")));
    }

    @Test
    @DisplayName("visible scope 밖 팀 상세를 조회하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void visible_scope_밖_팀_상세를_조회하면_auth_access_denied_응답을_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/" + deniedTeamId)
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("없는 팀이나 soft-delete 팀 상세를 조회하면 TEAM_NOT_FOUND 응답을 반환한다")
    void 없는_팀이나_soft_delete_팀_상세를_조회하면_team_not_found_응답을_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/999999")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("TEAM_NOT_FOUND")));

        mockMvc.perform(apiGet("/teams/" + deletedTeamId)
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("TEAM_NOT_FOUND")));
    }

    @Test
    @DisplayName("로그인한 사용자가 팀 상태 요약을 조회하면 visible scope 기준 집계를 반환한다")
    void 로그인한_사용자가_팀_상태_요약을_조회하면_visible_scope_기준_집계를_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(apiGet("/teams/summary")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.activeTeamCount", is(2)))
                .andExpect(jsonPath("$.data.inactiveTeamCount", is(1)))
                .andExpect(jsonPath("$.data.totalTeamCount", is(3)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /** FK 제약을 피하기 위해 업무일지부터 테스트 데이터를 비운다. */
    private void clearDatabase() {
        worklogRepository.deleteAll();
        teamAdminRepository.deleteAll();
        userTeamRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    /** HTTP 계층에서 visible scope, 응답 래핑, 에러 변환을 함께 검증할 수 있는 최소 데이터를 구성한다. */
    private void seedTeams() {
        Department department = departmentRepository.save(createDepartment());
        User caller = userRepository.save(createUser(department.getId(), "호출자", "team-detail-caller", UserRole.MEMBER));
        User member = userRepository.save(createUser(department.getId(), "구성원", "team-detail-member", UserRole.MEMBER));
        User deptHeadAdmin = userRepository.save(createUser(department.getId(), "사업부장관리자", "dept-head-admin", UserRole.DEPT_HEAD));
        callerEmail = caller.getEmail();

        Team visibleTeam = teamRepository.save(createTeam("상세조회팀", TeamStatus.ACTIVE));
        Team adminOnlyTeam = teamRepository.save(createTeam("관리전용상세팀", TeamStatus.ACTIVE));
        Team inactiveAdminTeam = teamRepository.save(createTeam("비활성요약팀", TeamStatus.INACTIVE));
        Team deniedTeam = teamRepository.save(createTeam("권한없는상세팀", TeamStatus.ACTIVE));
        Team deletedTeam = teamRepository.save(createDeletedTeam("삭제상세팀"));

        userTeamRepository.save(UserTeam.create(caller.getId(), visibleTeam.getId(), true, "리더", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(member.getId(), visibleTeam.getId(), false, "구성원", "겸임", false, UserTeamStatus.ACTIVE));
        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), adminOnlyTeam.getId()));
        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), inactiveAdminTeam.getId()));
        teamAdminRepository.save(TeamAdmin.grant(deptHeadAdmin.getId(), visibleTeam.getId()));

        visibleTeamId = visibleTeam.getId();
        adminOnlyTeamId = adminOnlyTeam.getId();
        deniedTeamId = deniedTeam.getId();
        deletedTeamId = deletedTeam.getId();
        deptHeadAdminUserId = deptHeadAdmin.getId();
    }

    private String loginAndGetAuthorizationHeader() throws Exception {
        MvcResult result = mockMvc.perform(apiPost("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(callerEmail, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
    }

    private Department createDepartment() {
        Department department = Department.create("팀상세E2E부서", "팀 상세 E2E 검증용 부서");
        department.changeStatus(DepartmentStatus.ACTIVE);
        return department;
    }

    private User createUser(Long departmentId, String userName, String emailPrefix, UserRole role) {
        return User.create(
                departmentId,
                userName,
                emailPrefix + "-" + System.nanoTime() + "@ibank.com",
                passwordEncoder.encode(RAW_PASSWORD),
                role,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }

    private Team createTeam(String teamName, TeamStatus status) {
        return Team.create(
                teamName,
                status,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

    private Team createDeletedTeam(String teamName) {
        Team team = createTeam(teamName, TeamStatus.ACTIVE);
        team.markDeleted(LocalDateTime.of(2026, 5, 1, 0, 0));
        return team;
    }

}
