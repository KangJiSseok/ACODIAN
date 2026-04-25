package com.ibank.axwms.domain.organization.user.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

class UserMeProfileE2eTest extends E2eTestSupport {

    private static final String API_CONTEXT_PATH = "/api";
    private static final String EMAIL = "user-me-e2e@ibank.com";
    private static final String RAW_PASSWORD = "password1!";

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUserContext() {
        userTeamRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();

        Department department = departmentRepository.save(Department.create(
                "E2E 사용자문맥부서",
                "현재 사용자 조회 E2E 검증용 부서"
        ));
        User user = userRepository.save(User.create(
                department.getId(),
                "E2E 현재 사용자",
                EMAIL,
                passwordEncoder.encode(RAW_PASSWORD),
                UserRole.MEMBER,
                EmploymentStatus.ACTIVE,
                "과장",
                "팀장",
                LocalDate.of(2025, 1, 1)
        ));
        Team primaryTeam = teamRepository.save(Team.create(
                department.getId(),
                "E2E플랫폼팀",
                TeamStatus.ACTIVE,
                "현재 사용자 조회 E2E 기본 팀",
                LocalDate.of(2025, 1, 1),
                null
        ));
        Team secondaryTeam = teamRepository.save(Team.create(
                department.getId(),
                "E2ESCM팀",
                TeamStatus.ACTIVE,
                "현재 사용자 조회 E2E 추가 팀",
                LocalDate.of(2025, 1, 1),
                null
        ));

        userTeamRepository.save(UserTeam.create(
                user.getId(),
                primaryTeam.getId(),
                true,
                "플랫폼 총괄",
                "주담당",
                true,
                UserTeamStatus.ACTIVE
        ));
        userTeamRepository.save(UserTeam.create(
                user.getId(),
                secondaryTeam.getId(),
                false,
                "SCM 분석",
                "겸임",
                false,
                UserTeamStatus.LEFT
        ));
    }

    @Test
    @DisplayName("로그인한 사용자가 현재 사용자 조회를 호출하면 ACTIVE membership 만 반환한다")
    void 로그인한_사용자가_현재_사용자_조회를_호출하면_ACTIVE_membership_만_반환한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();

        mockMvc.perform(get("/api/users/me")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userName", is("E2E 현재 사용자")))
                .andExpect(jsonPath("$.data.teams.length()", is(1)))
                .andExpect(jsonPath("$.data.teams[0].isPrimary", is(true)))
                .andExpect(jsonPath("$.data.teams[0].teamName", is("E2E플랫폼팀")))
                .andExpect(jsonPath("$.data.teams[0].teamLeader", is(true)))
                .andExpect(jsonPath("$.data.teams[0].teamRole", is("플랫폼 총괄")))
                .andExpect(jsonPath("$.data.teams[0].allocation", is("주담당")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private String loginAndGetAuthorizationHeader() throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, RAW_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
    }
}
