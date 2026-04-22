package com.ibank.axwms.domain.auth.controller;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.domain.auth.service.AuthService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.security.AuthTokenResponseWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final AuthTokenResponseWriter authTokenResponseWriter;

    @Override
    @PostMapping("/login")
    public EmptyResponse login(@Valid @RequestBody LoginApiDto.Request request, HttpServletResponse response) {
        LoginApiDto.Result result = authService.login(request);
        authTokenResponseWriter.writeAccessTokenHeader(response, result.accessToken());
        authTokenResponseWriter.writeRefreshCookie(response, result.refreshToken());
        return EmptyResponse.INSTANCE;
    }
}
