package com.ibank.axwms.domain.organization.department.controller;

import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Department", description = "부서 API")
public interface DepartmentControllerDocs {

    @Operation(
            summary = "활성 부서 목록 및 집계 조회",
            description = "활성 부서 목록과 활성 부서/팀/사용자 집계를 한 번에 반환한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "활성 부서 목록과 집계를 반환한다.",
                    content = @Content(schema = @Schema(implementation = GetDepartmentsApiDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "DIRECTOR 권한이 없어 접근할 수 없다.", content = @Content)
    })
    GetDepartmentsApiDto.Response getDepartments();
}
