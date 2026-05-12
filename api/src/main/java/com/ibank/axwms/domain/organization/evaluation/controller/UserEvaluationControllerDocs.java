package com.ibank.axwms.domain.organization.evaluation.controller;

import com.ibank.axwms.domain.organization.evaluation.dto.CreateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.UpdateUserEvaluationApiDto;
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

@Tag(name = "UserEvaluation", description = "사용자 평가 API")
public interface UserEvaluationControllerDocs {

    @Operation(
            summary = "사용자 평가 이력 조회",
            description = "DIRECTOR 또는 DEPT_HEAD 가 특정 사용자의 평가 이력을 페이지로 조회한다. "
                    + "DIRECTOR 는 자기 자신을 제외한 전체 사용자를, DEPT_HEAD 는 같은 부서 TEAM_LEAD/MEMBER 만 조회할 수 있다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 평가 이력 페이지를 반환한다.",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "평가 이력 조회 권한 또는 대상 사용자 접근 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "조회 대상 사용자를 찾을 수 없다.", content = @Content)
    })
    PageResponse<GetUserEvaluationsApiDto.EvaluationSummary> getUserEvaluations(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long userId,
            @ParameterObject GetUserEvaluationsApiDto.Request request
    );

    @Operation(
            summary = "사용자 평가 등록",
            description = "DIRECTOR 또는 DEPT_HEAD 가 특정 사용자에게 평가를 등록한다. "
                    + "DIRECTOR 는 자기 자신을 제외한 사용자를, DEPT_HEAD 는 같은 부서 TEAM_LEAD/MEMBER 만 평가할 수 있다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "사용자 평가 등록에 성공한다.",
                    content = @Content(schema = @Schema(implementation = EmptyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패했다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "평가 등록 권한 또는 대상 사용자 접근 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "평가 대상 사용자를 찾을 수 없다.", content = @Content)
    })
    EmptyResponse createUserEvaluation(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long userId,
            CreateUserEvaluationApiDto.Request request
    );

    @Operation(
            summary = "사용자 평가 수정",
            description = "평가를 등록한 본인만 특정 사용자의 평가 내용을 수정한다. "
                    + "평가 레코드의 피평가자와 path 사용자 ID 가 다르면 접근 권한 오류로 처리한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 평가 수정에 성공한다.",
                    content = @Content(schema = @Schema(implementation = EmptyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패했다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "평가 수정 권한 또는 path 사용자 접근 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "평가 대상 사용자 또는 평가 레코드를 찾을 수 없다.", content = @Content)
    })
    EmptyResponse updateUserEvaluation(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long userId,
            @Parameter(description = "평가 ID", example = "501") Long id,
            UpdateUserEvaluationApiDto.Request request
    );
}
