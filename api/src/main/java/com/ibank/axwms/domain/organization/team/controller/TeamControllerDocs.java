package com.ibank.axwms.domain.organization.team.controller;

import com.ibank.axwms.domain.organization.team.dto.BulkUpsertTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamDetailApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamStatusApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
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
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Team", description = "팀 API")
public interface TeamControllerDocs {

    @Operation(summary = "팀 목록 조회", description = "최신 team spec 기준으로 역할별 조회 범위의 팀 목록을 페이지네이션 조회한다. 전체 부서로 요청할때에는 departmentId = null 로 주면 된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 목록을 반환한다."))
    PageResponse<GetTeamsApiDto.Response.Item> getTeams(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetTeamsApiDto.Request request
    );

    @Operation(summary = "팀 상단 집계 조회", description = "페이지네이션 목록과 분리된 팀 상단 집계를 반환한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 상단 집계를 반환한다.", content = @Content(schema = @Schema(implementation = GetTeamsSummaryApiDto.Response.class))))
    GetTeamsSummaryApiDto.Response getTeamSummary(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetTeamsSummaryApiDto.Request request
    );

    @Operation(summary = "팀 상세 조회", description = "최신 team spec 기준으로 단일 팀 상세와 업무일지 집계를 반환한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 상세를 반환한다.", content = @Content(schema = @Schema(implementation = GetTeamDetailApiDto.Response.class))))
    GetTeamDetailApiDto.Response getTeamDetail(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id
    );

    @Operation(summary = "팀 사용자 목록 조회", description = "최신 team spec 기준으로 특정 팀 사용자 목록을 반환한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 사용자 목록을 반환한다."))
    PageResponse<GetTeamUsersApiDto.Response.Item> getTeamUsers(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id,
            @ParameterObject GetTeamUsersApiDto.Request request
    );

    @Operation(summary = "팀 업무일지 목록 조회", description = "최신 team spec 기준으로 특정 팀 업무일지 목록을 반환한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 업무일지 목록을 반환한다."))
    PageResponse<GetTeamWorklogsApiDto.Response.Item> getTeamWorklogs(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id,
            @ParameterObject GetTeamWorklogsApiDto.Request request
    );

    @Operation(summary = "팀 생성", description = "최신 team spec 기준으로 새 팀을 생성한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "팀 생성에 성공한다.", content = @Content(schema = @Schema(implementation = EmptyResponse.class))))
    EmptyResponse createTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            CreateTeamApiDto.Request request
    );

    @Operation(summary = "팀 수정", description = "최신 team spec 기준으로 팀 기본 정보를 수정한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 수정에 성공한다.", content = @Content(schema = @Schema(implementation = EmptyResponse.class))))
    EmptyResponse updateTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id,
            UpdateTeamApiDto.Request request
    );

    @Operation(summary = "팀 상태 변경", description = "soft-delete 와 분리된 팀 운영 상태를 변경한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 상태 변경에 성공한다.", content = @Content(schema = @Schema(implementation = EmptyResponse.class))))
    EmptyResponse updateTeamStatus(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id,
            UpdateTeamStatusApiDto.Request request
    );

    @Operation(summary = "팀 사용자 일괄 반영", description = "최신 team spec 기준으로 팀 사용자 추가/제거를 일괄 반영한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 사용자 일괄 반영에 성공한다.", content = @Content(schema = @Schema(implementation = EmptyResponse.class))))
    EmptyResponse bulkUpsertTeamUsers(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id,
            BulkUpsertTeamUsersApiDto.Request request
    );

    @Operation(summary = "팀 삭제", description = "팀을 soft-delete 한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "팀 삭제에 성공한다.", content = @Content(schema = @Schema(implementation = EmptyResponse.class))))
    EmptyResponse deleteTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id
    );
}
