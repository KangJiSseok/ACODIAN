package com.ibank.axwms.domain.organization.evaluation.controller;

import com.ibank.axwms.domain.organization.evaluation.dto.CreateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
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

    @Operation(summary = "사용자 평가 이력 조회", description = "특정 사용자의 평가 이력을 페이지네이션 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "사용자 평가 이력을 반환한다."))
    PageResponse<GetUserEvaluationsApiDto.Response.Item> getUserEvaluations(
            CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long id,
            @ParameterObject GetUserEvaluationsApiDto.Request request
    );

    @Operation(summary = "사용자 평가 등록", description = "특정 사용자에 대한 평가를 등록한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "사용자 평가 등록에 성공했다.", content = @Content(schema = @Schema(implementation = EmptyResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증에 실패한다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(
                    responseCode = "403",
                    description = "평가 대상 접근 권한이 없거나 자기 자신에게 평가를 작성하려 해 거부된다. EVALUATION_ACCESS_DENIED 또는 EVALUATION_SELF_WRITE_FORBIDDEN 을 반환한다.",
                    content = @Content
            ),
            @ApiResponse(responseCode = "404", description = "평가 대상 사용자를 찾지 못했다.", content = @Content)
    })
    EmptyResponse createUserEvaluation(
            CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long id,
            CreateUserEvaluationApiDto.Request request
    );
}
