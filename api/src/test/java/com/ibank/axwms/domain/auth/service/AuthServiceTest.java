package com.ibank.axwms.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@ibank.com";
    private static final String RAW_PASSWORD = "password1!";
    private static final String HASHED_PASSWORD = "$2a$10$fake-hashed";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private LoginApiDto.Request request;

    @BeforeEach
    void setUp() {
        request = new LoginApiDto.Request(EMAIL, RAW_PASSWORD);
    }

    @Test
    void 이메일이_존재하지_않으면_AUTH_INVALID_CREDENTIALS_를_던진다() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);

        verify(tokenService, never()).issueLoginTokens(any());
    }

    @Test
    void 비밀번호가_일치하지_않으면_AUTH_INVALID_CREDENTIALS_를_던진다() {
        User user = activeUser();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);

        verify(tokenService, never()).issueLoginTokens(any());
    }

    @Test
    void 고용상태가_ACTIVE_가_아니면_AUTH_LOGIN_NOT_ALLOWED_를_던진다() {
        User user = userWithStatus(EmploymentStatus.LEAVE);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_LOGIN_NOT_ALLOWED);

        verify(tokenService, never()).issueLoginTokens(any());
    }

    @Test
    void 정상_로그인하면_access_refresh_토큰을_담은_Result_를_반환한다() {
        User user = activeUser();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(true);
        given(tokenService.issueLoginTokens(user))
                .willReturn(new TokenService.IssuedTokens("session-1", "access-token", "refresh-token"));

        LoginApiDto.Result result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    private User activeUser() {
        return userWithStatus(EmploymentStatus.ACTIVE);
    }

    private User userWithStatus(EmploymentStatus status) {
        return User.create(
                1L,
                "테스트 사용자",
                EMAIL,
                HASHED_PASSWORD,
                UserRole.MEMBER,
                status,
                "대리",
                "파트장",
                LocalDate.of(2025, 1, 1)
        );
    }
}
