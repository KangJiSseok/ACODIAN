package com.ibank.axwms.domain.organization.evaluation.controller;

import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long id,
            @ParameterObject GetUserEvaluationsApiDto.Request request
    );
}
