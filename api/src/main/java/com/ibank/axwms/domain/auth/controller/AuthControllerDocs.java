package com.ibank.axwms.domain.auth.controller;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "Auth", description = "인증/인가 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 인증 후 accessToken 은 Authorization 응답 헤더, "
                    + "refreshToken 은 설정된 이름의 HttpOnly 쿠키(Path=/api/auth/refresh)로 전달한다. "
                    + "응답 바디는 비어 있으며, 사용자 정보는 별도 현재 사용자 조회 API 로 제공한다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인에 성공한다.",
                    headers = {
                            @Header(
                                    name = "Authorization",
                                    description = "발급된 access token. `Bearer <token>` 형식이며 이후 요청의 Authorization 헤더에 그대로 재사용한다.",
                                    schema = @Schema(type = "string")
                            ),
                            @Header(
                                    name = "Set-Cookie",
                                    description = "<configured-refresh-cookie-name>=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth/refresh; Max-Age=<jwt.refresh-token-expiration(초)>",
                                    schema = @Schema(type = "string")
                            )
                    }
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패한다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호가 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "현재 계정 상태로는 로그인할 수 없다.", content = @Content)
    })
    EmptyResponse login(
            LoginApiDto.Request request,
            @Parameter(hidden = true, in = ParameterIn.HEADER) HttpServletResponse response
    );

    @Operation(
            summary = "refresh token 으로 access token 재발급",
            description = "설정된 HttpOnly refresh cookie 를 검증해 새 access token 을 Authorization 응답 헤더로 내려준다. "
                    + "refresh token 남은 유효 시간이 전체 TTL 의 절반 이하이면 같은 세션으로 새 refresh cookie 도 함께 회전한다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "재발급에 성공한다.",
                    headers = {
                            @Header(
                                    name = "Authorization",
                                    description = "새 access token. `Bearer <token>` 형식이다.",
                                    schema = @Schema(type = "string")
                            ),
                            @Header(
                                    name = "Set-Cookie",
                                    description = "refresh token 회전이 필요할 때만 추가되는 HttpOnly 쿠키.",
                                    schema = @Schema(type = "string")
                            )
                    }
            ),
            @ApiResponse(responseCode = "401", description = "refresh token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "현재 계정 상태로는 재발급할 수 없다.", content = @Content)
    })
    EmptyResponse refresh(
            @Parameter(hidden = true, in = ParameterIn.COOKIE) HttpServletRequest request,
            @Parameter(hidden = true, in = ParameterIn.HEADER) HttpServletResponse response
    );
}
