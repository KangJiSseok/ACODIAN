package com.ibank.axwms.domain.organization.user.controller;

import com.ibank.axwms.domain.organization.user.dto.GetManagerCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "User", description = "사용자 API")
public interface UserControllerDocs {

    @Operation(
            summary = "현재 로그인 사용자 조회",
            description = "JWT access token 으로 인증된 현재 사용자의 프로필/이메일/전화번호/입사일/재직 상태/부서/소속 팀 문맥을 반환한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "현재 로그인 사용자 정보를 반환한다.",
                    content = @Content(schema = @Schema(implementation = GetMyProfileApiDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "현재 사용자 문맥에 해당하는 사용자를 찾을 수 없다.", content = @Content)
    })
    GetMyProfileApiDto.Response getMyProfile(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(
            summary = "관리자 선택 후보 조회",
            description = "JWT access token 의 인증된 role 기준으로 관리자 선택 후보를 반환한다. "
                    + "DIRECTOR 는 DEPT_HEAD 사용자 전체를, DEPT_HEAD 는 자기 자신만 조회한다. "
                    + "요청 query/body 로 role 값을 받지 않는다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "관리자 선택 후보 목록을 배열로 반환한다.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GetManagerCandidatesApiDto.Response.class)))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 선택 후보 조회 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "현재 사용자 문맥에 해당하는 사용자를 찾을 수 없다.", content = @Content)
    })
    List<GetManagerCandidatesApiDto.Response> getManagerCandidates(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );
}
