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
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
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
import org.springframework.http.MediaType;

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
    private Long logisticsHeadUserId;
    private Long operationsDepartmentId;
    private Long emptyHeadDepartmentId;
    private Long dormantDepartmentId;
    private Long headlessDepartmentId;
    private Long deptHeadCandidateUserId;
    private Long logisticsCandidateUserId;
    private Long freeCandidateUserId;
    private Long duplicateHeadCandidateUserId;
    private Long directorCandidateUserId;
    private Long memberCandidateUserId;
    private Long teamLeadCandidateUserId;

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
                .andExpect(jsonPath("$.data.departments[0].departmentName", is("물류본부")))
                .andExpect(jsonPath("$.data.departments[2].departmentName", is("비상대응본부")))
                .andExpect(jsonPath("$.data.departments[2].departmentHeadUserId", nullValue()));
    }

    @Test
    @DisplayName("DIRECTOR 가 활성 부서 상세를 조회하면 header와 소유 팀 목록을 반환한다")
    void director_가_활성_부서_상세를_조회하면_header와_소유_팀_목록을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/departments/" + logisticsDepartmentId + "/detail")
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.departmentId", is(logisticsDepartmentId.intValue())))
                .andExpect(jsonPath("$.data.departmentName", is("물류본부")))
                .andExpect(jsonPath("$.data.departmentHeadUserId", is(logisticsHeadUserId.intValue())))
                .andExpect(jsonPath("$.data.departmentHeadUserName", is("박본부")))
                .andExpect(jsonPath("$.data.teams", hasSize(2)))
                .andExpect(jsonPath("$.data.teams[0].teamName", is("플랫폼개발팀")))
                .andExpect(jsonPath("$.data.teams[0].memberCount", is(1)))
                .andExpect(jsonPath("$.data.teams[0].leaderId", nullValue()))
                .andExpect(jsonPath("$.data.teams[1].teamName", is("아키텍처TF")));
    }

    @Test
    @DisplayName("DIRECTOR 가 팀이 없는 활성 부서 상세를 조회하면 빈 teams를 반환한다")
    void director_가_팀이_없는_활성_부서_상세를_조회하면_빈_teams를_반환한다() throws Exception {
        mockMvc.perform(apiGet("/departments/" + headlessDepartmentId + "/detail")
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.teams", hasSize(0)));
    }

    @Test
    @DisplayName("DIRECTOR 가 inactive 부서 상세를 조회하면 DEPARTMENT_NOT_FOUND 응답을 반환한다")
    void director_가_inactive_부서_상세를_조회하면_department_not_found_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/departments/" + dormantDepartmentId + "/detail")
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_NOT_FOUND")));
    }

    @Test
    @DisplayName("DIRECTOR 권한이 아니면 부서 상세 조회를 AUTH_ACCESS_DENIED로 거부한다")
    void director_권한이_아니면_부서_상세_조회를_auth_access_denied로_거부한다() throws Exception {
        mockMvc.perform(apiGet("/departments/" + logisticsDepartmentId + "/detail")
                        .with(user("member@ibank.com").roles("MEMBER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("소유한 ACTIVE 팀이 있으면 부서 삭제를 DEPARTMENT_HAS_ACTIVE_TEAMS 로 거부한다")
    void 소유한_active_팀이_있으면_부서_삭제를_department_has_active_teams로_거부한다() throws Exception {
        mockMvc.perform(apiDelete("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_HAS_ACTIVE_TEAMS")));

        Department updated = departmentRepository.findById(logisticsDepartmentId).orElseThrow();
        assertThat(updated.getStatusCode()).isEqualTo(DepartmentStatus.ACTIVE);
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

    @Test
    @DisplayName("DIRECTOR 가 새 부서를 등록하면 201 Created 와 빈 응답을 반환한다")
    void director_가_새_부서를_등록하면_201_created와_빈_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "플랫폼전략본부",
                                  "description": "전사 플랫폼 전략",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(deptHeadCandidateUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        Department saved = departmentRepository.findByDepartmentName("플랫폼전략본부").orElseThrow();
        assertThat(saved.getDepartmentHeadUserId()).isEqualTo(deptHeadCandidateUserId);
    }

    @Test
    @DisplayName("DIRECTOR 가 DIRECTOR 사용자를 부서장으로 지정해도 201 Created 를 반환한다")
    void director_가_director_사용자를_부서장으로_지정해도_201_created를_반환한다() throws Exception {
        mockMvc.perform(apiPost("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "전략운영본부",
                                  "description": "전사 운영 전략",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(directorCandidateUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        Department saved = departmentRepository.findByDepartmentName("전략운영본부").orElseThrow();
        assertThat(saved.getDepartmentHeadUserId()).isEqualTo(directorCandidateUserId);
    }

    @Test
    @DisplayName("중복 부서명으로 등록하면 DEPARTMENT_DUPLICATE_NAME 응답을 반환한다")
    void 중복_부서명으로_등록하면_department_duplicate_name_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "물류본부",
                                  "description": "중복 이름"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_DUPLICATE_NAME")));
    }

    @Test
    @DisplayName("존재하지 않는 부서장 사용자 ID 로 등록하면 USER_NOT_FOUND 응답을 반환한다")
    void 존재하지_않는_부서장_사용자_id로_등록하면_user_not_found_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "플랫폼전략본부",
                                  "description": "전사 플랫폼 전략",
                                  "departmentHeadUserId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("USER_NOT_FOUND")));
    }

    @Test
    @DisplayName("이미 다른 부서의 부서장인 사용자를 지정하면 DEPARTMENT_DUPLICATE_HEAD_USER 응답을 반환한다")
    void 이미_다른_부서의_부서장인_사용자를_지정하면_department_duplicate_head_user_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "플랫폼전략본부",
                                  "description": "전사 플랫폼 전략",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(logisticsHeadUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_DUPLICATE_HEAD_USER")));
    }

    @Test
    @DisplayName("TEAM_LEAD 사용자를 부서장으로 지정하면 DEPARTMENT_INVALID_HEAD_USER_ROLE 응답을 반환한다")
    void team_lead_사용자를_부서장으로_지정하면_department_invalid_head_user_role_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "전략운영본부",
                                  "description": "전사 운영 전략",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(teamLeadCandidateUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_INVALID_HEAD_USER_ROLE")));
    }

    @Test
    @DisplayName("MEMBER 사용자를 부서장으로 지정하면 DEPARTMENT_INVALID_HEAD_USER_ROLE 응답을 반환한다")
    void member_사용자를_부서장으로_지정하면_department_invalid_head_user_role_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/departments")
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "전략운영본부",
                                  "description": "전사 운영 전략",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(memberCandidateUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_INVALID_HEAD_USER_ROLE")));
    }

    @Test
    @DisplayName("인증 없이 부서를 수정하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_부서를_수정하면_auth_unauthorized_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "플랫폼전략본부",
                                  "description": "전사 플랫폼 전략"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")));
    }

    @Test
    @DisplayName("DIRECTOR 권한이 아니면 부서 수정을 AUTH_ACCESS_DENIED 로 거부한다")
    void director_권한이_아니면_부서_수정을_auth_access_denied로_거부한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("member@ibank.com").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "플랫폼전략본부",
                                  "description": "전사 플랫폼 전략"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("departmentName 이 비어 있으면 COMMON_VALIDATION_ERROR 응답을 반환한다")
    void departmentName_이_비어_있으면_common_validation_error_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "",
                                  "description": "전사 플랫폼 전략"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("COMMON_VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("departmentName 이 100자를 초과하면 COMMON_VALIDATION_ERROR 응답을 반환한다")
    void departmentName_이_100자를_초과하면_common_validation_error_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "%s",
                                  "description": "전사 플랫폼 전략"
                                }
                                """.formatted("가".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("COMMON_VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("DIRECTOR 가 활성 부서를 수정하면 200 OK 와 빈 응답을 반환한다")
    void director_가_활성_부서를_수정하면_200_ok와_빈_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "플랫폼전략본부",
                                  "description": "전사 플랫폼 전략",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(logisticsCandidateUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(logisticsDepartmentId).orElseThrow();
        assertThat(updated.getDepartmentName()).isEqualTo("플랫폼전략본부");
        assertThat(updated.getDescription()).isEqualTo("전사 플랫폼 전략");
        assertThat(updated.getDepartmentHeadUserId()).isEqualTo(logisticsCandidateUserId);
    }

    @Test
    @DisplayName("DIRECTOR 역할 사용자를 부서장으로 지정하면 수정에 성공한다")
    void director_역할_사용자를_부서장으로_지정하면_수정에_성공한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + emptyHeadDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "무부장본부",
                                  "description": "DIRECTOR 지정",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(directorCandidateUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(emptyHeadDepartmentId).orElseThrow();
        assertThat(updated.getDepartmentHeadUserId()).isEqualTo(directorCandidateUserId);
    }

    @Test
    @DisplayName("다른 부서 소속 사용자를 부서장으로 지정하면 DEPARTMENT_HEAD_USER_DEPARTMENT_MISMATCH 응답을 반환한다")
    void 다른_부서_소속_사용자를_부서장으로_지정하면_department_head_user_department_mismatch_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "물류본부",
                                  "description": "다른 부서 사용자 지정 시도",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(freeCandidateUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_HEAD_USER_DEPARTMENT_MISMATCH")));
    }

    @Test
    @DisplayName("inactive 부서를 수정하면 DEPARTMENT_NOT_FOUND 응답을 반환한다")
    void inactive_부서를_수정하면_department_not_found_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + dormantDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "휴면본부수정",
                                  "description": "수정 시도"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_NOT_FOUND")));
    }

    @Test
    @DisplayName("다른 활성 부서의 이름으로 수정하면 DEPARTMENT_DUPLICATE_NAME 응답을 반환한다")
    void 다른_활성_부서의_이름으로_수정하면_department_duplicate_name_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + emptyHeadDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "물류본부",
                                  "description": "중복 이름"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_DUPLICATE_NAME")));
    }

    @Test
    @DisplayName("존재하지 않는 부서장 사용자 ID 로 수정하면 USER_NOT_FOUND 응답을 반환한다")
    void 존재하지_않는_부서장_사용자_id로_수정하면_user_not_found_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "물류본부",
                                  "description": "수정 설명",
                                  "departmentHeadUserId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("USER_NOT_FOUND")));
    }

    @Test
    @DisplayName("다른 부서의 부서장과 충돌하면 DEPARTMENT_DUPLICATE_HEAD_USER 응답을 반환한다")
    void 다른_부서의_부서장과_충돌하면_department_duplicate_head_user_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + emptyHeadDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "무부장본부",
                                  "description": "수정 설명",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(duplicateHeadCandidateUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_DUPLICATE_HEAD_USER")));
    }

    @Test
    @DisplayName("TEAM_LEAD 사용자를 부서장으로 지정하면 DEPARTMENT_HEAD_ROLE_NOT_ALLOWED 응답을 반환한다")
    void team_lead_사용자를_부서장으로_지정하면_department_head_role_not_allowed_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + emptyHeadDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "무부장본부",
                                  "description": "TEAM_LEAD 지정 시도",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(teamLeadCandidateUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_HEAD_ROLE_NOT_ALLOWED")));
    }

    @Test
    @DisplayName("MEMBER 사용자를 부서장으로 지정하면 DEPARTMENT_HEAD_ROLE_NOT_ALLOWED 응답을 반환한다")
    void member_사용자를_부서장으로_지정하면_department_head_role_not_allowed_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + emptyHeadDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "무부장본부",
                                  "description": "MEMBER 지정 시도",
                                  "departmentHeadUserId": %d
                                }
                                """.formatted(memberCandidateUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DEPARTMENT_HEAD_ROLE_NOT_ALLOWED")));
    }

    @Test
    @DisplayName("departmentHeadUserId 가 null 이면 기존 부서장을 해제한다")
    void departmentHeadUserId_가_null_이면_기존_부서장을_해제한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "물류본부",
                                  "description": "수정 설명",
                                  "departmentHeadUserId": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(logisticsDepartmentId).orElseThrow();
        assertThat(updated.getDepartmentHeadUserId()).isNull();
    }

    @Test
    @DisplayName("departmentHeadUserId 필드를 생략해도 기존 부서장을 해제한다")
    void departmentHeadUserId_필드를_생략해도_기존_부서장을_해제한다() throws Exception {
        mockMvc.perform(apiPut("/departments/" + logisticsDepartmentId)
                        .with(user("director@ibank.com").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "물류본부",
                                  "description": "필드 생략"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(logisticsDepartmentId).orElseThrow();
        assertThat(updated.getDepartmentHeadUserId()).isNull();
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

    /** GET /departments 스펙, PUT 시나리오, DELETE 시나리오를 함께 검증할 수 있는 최소 테스트 데이터를 구성한다. */
    private void seedDepartments() {
        Department logisticsDepartment = departmentRepository.save(createDepartment("물류본부", DepartmentStatus.ACTIVE));
        Department operationsDepartment = departmentRepository.save(createDepartment("무부장본부", DepartmentStatus.ACTIVE));
        Department inactiveDepartment = departmentRepository.save(createDepartment("휴면본부", DepartmentStatus.INACTIVE));
        Department headlessDepartment = departmentRepository.save(createDepartment("비상대응본부", DepartmentStatus.ACTIVE));
        logisticsDepartmentId = logisticsDepartment.getId();
        operationsDepartmentId = operationsDepartment.getId();
        emptyHeadDepartmentId = operationsDepartment.getId();
        dormantDepartmentId = inactiveDepartment.getId();
        headlessDepartmentId = headlessDepartment.getId();

        User departmentHead = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "박본부",
                "director-head-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DIRECTOR
        ));
        logisticsHeadUserId = departmentHead.getId();
        logisticsDepartment.assignHeadUserId(logisticsHeadUserId);
        departmentRepository.save(logisticsDepartment);

        User createDeptHeadCandidate = userRepository.save(createUser(
                headlessDepartment.getId(),
                "윤후보",
                "create-candidate-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DEPT_HEAD
        ));
        deptHeadCandidateUserId = createDeptHeadCandidate.getId();

        User freeCandidate = userRepository.save(createUser(
                operationsDepartment.getId(),
                "윤후보",
                "candidate-head-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DEPT_HEAD
        ));
        freeCandidateUserId = freeCandidate.getId();

        User logisticsCandidate = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "류후보",
                "logistics-candidate-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DEPT_HEAD
        ));
        logisticsCandidateUserId = logisticsCandidate.getId();

        User duplicateHeadCandidate = userRepository.save(createUser(
                operationsDepartment.getId(),
                "중복부서장후보",
                "duplicate-head-candidate-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DEPT_HEAD
        ));
        duplicateHeadCandidateUserId = duplicateHeadCandidate.getId();
        inactiveDepartment.assignHeadUserId(duplicateHeadCandidateUserId);
        departmentRepository.save(inactiveDepartment);

        User directorCandidate = userRepository.save(createUser(
                operationsDepartment.getId(),
                "강본부장",
                "director-candidate-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DIRECTOR
        ));
        directorCandidateUserId = directorCandidate.getId();

        User teamLeadCandidate = userRepository.save(createUser(
                operationsDepartment.getId(),
                "한팀장",
                "team-lead-candidate-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.TEAM_LEAD
        ));
        teamLeadCandidateUserId = teamLeadCandidate.getId();

        User memberCandidate = userRepository.save(createUser(
                operationsDepartment.getId(),
                "박사원",
                "member-candidate-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        memberCandidateUserId = memberCandidate.getId();

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

        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), logisticsActiveTeam.getId(), false, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(activeMemberTwo.getId(), operationsInactiveTeam.getId(), true, "리드", "주담당", true, UserTeamStatus.ACTIVE));
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
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }
}
