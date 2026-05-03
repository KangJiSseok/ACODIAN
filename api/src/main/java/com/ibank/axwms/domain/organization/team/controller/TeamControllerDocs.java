package com.ibank.axwms.domain.organization.team.controller;

import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Team", description = "팀 API")
public interface TeamControllerDocs {

    @Operation(summary = "팀 목록 조회",
            description = "로그인 사용자의 admin grant 팀과 ACTIVE membership 팀의 DISTINCT 합집합을 페이지네이션으로 조회한다. "
                    + "soft-delete 된 팀은 제외하며, 응답의 memberCount 는 ACTIVE membership 수다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 목록을 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "팀 목록 조회 권한이 없다.", content = @Content)
    })
    PageResponse<GetTeamsApiDto.Response> getTeams(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetTeamsApiDto.Request request
    );

    @Operation(summary = "팀 상태 요약 조회",
            description = "로그인 사용자의 admin grant 팀과 ACTIVE membership 팀의 DISTINCT 합집합에서 "
                    + "soft-delete 되지 않은 ACTIVE/INACTIVE/전체 팀 수를 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 상태 요약을 반환한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "팀 상태 요약 조회 권한이 없다.", content = @Content)
    })
    GetTeamSummaryApiDto.Response getTeamSummary(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(summary = "팀 상세 조회",
            description = "로그인 사용자의 admin grant 또는 ACTIVE membership 으로 접근 가능한 단일 팀 상세와 "
                    + "soft-delete 되지 않은 업무일지 집계를 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 상세를 반환한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "대상 팀에 접근할 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "팀을 찾을 수 없다.", content = @Content)
    })
    GetTeamApiDto.Response getTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id
    );

    @Operation(summary = "팀 사용자 목록 조회",
            description = "로그인 사용자의 admin grant 또는 ACTIVE membership 으로 접근 가능한 팀의 "
                    + "ACTIVE 사용자 전체 목록을 리더 우선, 사용자 ID 오름차순으로 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 사용자 목록을 반환한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "대상 팀에 접근할 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "팀을 찾을 수 없다.", content = @Content)
    })
    GetTeamUsersApiDto.Response getTeamUsers(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id
    );

    @Operation(summary = "팀 생성",
            description = "DIRECTOR 또는 DEPT_HEAD 가 새 팀을 생성하고, 요청된 admin grant 와 팀원을 함께 추가한다. "
                    + "모든 DIRECTOR 사용자도 생성된 팀의 admin grant 로 추가된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "팀을 생성한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "팀 생성 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "요청에 포함된 사용자를 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "같은 이름의 팀이 이미 존재한다.", content = @Content)
    })
    EmptyResponse createTeam(CreateTeamApiDto.Request request);

    @Operation(summary = "팀 부분 수정",
            description = "DIRECTOR 또는 DEPT_HEAD 가 대상 팀의 admin grant 를 보유한 경우 팀 기본 정보, "
                    + "admin grant, 팀원을 부분 업데이트한다. null 필드는 변경하지 않는다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀을 부분 수정한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "대상 팀 관리 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "팀 또는 사용자를 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "같은 이름의 팀이 이미 존재한다.", content = @Content)
    })
    EmptyResponse updateTeam(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "팀 ID", example = "21") Long id,
            UpdateTeamApiDto.Request request
    );
}
