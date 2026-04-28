package com.ibank.axwms.domain.auth.service;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.domain.auth.dto.RefreshAccessTokenApiDto;
import com.ibank.axwms.domain.auth.dto.SignupApiDto;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService;
import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.user.event.ProfileImageCommittedEvent;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService.TempUploadResult;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ProfileImageStorageService profileImageStorageService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 이메일과 비밀번호를 검증하고 발급된 access/refresh 토큰 쌍을 반환한다.
     * 토큰은 Controller 경계에서 Authorization 헤더와 HttpOnly 쿠키로 분리 전송되므로, Service 는 바디 포맷을 알지 않는다.
     */
    @Transactional
    public LoginApiDto.Result login(LoginApiDto.Request request) {
        User user = getAuthenticatedUser(request);
        validateLoginAllowed(user);

        TokenService.IssuedTokens tokens = tokenService.issueLoginTokens(user);
        return new LoginApiDto.Result(tokens.accessToken(), tokens.refreshToken());
    }

    /**
     * 공개 회원가입 요청을 처리한다.
     * auth 가 공개 진입점을 소유하되, 사용자 role/title 규칙과 엔티티 생성 불변식은 user 도메인 타입을 재사용한다.
     */
    @Transactional
    public void signup(SignupApiDto.Request request, MultipartFile profileImage) {
        validateDuplicateEmail(request.email());
        validateActiveDepartment(request.departmentId());

        UserRole roleCode = UserRole.findByTitleName(request.titleName())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_SIGNUP_INVALID_TITLE_NAME));
        String passwordHash = passwordEncoder.encode(request.password());
        TempUploadResult tempUpload = profileImageStorageService.uploadTemp(profileImage);
        String profileImageUrl = tempUpload != null ? tempUpload.finalUrl() : null;

        userRepository.save(User.create(
                request.departmentId(),
                request.userName(),
                request.email(),
                passwordHash,
                roleCode,
                request.employmentStatus(),
                request.positionName(),
                request.titleName(),
                request.joinDate(),
                request.phone(),
                profileImageUrl
        ));

        if (tempUpload != null) {
            eventPublisher.publishEvent(new ProfileImageCommittedEvent(tempUpload.tempKey(), tempUpload.finalKey()));
        }
    }

    /**
     * refresh token 을 검증한 뒤 현재 사용자 상태를 다시 확인하고 access token 을 재발급한다.
     * refresh 회전 여부와 저장소 overwrite 정책은 TokenService 가 판단하고, AuthService 는 사용자 상태 검증을 담당한다.
     */
    @Transactional
    public RefreshAccessTokenApiDto.Result refresh(String refreshToken) {
        TokenService.ValidatedRefreshToken validatedRefreshToken = tokenService.validateRefreshToken(refreshToken);
        User user = findRefreshUser(validatedRefreshToken.userId());
        validateLoginAllowed(user);

        TokenService.RefreshedTokens refreshedTokens = tokenService.issueRefreshTokens(user, validatedRefreshToken);
        return new RefreshAccessTokenApiDto.Result(
                refreshedTokens.accessToken(),
                refreshedTokens.rotatedRefreshToken()
        );
    }

    /**
     * 현재 브라우저의 refresh token 이 가리키는 세션을 종료한다.
     * refresh 쿠키가 없거나 이미 만료됐더라도 logout API 는 멱등 성공 정책을 따르므로, Service 도 예외 없이 no-op 으로 처리한다.
     */
    @Transactional
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    /**
     * 이메일로 User 를 조회하고 비밀번호 해시까지 일치해야 반환한다.
     * 계정 미존재와 비밀번호 불일치를 동일한 AUTH_INVALID_CREDENTIALS 로 묶어, 이메일 존재 여부가 응답으로 노출되는 enumeration 공격 소재를 차단한다.
     */
    private User getAuthenticatedUser(LoginApiDto.Request request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        return user;
    }

    /**
     * refresh token subject 로 지정된 현재 사용자를 다시 조회한다.
     * 토큰 안의 userId 가 더 이상 유효한 사용자를 가리키지 않으면 재발급 전체를 무효 refresh token 으로 취급한다.
     */
    private User findRefreshUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN));
    }

    /**
     * 이미 가입된 이메일이면 회원가입을 거부한다.
     * 로그인과 달리 signup 은 이메일 중복 여부를 명시적으로 알려도 되는 계약이라 별도 에러 코드로 선제 차단한다.
     */
    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_SIGNUP_DUPLICATE_EMAIL);
        }
    }

    /**
     * 활성 상태의 부서만 회원가입 대상으로 허용한다.
     * 존재하지 않거나 비활성인 부서는 모두 가입 불가로 처리해, 휴면 조직 하위로 사용자가 생성되는 것을 막는다.
     */
    private void validateActiveDepartment(Long departmentId) {
        departmentRepository.findByIdAndStatusCode(departmentId, DepartmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    /**
     * 자격 증명이 맞더라도 재직 상태가 ACTIVE 가 아니면 로그인 자체를 거부한다.
     * 휴직/퇴사 계정이 토큰을 발급받아 보호된 API 에 접근하지 못하도록, 토큰 발급 직전 단계에서 분리해 검사한다.
     */
    private void validateLoginAllowed(User user) {
        if (user.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_NOT_ALLOWED);
        }
    }
}
