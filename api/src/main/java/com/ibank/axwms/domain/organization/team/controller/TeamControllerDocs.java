package com.ibank.axwms.domain.organization.team.controller;

import com.ibank.axwms.domain.organization.team.dto.BulkUpsertTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.DeleteTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamDetailApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamStatusApiDto;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Team", description = "팀 API")
public interface TeamControllerDocs {

    @Operation(summary = "팀 목록 조회", description = "skeleton 단계에서 팀 목록 조회 응답 계약을 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 목록을 반환한다.", content = @Content(schema = @Schema(implementation = GetTeamsApiDto.Response.Item.class))))
    PageResponse<GetTeamsApiDto.Response.Item> getTeams(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            GetTeamsApiDto.Request request
    );

    @Operation(summary = "팀 상세 조회", description = "skeleton 단계에서 단일 팀 상세 응답 계약을 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 상세를 반환한다.", content = @Content(schema = @Schema(implementation = GetTeamDetailApiDto.Response.class))))
    GetTeamDetailApiDto.Response getTeamDetail(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            Long id
    );

    @Operation(summary = "팀 사용자 목록 조회", description = "skeleton 단계에서 특정 팀 membership 사용자 목록 응답 계약을 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    PageResponse<GetTeamUsersApiDto.Response.Item> getTeamUsers(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            Long id,
            GetTeamUsersApiDto.Request request
    );

    @Operation(summary = "팀 업무일지 목록 조회", description = "skeleton 단계에서 특정 팀 기준 업무일지 목록 응답 계약을 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    PageResponse<GetTeamWorklogsApiDto.Response.Item> getTeamWorklogs(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            Long id,
            GetTeamWorklogsApiDto.Request request
    );

    @Operation(summary = "팀 생성", description = "skeleton 단계에서 팀 생성 응답 계약만 먼저 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    CreateTeamApiDto.Response createTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            CreateTeamApiDto.Request request
    );

    @Operation(summary = "팀 수정", description = "skeleton 단계에서 팀 수정 응답 계약만 먼저 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    UpdateTeamApiDto.Response updateTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            Long id,
            UpdateTeamApiDto.Request request
    );

    @Operation(summary = "팀 상태 변경", description = "skeleton 단계에서 soft-delete 와 분리된 팀 상태 변경 응답 계약만 먼저 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    UpdateTeamStatusApiDto.Response updateTeamStatus(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            Long id,
            UpdateTeamStatusApiDto.Request request
    );

    @Operation(summary = "팀 사용자 일괄 반영", description = "skeleton 단계에서 legacy members/bulk 를 대체하는 canonical users/bulk 응답 계약만 먼저 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    BulkUpsertTeamUsersApiDto.Response bulkUpsertTeamUsers(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            Long id,
            BulkUpsertTeamUsersApiDto.Request request
    );

    @Operation(summary = "팀 삭제", description = "skeleton 단계에서 팀 soft-delete 응답 계약만 먼저 고정한다.")
    @SecurityRequirement(name = "bearerAuth")
    DeleteTeamApiDto.Response deleteTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            Long id
    );
}
