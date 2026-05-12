package com.ibank.axwms.domain.organization.department.controller;

import com.ibank.axwms.domain.organization.department.dto.CreateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentCandidatesApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentDetailApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.dto.UpdateDepartmentApiDto;
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
            summary = "부서 선택 후보 조회",
            description = "DIRECTOR 는 모든 ACTIVE 부서를, DEPT_HEAD 는 현재 로그인 사용자의 주 소속 ACTIVE 부서만 departmentId, departmentName 으로 반환한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "권한 범위에 맞는 부서 선택 후보 목록을 반환한다.",
                    content = @Content(schema = @Schema(implementation = GetDepartmentCandidatesApiDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "DIRECTOR 또는 DEPT_HEAD 권한이 없어 접근할 수 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "현재 사용자 문맥에 해당하는 사용자를 찾을 수 없다.", content = @Content)
    })
    GetDepartmentCandidatesApiDto.Response getDepartmentCandidates(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(
            summary = "부서 상세 조회",
            description = "ACTIVE 부서의 header 와 해당 부서가 직접 소유한 ACTIVE/non-deleted 팀 목록을 반환한다. 팀이 없으면 teams 는 빈 배열이다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "부서 상세와 소유 팀 목록을 반환한다.",
                    content = @Content(schema = @Schema(implementation = GetDepartmentDetailApiDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "DIRECTOR 권한이 없어 접근할 수 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "ACTIVE 부서를 찾지 못했다.", content = @Content)
    })
    GetDepartmentDetailApiDto.Response getDepartmentDetail(@Parameter(description = "부서 ID", example = "10") Long id);

    @Operation(
            summary = "부서 수정",
            description = "활성 부서의 기본 정보를 수정한다. departmentHeadUserId 는 DIRECTOR 또는 DEPT_HEAD 역할의 사용자만 허용하며, null 이거나 필드가 생략되면 부서장을 해제한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부서 수정에 성공한다."),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패한다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "DIRECTOR 권한이 없어 접근할 수 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "활성 부서를 찾지 못했거나 부서장 사용자 ID 가 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "부서명 중복, 부서장 중복, 허용되지 않는 부서장 역할, 또는 다른 부서 소속 사용자 지정으로 충돌한다.", content = @Content)
    })
    EmptyResponse updateDepartment(
            @Parameter(description = "부서 ID", example = "10") Long id,
            UpdateDepartmentApiDto.Request request
    );

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
    @Operation(
            summary = "부서 등록",
            description = "새 부서를 등록한다. 부서명과 부서장은 기존 inactive row 까지 포함한 UNIQUE 제약을 따른다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "부서 등록에 성공한다.",
                    content = @Content(schema = @Schema(implementation = EmptyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패했거나 부서장 사용자 역할이 DEPT_HEAD 또는 DIRECTOR 가 아니다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "DIRECTOR 권한이 없어 접근할 수 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "부서장 사용자 ID 가 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "부서명 또는 부서장 UNIQUE 제약과 충돌한다.", content = @Content)
    })
    EmptyResponse createDepartment(CreateDepartmentApiDto.Request request);
}
