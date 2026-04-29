package com.ibank.axwms.domain.auth.controller;

import com.ibank.axwms.domain.auth.dto.LoginApiDto;
import com.ibank.axwms.domain.auth.dto.SignupApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Auth", description = "인증/인가 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 인증 후 accessToken 은 Authorization 응답 헤더, "
                    + "refreshToken 은 설정된 이름의 HttpOnly 쿠키(Path=/api/auth)로 전달한다. "
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
                                    description = "<configured-refresh-cookie-name>=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=<jwt.refresh-token-expiration(초)>",
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
            HttpServletResponse response
    );

    @Operation(
            summary = "회원가입",
            description = "비인증 사용자가 multipart/form-data 로 계정을 생성한다. "
                    + "요청 JSON part(`request`)에는 부서/사용자/직책/재직 등 상태 정보를 담고, "
                    + "선택 file part(`profile_image`)가 있으면 프로필 이미지를 함께 업로드한다. "
                    + "signup 성공 시 토큰 헤더/쿠키는 발급하지 않는다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입에 성공한다."),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패했거나 지원하지 않는 title_name 이다.", content = @Content),
            @ApiResponse(responseCode = "413", description = "multipart 업로드 크기 제한을 초과했다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "활성 부서를 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일이다.", content = @Content),
            @ApiResponse(responseCode = "503", description = "프로필 이미지 업로드를 처리할 수 없다. 잠시 후 다시 시도해야 한다.", content = @Content)
    })
    @PostMapping("/signup")
    EmptyResponse signup(
            @Parameter(description = "회원가입 필수 JSON part. multipart part name 은 `request` 이다.")
            @RequestPart("request")
            SignupApiDto.Request request,
            @Parameter(description = "선택 프로필 이미지 file part. multipart part name 은 `profile_image` 이다.")
            @RequestPart(value = "profile_image", required = false)
            MultipartFile profileImage
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
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "로그아웃",
            description = "HttpOnly refresh 쿠키가 가리키는 현재 세션을 종료한다. "
                    + "refresh cookie 는 /api/auth 하위 인증 엔드포인트에서 자동 전송되며, 로그아웃은 이를 사용해 현재 세션을 종료한다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃에 성공한다. refresh 쿠키가 없거나 이미 만료된 경우에도 멱등적으로 성공한다.",
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "refreshToken=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0",
                            schema = @Schema(type = "string")
                    )
            )
    })
    @PostMapping("/logout")
    EmptyResponse logout(
            HttpServletRequest request,
            HttpServletResponse response
    );
}
