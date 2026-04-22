package com.ibank.axwms.domain.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.E2eTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerE2eTest extends E2eTestSupport {

    private static final String EMAIL = "e2e-login@ibank.com";
    private static final String RAW_PASSWORD = "password1!";

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);

        Department department = departmentRepository.save(Department.create(
                "E2E 테스트 부서 " + System.nanoTime(),
                "로그인 E2E 테스트 전용"));

        userRepository.save(User.create(
                department.getId(),
                "E2E 테스트 사용자",
                EMAIL,
                passwordEncoder.encode(RAW_PASSWORD),
                UserRole.MEMBER,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1)
        ));
    }

    @Test
    void 로그인에_성공하면_Authorization_헤더와_refresh_쿠키를_내려주고_바디에는_토큰_이_없다() throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, RAW_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth/refresh"))
                .andExpect(cookie().maxAge("refreshToken", (int) java.time.Duration.ofMillis(1_209_600_000L).toSeconds()))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void Set_Cookie_헤더_원문에_SameSite_속성이_포함된다() throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, RAW_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    void 비밀번호가_틀리면_401_을_반환하고_헤더와_쿠키가_비어있다() throws Exception {
        String body = """
                {"email":"%s","password":"wrong-password!"}
                """.formatted(EMAIL);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Authorization"))
                .andExpect(cookie().doesNotExist("refreshToken"));
    }
}
