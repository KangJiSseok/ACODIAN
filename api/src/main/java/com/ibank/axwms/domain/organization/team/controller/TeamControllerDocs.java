package com.ibank.axwms.domain.organization.team.controller;

import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
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
}
