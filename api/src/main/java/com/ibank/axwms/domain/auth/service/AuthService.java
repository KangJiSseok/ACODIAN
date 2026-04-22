package com.ibank.axwms.domain.auth.service;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.domain.auth.dto.RefreshAccessTokenApiDto;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

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
     * 자격 증명이 맞더라도 재직 상태가 ACTIVE 가 아니면 로그인 자체를 거부한다.
     * 휴직/퇴사 계정이 토큰을 발급받아 보호된 API 에 접근하지 못하도록, 토큰 발급 직전 단계에서 분리해 검사한다.
     */
    private void validateLoginAllowed(User user) {
        if (user.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_NOT_ALLOWED);
        }
    }
}
