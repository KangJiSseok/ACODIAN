package com.ibank.axwms.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TokenServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        Department department = departmentRepository.save(Department.create(
                "토큰 통합 테스트 부서 " + System.nanoTime(),
                "refresh token 통합 테스트 전용"
        ));
        user = userRepository.save(User.create(
                department.getId(),
                "통합 테스트 사용자",
                "token-integration-" + System.nanoTime() + "@ibank.com",
                "$2a$10$fake-hashed",
                UserRole.MEMBER,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        ));
    }

    @Test
    @DisplayName("남은 수명이 절반 이하이면 같은 sessionId 레코드를 새 refresh token 으로 덮어쓴다")
    void 남은_수명이_절반_이하이면_같은_sessionId_레코드를_새_refresh_token_으로_덮어쓴다() {
        String sessionId = "rotate-session";
        String originalToken = saveRefreshToken(sessionId, refreshToken(sessionId, user.getId(), 20, 5));

        TokenService.ValidatedRefreshToken validatedRefreshToken = tokenService.validateRefreshToken(originalToken);
        TokenService.RefreshedTokens refreshedTokens = tokenService.issueRefreshTokens(user, validatedRefreshToken);

        RefreshToken savedToken = refreshTokenRepository.findById(sessionId).orElseThrow();
        JwtTokenProvider.RefreshTokenClaims rotatedClaims =
                jwtTokenProvider.parseRefreshToken(refreshedTokens.rotatedRefreshToken());

        assertThat(validatedRefreshToken.shouldRotateRefreshToken()).isTrue();
        assertThat(refreshedTokens.rotatedRefreshToken()).isNotBlank();
        assertThat(savedToken.getSessionId()).isEqualTo(sessionId);
        assertThat(savedToken.getToken()).isEqualTo(refreshedTokens.rotatedRefreshToken());
        assertThat(savedToken.getToken()).isNotEqualTo(originalToken);
        assertThat(rotatedClaims.sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("남은 수명이 절반 초과이면 기존 Redis 레코드를 유지한다")
    void 남은_수명이_절반_초과이면_기존_Redis_레코드를_유지한다() {
        String sessionId = "keep-session";
        String originalToken = saveRefreshToken(sessionId, refreshToken(sessionId, user.getId(), 5, 15));

        TokenService.ValidatedRefreshToken validatedRefreshToken = tokenService.validateRefreshToken(originalToken);
        TokenService.RefreshedTokens refreshedTokens = tokenService.issueRefreshTokens(user, validatedRefreshToken);

        RefreshToken savedToken = refreshTokenRepository.findById(sessionId).orElseThrow();

        assertThat(validatedRefreshToken.shouldRotateRefreshToken()).isFalse();
        assertThat(refreshedTokens.rotatedRefreshToken()).isNull();
        assertThat(savedToken.getToken()).isEqualTo(originalToken);
    }

    @Test
    @DisplayName("다른 세션 레코드는 refresh 처리에 영향받지 않는다")
    void 다른_세션_레코드는_refresh_처리에_영향받지_않는다() {
        String targetSessionId = "target-session";
        String otherSessionId = "other-session";
        String targetToken = saveRefreshToken(targetSessionId, refreshToken(targetSessionId, user.getId(), 20, 5));
        String otherToken = saveRefreshToken(otherSessionId, refreshToken(otherSessionId, user.getId(), 5, 15));

        TokenService.ValidatedRefreshToken validatedRefreshToken = tokenService.validateRefreshToken(targetToken);
        tokenService.issueRefreshTokens(user, validatedRefreshToken);

        RefreshToken otherSavedToken = refreshTokenRepository.findById(otherSessionId).orElseThrow();

        assertThat(otherSavedToken.getToken()).isEqualTo(otherToken);
    }

    private String saveRefreshToken(String sessionId, String token) {
        refreshTokenRepository.save(RefreshToken.issue(
                sessionId,
                user.getId(),
                token,
                jwtTokenProvider.getRefreshTokenTtlSeconds()
        ));
        return token;
    }

    /**
     * 절반 이하/초과 분기를 안정적으로 재현하기 위해 현재 시각 기준 iat/exp 를 직접 지정한 refresh token 을 만든다.
     * 운영 코드와 같은 서명 키/claim 구조를 사용해야 validateRefreshToken 이 실제와 동일하게 동작한다.
     */
    private String refreshToken(String sessionId, Long userId, long issuedSecondsAgo, long expiresSecondsAfter) {
        Instant now = Instant.now();
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Instant issuedAt = now.minusSeconds(issuedSecondsAgo);
        Instant expiration = now.plusSeconds(expiresSecondsAfter);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(sessionId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .claim("tokenType", "REFRESH")
                .signWith(secretKey)
                .compact();
    }
}
