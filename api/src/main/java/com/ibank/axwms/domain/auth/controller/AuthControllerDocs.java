package com.ibank.axwms.domain.auth.controller;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "인증/인가 API")
public interface AuthControllerDocs {

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 access token, refresh token, 사용자 최소 문맥을 발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인에 성공한다."),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패한다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호가 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "현재 계정 상태로는 로그인할 수 없다.", content = @Content)
    })
    LoginApiDto.Response login(LoginApiDto.Request request);
}
