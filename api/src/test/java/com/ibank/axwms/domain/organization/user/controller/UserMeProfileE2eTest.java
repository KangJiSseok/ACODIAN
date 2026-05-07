package com.ibank.axwms.domain.organization.user.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.ibank.axwms.global.security.JwtTokenProvider;
import com.ibank.axwms.testsupport.E2eTestSupport;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

class UserMeProfileE2eTest extends E2eTestSupport {

    private static final String API_CONTEXT_PATH = "/api";
    private static final String EMAIL = "user-me-e2e@ibank.com";
    private static final String PHONE = "010-1234-5678";
    private static final String RAW_PASSWORD = "password1!";
    private static final LocalDate JOIN_DATE = LocalDate.of(2025, 1, 1);

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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
                JOIN_DATE,
                PHONE,
                null
        ));
        userRepository.save(user);
        Team primaryTeam = teamRepository.save(Team.create(
                "E2E플랫폼팀",
                TeamStatus.ACTIVE,
                "현재 사용자 조회 E2E 기본 팀",
                JOIN_DATE,
                null
        ));
        Team secondaryTeam = teamRepository.save(Team.create(
                "E2ESCM팀",
                TeamStatus.ACTIVE,
                "현재 사용자 조회 E2E 추가 팀",
                JOIN_DATE,
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
                .andExpect(jsonPath("$.data.email", is(EMAIL)))
                .andExpect(jsonPath("$.data.phone", is(PHONE)))
                .andExpect(jsonPath("$.data.joinDate", is("2025-01-01")))
                .andExpect(jsonPath("$.data.employmentStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.teams.length()", is(1)))
                .andExpect(jsonPath("$.data.teams[0].isPrimary", is(true)))
                .andExpect(jsonPath("$.data.teams[0].teamName", is("E2E플랫폼팀")))
                .andExpect(jsonPath("$.data.teams[0].teamLeader").doesNotExist())
                .andExpect(jsonPath("$.data.teams[0].isLeader", is(true)))
                .andExpect(jsonPath("$.data.teams[0].teamRole", is("플랫폼 총괄")))
                .andExpect(jsonPath("$.data.teams[0].allocation", is("주담당")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("로그인한 사용자가 이미지 없이 현재 사용자 프로필을 수정하면 조직성 필드를 포함해 반영한다")
    void 로그인한_사용자가_이미지_없이_현재_사용자_프로필을_수정하면_조직성_필드를_포함해_반영한다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();
        Department changedDepartment = departmentRepository.save(Department.create(
                "E2E 변경부서",
                "현재 사용자 self update 검증용 부서"
        ));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "department_id": %d,
                          "user_name": "E2E 수정 사용자",
                          "title_name": "본부장",
                          "employment_status": "LEAVE"
                        }
                        """.formatted(changedDepartment.getId()).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/users/me")
                        .file(requestPart)
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/users/me")
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.timestamp").exists());

        User updated = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThatUserWasSelfUpdated(updated, changedDepartment.getId(), EmploymentStatus.LEAVE);
    }

    @Test
    @DisplayName("현재 사용자 titleName 을 본부장으로 수정하면 다음 로그인 access token roleCode claim 은 DIRECTOR 다")
    void 현재_사용자_titleName을_본부장으로_수정하면_다음_로그인_access_token_roleCode_claim은_DIRECTOR다() throws Exception {
        String authorizationHeader = loginAndGetAuthorizationHeader();
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "title_name": "본부장",
                          "employment_status": "ACTIVE"
                        }
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/users/me")
                        .file(requestPart)
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/users/me")
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail(EMAIL).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getRoleCode()).isEqualTo(UserRole.DIRECTOR);

        String nextAuthorizationHeader = loginAndGetAuthorizationHeader();
        Claims claims = jwtTokenProvider.parseClaims(nextAuthorizationHeader.substring("Bearer ".length()));

        org.assertj.core.api.Assertions.assertThat(claims.get("roleCode", String.class)).isEqualTo(UserRole.DIRECTOR.name());
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

    /** self update 정책상 조직성 필드와 roleCode 동기화까지 함께 허용되는지 확인한다. */
    private void assertThatUserWasSelfUpdated(User updated, Long changedDepartmentId, EmploymentStatus employmentStatus) {
        org.assertj.core.api.Assertions.assertThat(updated.getDepartmentId()).isEqualTo(changedDepartmentId);
        org.assertj.core.api.Assertions.assertThat(updated.getUserName()).isEqualTo("E2E 수정 사용자");
        org.assertj.core.api.Assertions.assertThat(updated.getTitleName()).isEqualTo("본부장");
        org.assertj.core.api.Assertions.assertThat(updated.getRoleCode()).isEqualTo(UserRole.DIRECTOR);
        org.assertj.core.api.Assertions.assertThat(updated.getEmploymentStatus()).isEqualTo(employmentStatus);
    }
}
