package com.ibank.axwms.domain.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.domain.auth.entity.RefreshToken;
import com.ibank.axwms.domain.auth.repository.RefreshTokenRepository;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.security.JwtProperties;
import com.ibank.axwms.global.security.JwtTokenProvider;
import com.ibank.axwms.global.security.RefreshCookieProperties;
import com.ibank.axwms.testsupport.E2eTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = "auth.refresh-cookie.name=e2eRefreshToken")
class AuthControllerE2eTest extends E2eTestSupport {

    private static final String API_CONTEXT_PATH = "/api";
    private static final String EMAIL = "e2e-login@ibank.com";
    private static final String RAW_PASSWORD = "password1!";

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshCookieProperties refreshCookieProperties;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User activeUser;

    @BeforeEach
    void seedUser() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        Department department = departmentRepository.save(Department.create(
                "E2E 테스트 부서 " + System.nanoTime(),
                "로그인 E2E 테스트 전용"));

        activeUser = userRepository.save(User.create(
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

    @AfterEach
    void clearRefreshTokens() {
        userRepository.findByEmail(EMAIL)
                .ifPresent(user -> refreshTokenRepository.findAllByUserId(user.getId())
                        .forEach(refreshTokenRepository::delete));
    }

    @Test
    @DisplayName("로그인에 성공하면 설정된 쿠키 이름으로 Authorization 헤더와 refresh 쿠키를 내려준다")
    void 로그인에_성공하면_설정된_쿠키_이름으로_Authorization_헤더와_refresh_쿠키를_내려준다() throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, RAW_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andExpect(cookie().exists(refreshCookieProperties.name()))
                .andExpect(cookie().httpOnly(refreshCookieProperties.name(), true))
                .andExpect(cookie().path(refreshCookieProperties.name(), refreshCookieProperties.path()))
                .andExpect(cookie().maxAge(refreshCookieProperties.name(), (int) java.time.Duration.ofMillis(jwtProperties.refreshTokenExpiration()).toSeconds()))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("refresh 남은 수명이 절반 이하이면 새 Authorization 헤더와 refresh 쿠키를 함께 내려준다")
    void refresh_남은_수명이_절반_이하이면_새_Authorization_헤더와_refresh_쿠키를_함께_내려준다() throws Exception {
        String refreshToken = saveRefreshToken(activeUser, "rotate-session", createRefreshToken("rotate-session", activeUser.getId(), 20, 5));

        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/refresh")
                        .cookie(new Cookie(refreshCookieProperties.name(), refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andExpect(cookie().exists(refreshCookieProperties.name()))
                .andExpect(cookie().httpOnly(refreshCookieProperties.name(), true))
                .andExpect(cookie().path(refreshCookieProperties.name(), refreshCookieProperties.path()))
                .andExpect(header().string("Set-Cookie", containsString(refreshCookieProperties.name() + "=")))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("refresh 남은 수명이 절반 초과이면 Authorization 헤더만 재발급하고 Set-Cookie 는 추가하지 않는다")
    void refresh_남은_수명이_절반_초과이면_Authorization_헤더만_재발급하고_Set_Cookie_는_추가하지_않는다() throws Exception {
        String refreshToken = saveRefreshToken(activeUser, "keep-session", createRefreshToken("keep-session", activeUser.getId(), 5, 15));

        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/refresh")
                        .cookie(new Cookie(refreshCookieProperties.name(), refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("refresh cookie 가 없으면 401 표준 에러 응답을 반환한다")
    void refresh_cookie_가_없으면_401_표준_에러_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Authorization"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_INVALID_REFRESH_TOKEN")));
    }

    @Test
    @DisplayName("잘못된 refresh token 이면 인증 없이 접근해도 401 로 거부된다")
    void 잘못된_refresh_token_이면_인증_없이_접근해도_401_로_거부된다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/refresh")
                        .cookie(new Cookie(refreshCookieProperties.name(), "malformed-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Authorization"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_INVALID_REFRESH_TOKEN")));
    }

    @Test
    @DisplayName("inactive 사용자 refresh 는 403 으로 거부된다")
    void inactive_사용자_refresh_는_403_으로_거부된다() throws Exception {
        User inactiveUser = createUser(
                "inactive-refresh-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.LEAVE
        );
        String refreshToken = saveRefreshToken(
                inactiveUser,
                "inactive-session",
                createRefreshToken("inactive-session", inactiveUser.getId(), 20, 5)
        );

        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/refresh")
                        .cookie(new Cookie(refreshCookieProperties.name(), refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_LOGIN_NOT_ALLOWED")));
    }

    @Test
    @DisplayName("Set-Cookie 헤더 원문에는 SameSite 속성이 포함된다")
    void Set_Cookie_헤더_원문에는_SameSite_속성이_포함된다() throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, RAW_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    @DisplayName("로그아웃하면 refresh 세션을 삭제하고 만료 쿠키를 내려준다")
    void 로그아웃하면_refresh_세션을_삭제하고_만료_쿠키를_내려준다() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie refreshCookie = loginResult.getResponse().getCookie(refreshCookieProperties.name());
        String refreshToken = refreshCookie.getValue();
        String sessionId = jwtTokenProvider.parseClaims(refreshToken).getId();

        mockMvc.perform(post("/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Authorization"))
                .andExpect(cookie().value(refreshCookieProperties.name(), ""))
                .andExpect(cookie().httpOnly(refreshCookieProperties.name(), true))
                .andExpect(cookie().path(refreshCookieProperties.name(), refreshCookieProperties.path()))
                .andExpect(cookie().maxAge(refreshCookieProperties.name(), 0))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.timestamp").exists());

        org.assertj.core.api.Assertions.assertThat(refreshTokenRepository.findById(sessionId)).isEmpty();
    }

    @Test
    @DisplayName("로그아웃은 refresh 쿠키가 없어도 멱등하게 성공한다")
    void 로그아웃은_refresh_쿠키가_없어도_멱등하게_성공한다() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Authorization"))
                .andExpect(cookie().value(refreshCookieProperties.name(), ""))
                .andExpect(cookie().path(refreshCookieProperties.name(), refreshCookieProperties.path()))
                .andExpect(cookie().maxAge(refreshCookieProperties.name(), 0))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 을 반환하고 헤더와 쿠키가 비어 있다")
    void 비밀번호가_틀리면_401_을_반환하고_헤더와_쿠키가_비어있다() throws Exception {
        String body = """
                {"email":"%s","password":"wrong-password!"}
                """.formatted(EMAIL);

        mockMvc.perform(post("/api/auth/login")
                        .contextPath(API_CONTEXT_PATH)
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Authorization"))
                .andExpect(cookie().doesNotExist(refreshCookieProperties.name()));
    }

    private User createUser(String email, EmploymentStatus employmentStatus) {
        Department department = departmentRepository.save(Department.create(
                "E2E 사용자 부서 " + System.nanoTime(),
                "refresh 사용자 상태 테스트 전용"
        ));
        return userRepository.save(User.create(
                department.getId(),
                "E2E 테스트 사용자",
                email,
                passwordEncoder.encode(RAW_PASSWORD),
                UserRole.MEMBER,
                employmentStatus,
                "사원",
                null,
                LocalDate.of(2025, 1, 1)
        ));
    }

    private String saveRefreshToken(User user, String sessionId, String token) {
        refreshTokenRepository.save(RefreshToken.issue(
                sessionId,
                user.getId(),
                token,
                java.time.Duration.ofMillis(jwtProperties.refreshTokenExpiration()).toSeconds()
        ));
        return token;
    }

    /**
     * 절반 분기와 설정 기반 쿠키 추출을 함께 검증하기 위해 기본 TTL 과 무관한 exp/iat 를 가진 refresh token 을 직접 만든다.
     * 컨트롤러는 쿠키 이름만 프로퍼티에서 읽고, 토큰 구조 검증은 운영 코드와 동일한 서명 키/claim 으로 수행된다.
     */
    private String createRefreshToken(String sessionId, Long userId, long issuedSecondsAgo, long expiresSecondsAfter) {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(sessionId)
                .issuedAt(Date.from(now.minusSeconds(issuedSecondsAgo)))
                .expiration(Date.from(now.plusSeconds(expiresSecondsAfter)))
                .claim("tokenType", "REFRESH")
                .signWith(secretKey)
                .compact();
    }
}
