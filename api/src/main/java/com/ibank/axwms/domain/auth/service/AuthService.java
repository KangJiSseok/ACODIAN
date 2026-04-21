package com.ibank.axwms.domain.auth.service;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
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

    /** 이메일과 비밀번호를 검증하고 JWT 토큰과 최소 사용자 문맥을 반환한다. */
    @Transactional
    public LoginApiDto.Response login(LoginApiDto.Request request) {
        User user = getAuthenticatedUser(request);
        validateLoginAllowed(user);

        TokenService.IssuedTokens tokens = tokenService.issueLoginTokens(user);
        return LoginApiDto.Response.of(tokens.accessToken(), tokens.refreshToken());
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
     * 자격 증명이 맞더라도 재직 상태가 ACTIVE 가 아니면 로그인 자체를 거부한다.
     * 휴직/퇴사 계정이 토큰을 발급받아 보호된 API 에 접근하지 못하도록, 토큰 발급 직전 단계에서 분리해 검사한다.
     */
    private void validateLoginAllowed(User user) {
        if (user.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_NOT_ALLOWED);
        }
    }
}
