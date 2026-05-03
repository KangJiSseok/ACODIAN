package com.ibank.axwms.domain.organization.user.controller;

import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
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
import org.springdoc.core.annotations.ParameterObject;

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
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GetAdminCandidatesApiDto.Response.class)))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 선택 후보 조회 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "현재 사용자 문맥에 해당하는 사용자를 찾을 수 없다.", content = @Content)
    })
    List<GetAdminCandidatesApiDto.Response> getAdminCandidates(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(
            summary = "사용자 목록 조회",
            description = "DIRECTOR 또는 DEPT_HEAD 가 사용자 목록을 페이지네이션 없이 조회한다. "
                    + "userName, departmentId, positionName, employmentStatus optional filter 를 적용하며, "
                    + "employmentStatus 가 없으면 ACTIVE/LEAVE 만 포함하고 RETIRED 는 항상 제외한다. "
                    + "정렬은 DIRECTOR, DEPT_HEAD, TEAM_LEAD, MEMBER 순서다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 목록을 배열로 반환한다.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GetUsersApiDto.Response.class)))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "사용자 목록 조회 권한이 없다.", content = @Content)
    })
    List<GetUsersApiDto.Response> getUsers(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetUsersApiDto.Request request
    );
}
