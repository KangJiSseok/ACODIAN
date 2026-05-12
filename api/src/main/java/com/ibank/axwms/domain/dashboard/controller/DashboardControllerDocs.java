package com.ibank.axwms.domain.dashboard.controller;

import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Dashboard", description = "대시보드 조회 API")
public interface DashboardControllerDocs {

    @Operation(summary = "대시보드 조회",
            description = "scope 파라미터에 따라 4가지 응답 중 하나를 반환한다. "
                    + "ME 는 모든 인증 사용자, DEPARTMENT_COMPARISON 은 DIRECTOR 만, "
                    + "DEPARTMENT_DETAIL 은 DIRECTOR 또는 자기 부서를 관리하는 DEPT_HEAD, "
                    + "TEAM_DETAIL 은 DIRECTOR 또는 팀 소속 부서의 DEPT_HEAD 또는 팀의 TEAM_LEAD 가 접근 가능하다. "
                    + "응답은 sealed interface 다형성 직렬화이며 scope 디스크리미네이터로 타입을 식별한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대시보드 응답"),
            @ApiResponse(responseCode = "400", description = "필수 파라미터 누락 또는 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "현재 role 로는 해당 scope 를 조회할 수 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "자원이 없거나 자원에 대한 접근 권한이 없다.", content = @Content)
    })
    GetDashboardApiDto.Response getDashboard(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetDashboardApiDto.Request request
    );
}
