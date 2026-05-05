package com.ibank.axwms.domain.organization.skill.controller;

import com.ibank.axwms.domain.organization.skill.dto.CreateSkillApiDto;
import com.ibank.axwms.domain.organization.skill.dto.GetSkillsApiDto;
import com.ibank.axwms.domain.organization.skill.dto.UpdateSkillApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "UserSkill", description = "사용자 스킬 API")
public interface UserSkillControllerDocs {

    @Operation(
            summary = "사용자 스킬 목록 조회",
            description = "특정 사용자의 보유 스킬 목록을 반환한다. 자기 자신 또는 DEPT_HEAD 가 DIRECTOR 를 조회하는 경우에는 빈 목록을 반환한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 스킬 목록을 반환한다.",
                    content = @Content(schema = @Schema(implementation = GetSkillsApiDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "대상 사용자 스킬 조회 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없다.", content = @Content)
    })
    GetSkillsApiDto.Response getSkills(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long userId
    );

    @Operation(
            summary = "사용자 스킬 등록",
            description = "특정 사용자에게 스킬을 등록한다. DIRECTOR 는 자기 자신을 제외한 사용자를 대상으로 등록하고, DEPT_HEAD 는 자기 자신과 DIRECTOR 를 제외한 같은 부서 사용자만 대상으로 등록한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "사용자 스킬 등록에 성공한다.",
                    content = @Content(schema = @Schema(implementation = EmptyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패했다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "대상 사용자 스킬 등록 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "같은 스킬명이 이미 등록되어 있다.", content = @Content)
    })
    EmptyResponse createSkill(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long userId,
            CreateSkillApiDto.Request request
    );

    @Operation(
            summary = "사용자 스킬 수정",
            description = "특정 사용자의 특정 스킬 정보를 부분 수정한다. DIRECTOR 는 자기 자신을 제외한 사용자를 대상으로 수정할 수 있고, DEPT_HEAD 는 자기 자신과 DIRECTOR 를 제외한 같은 부서 사용자만 대상으로 수정할 수 있다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 스킬 수정에 성공한다.",
                    content = @Content(schema = @Schema(implementation = EmptyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패한다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "대상 사용자 스킬 수정 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 또는 사용자 스킬을 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "같은 스킬명이 이미 등록되어 있다.", content = @Content)
    })
    EmptyResponse updateSkill(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long userId,
            @Parameter(description = "스킬 레코드 ID", example = "1") Long id,
            UpdateSkillApiDto.Request request
    );

}
