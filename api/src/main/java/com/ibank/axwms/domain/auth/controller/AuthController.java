package com.ibank.axwms.domain.auth.controller;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.domain.auth.dto.RefreshAccessTokenApiDto;
import com.ibank.axwms.domain.auth.dto.SignupApiDto;
import com.ibank.axwms.domain.auth.dto.ChangePasswordApiDto;
import com.ibank.axwms.domain.auth.service.AuthService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.security.AuthTokenResponseWriter;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import com.ibank.axwms.global.security.RefreshCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final AuthTokenResponseWriter authTokenResponseWriter;
    private final RefreshCookieProperties refreshCookieProperties;

    @Override
    @PostMapping("/login")
    public EmptyResponse login(@Valid @RequestBody LoginApiDto.Request request, HttpServletResponse response) {
        LoginApiDto.Result result = authService.login(request);
        authTokenResponseWriter.writeAccessTokenHeader(response, result.accessToken());
        authTokenResponseWriter.writeRefreshCookie(response, result.refreshToken());
        return EmptyResponse.INSTANCE;
    }

    @Override
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EmptyResponse signup(
            @Valid @RequestPart("request") SignupApiDto.Request request,
            @RequestPart(value = "profile_image", required = false) MultipartFile profileImage
    ) {
        authService.signup(request, profileImage);
        return EmptyResponse.INSTANCE;
    }

    @Override
    @PostMapping("/refresh")
    public EmptyResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        RefreshAccessTokenApiDto.Result result = authService.refresh(extractRefreshToken(request));
        authTokenResponseWriter.writeAccessTokenHeader(response, result.accessToken());
        writeRotatedRefreshCookie(response, result.rotatedRefreshToken());
        return EmptyResponse.INSTANCE;
    }

    @PostMapping("/logout")
    @Override
    public EmptyResponse logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(extractRefreshToken(request));
        authTokenResponseWriter.writeExpiredRefreshCookie(response);
        return EmptyResponse.INSTANCE;
    }

    @Override
    @PostMapping("/change-password")
    public EmptyResponse changePassword(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ChangePasswordApiDto.Request request
    ) {
        authService.changePassword(principal, request);
        return EmptyResponse.INSTANCE;
    }

    /** 설정된 refresh 쿠키 이름으로 현재 요청의 refresh token 값을 찾는다. */
    private String extractRefreshToken(HttpServletRequest request) {
        Cookie refreshCookie = WebUtils.getCookie(request, refreshCookieProperties.name());
        return refreshCookie != null ? refreshCookie.getValue() : null;
    }

    /** rotation 결과가 있을 때만 refresh cookie 를 다시 기록해 controller 분기를 단순화한다. */
    private void writeRotatedRefreshCookie(HttpServletResponse response, String rotatedRefreshToken) {
        if (StringUtils.hasText(rotatedRefreshToken)) {
            authTokenResponseWriter.writeRefreshCookie(response, rotatedRefreshToken);
        }
    }
}
