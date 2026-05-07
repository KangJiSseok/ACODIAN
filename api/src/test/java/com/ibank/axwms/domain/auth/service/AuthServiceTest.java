package com.ibank.axwms.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.auth.dto.ChangePasswordApiDto;
import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.domain.auth.dto.RefreshAccessTokenApiDto;
import com.ibank.axwms.domain.auth.dto.SignupApiDto;
import com.ibank.axwms.domain.organization.department.service.DepartmentService;
import com.ibank.axwms.domain.organization.user.event.ProfileImageCommittedEvent;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService.TempUploadResult;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
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
    private DepartmentService departmentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private ProfileImageStorageService profileImageStorageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private LoginApiDto.Request request;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        request = new LoginApiDto.Request(EMAIL, RAW_PASSWORD);
        refreshToken = "refresh-token";
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

    @Test
    void 회원가입_저장에_실패하면_confirm_이벤트를_발행하지_않는다() {
        SignupApiDto.Request signupRequest = signupRequest();
        MockMultipartFile profileImage = new MockMultipartFile(
                "profile_image",
                "avatar.png",
                "image/png",
                "image-content".getBytes()
        );
        given(userRepository.existsByEmail(signupRequest.email())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.password())).willReturn(HASHED_PASSWORD);
        given(profileImageStorageService.uploadTemp(profileImage))
                .willReturn(new TempUploadResult("temp/profile/2026/04/28/avatar.png", "profile/2026/04/28/avatar.png", "https://cdn.axwms.com/profile/2026/04/28/avatar.png"));
        RuntimeException exception = new RuntimeException("db timeout");
        given(userRepository.save(any(User.class))).willThrow(exception);

        assertThatThrownBy(() -> authService.signup(signupRequest, profileImage))
                .isSameAs(exception);

        verify(eventPublisher, never()).publishEvent(any(ProfileImageCommittedEvent.class));
    }

    @Test
    void 회원가입이_성공하면_confirm_이벤트를_발행한다() {
        SignupApiDto.Request signupRequest = signupRequest();
        MockMultipartFile profileImage = new MockMultipartFile(
                "profile_image",
                "avatar.png",
                "image/png",
                "image-content".getBytes()
        );
        given(userRepository.existsByEmail(signupRequest.email())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.password())).willReturn(HASHED_PASSWORD);
        given(profileImageStorageService.uploadTemp(profileImage))
                .willReturn(new TempUploadResult("temp/profile/2026/04/28/avatar.png", "profile/2026/04/28/avatar.png", "https://cdn.axwms.com/profile/2026/04/28/avatar.png"));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        authService.signup(signupRequest, profileImage);

        verify(eventPublisher).publishEvent(any(ProfileImageCommittedEvent.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 프로필_이미지가_없으면_confirm_이벤트를_발행하지_않는다() {
        SignupApiDto.Request signupRequest = signupRequest();
        given(userRepository.existsByEmail(signupRequest.email())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.password())).willReturn(HASHED_PASSWORD);
        given(profileImageStorageService.uploadTemp(null)).willReturn(null);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        authService.signup(signupRequest, null);

        verify(eventPublisher, never()).publishEvent(any(ProfileImageCommittedEvent.class));
    }

    @Test
    void 회원가입시_활성_부서_검증을_organization_서비스_경계로_위임한다() {
        SignupApiDto.Request signupRequest = signupRequest();
        given(userRepository.existsByEmail(signupRequest.email())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.password())).willReturn(HASHED_PASSWORD);
        given(profileImageStorageService.uploadTemp(null)).willReturn(null);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        authService.signup(signupRequest, null);

        verify(departmentService).validateActiveDepartment(signupRequest.departmentId());
    }

    @Test
    void 회원가입_titleName_매핑이_실패하면_signup_전용_AUTH_SIGNUP_INVALID_TITLE_NAME을_던진다() {
        SignupApiDto.Request signupRequest = invalidTitleSignupRequest();
        given(userRepository.existsByEmail(signupRequest.email())).willReturn(false);

        assertThatThrownBy(() -> authService.signup(signupRequest, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_SIGNUP_INVALID_TITLE_NAME);

        verify(profileImageStorageService, never()).uploadTemp(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 유효한_refresh_token_이면_새_access_token_과_rotation_결과를_반환한다() {
        User user = activeUser();
        TokenService.ValidatedRefreshToken validatedRefreshToken =
                new TokenService.ValidatedRefreshToken(1L, "session-1", true);
        given(tokenService.validateRefreshToken(refreshToken)).willReturn(validatedRefreshToken);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(tokenService.issueRefreshTokens(user, validatedRefreshToken))
                .willReturn(new TokenService.RefreshedTokens("new-access-token", "rotated-refresh-token"));

        RefreshAccessTokenApiDto.Result result = authService.refresh(refreshToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.rotatedRefreshToken()).isEqualTo("rotated-refresh-token");
    }

    @Test
    void refresh_token_이_유효하지_않으면_AUTH_INVALID_REFRESH_TOKEN_을_던진다() {
        given(tokenService.validateRefreshToken(refreshToken))
                .willThrow(new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN));

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);

        verify(userRepository, never()).findById(any());
        verify(tokenService, never()).issueRefreshTokens(any(), any());
    }

    @Test
    void refresh_사용자의_고용상태가_ACTIVE_가_아니면_AUTH_LOGIN_NOT_ALLOWED_를_던진다() {
        User user = userWithStatus(EmploymentStatus.LEAVE);
        TokenService.ValidatedRefreshToken validatedRefreshToken =
                new TokenService.ValidatedRefreshToken(1L, "session-1", false);
        given(tokenService.validateRefreshToken(refreshToken)).willReturn(validatedRefreshToken);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_LOGIN_NOT_ALLOWED);

        verify(tokenService, never()).issueRefreshTokens(any(), any());
    }

    @Test
    void 로그아웃하면_refresh_token_폐기를_TokenService_에_위임한다() {
        authService.logout("refresh-token");

        verify(tokenService).revokeRefreshToken("refresh-token");
    }

    @Test
    void 현재_비밀번호가_일치하지_않으면_AUTH_PASSWORD_MISMATCH를_던진다() {
        User user = activeUser();
        ChangePasswordApiDto.Request changeRequest = changePasswordRequest("newPassword1!");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword(principal(), changeRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_PASSWORD_MISMATCH);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void 비밀번호_변경에_성공하면_새_해시로_교체한다() {
        User user = activeUser();
        ChangePasswordApiDto.Request changeRequest = changePasswordRequest("newPassword1!");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(true);
        given(passwordEncoder.encode("newPassword1!")).willReturn("new-hashed-password");

        authService.changePassword(principal(), changeRequest);

        assertThat(user.getPasswordHash()).isEqualTo("new-hashed-password");
    }

    private SignupApiDto.Request signupRequest() {
        return new SignupApiDto.Request(
                1L,
                "신규 사용자",
                EMAIL,
                RAW_PASSWORD,
                "사원",
                "팀원",
                LocalDate.of(2025, 1, 1),
                "010-1234-5678",
                EmploymentStatus.ACTIVE
        );
    }

    private SignupApiDto.Request invalidTitleSignupRequest() {
        return new SignupApiDto.Request(
                1L,
                "신규 사용자",
                EMAIL,
                RAW_PASSWORD,
                "사원",
                "대표",
                LocalDate.of(2025, 1, 1),
                "010-1234-5678",
                EmploymentStatus.ACTIVE
        );
    }

    private ChangePasswordApiDto.Request changePasswordRequest(String newPassword) {
        return new ChangePasswordApiDto.Request(RAW_PASSWORD, newPassword);
    }

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(1L, EMAIL, UserRole.MEMBER.name());
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
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }
}
