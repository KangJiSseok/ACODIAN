package com.ibank.axwms.domain.organization.department.controller;

import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(
            summary = "부서 비활성화",
            description = "부서를 soft-delete 한다. ACTIVE 팀이 남아 있으면 실패하고, 이미 INACTIVE 인 부서는 no-op 성공으로 처리한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부서 비활성화에 성공한다."),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "DIRECTOR 권한이 없어 접근할 수 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "부서를 찾지 못했다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "활성 팀이 남아 있어 비활성화할 수 없다.", content = @Content)
    })
    EmptyResponse deleteDepartment(@Parameter(description = "부서 ID", example = "10") Long id);
}
