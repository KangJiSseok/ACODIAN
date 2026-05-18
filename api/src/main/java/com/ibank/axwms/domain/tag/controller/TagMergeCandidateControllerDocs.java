package com.ibank.axwms.domain.tag.controller;

import com.ibank.axwms.domain.tag.TagMergeCandidateStatus;
import com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "TagMergeCandidate", description = "태그 병합 후보 API")
public interface TagMergeCandidateControllerDocs {

    @Operation(summary = "태그 병합 후보 저장",
            description = "AI가 추천한 태그 병합 후보를 운영 검토 대상으로 저장한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장된 병합 후보 목록을 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content)
    })
    TagMergeCandidateApiDto.Response createCandidates(TagMergeCandidateApiDto.Request request);

    @Operation(summary = "AI 태그 병합 후보 생성",
            description = "Spring 권한 경계 안에서 AI 서버에 후보 생성을 요청하고 생성 결과를 운영 검토 대상으로 저장한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 및 저장된 병합 후보 목록을 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "503", description = "AI 서버에서 후보를 생성하지 못했다.", content = @Content)
    })
    TagMergeCandidateApiDto.Response generateCandidates(TagMergeCandidateApiDto.GenerateRequest request);

    @Operation(summary = "태그 병합 후보 조회",
            description = "저장된 태그 병합 후보를 상태 조건으로 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "병합 후보 목록을 반환한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content)
    })
    TagMergeCandidateApiDto.Response getCandidates(
            @Parameter(description = "후보 상태 필터") TagMergeCandidateStatus statusCode
    );

    @Operation(summary = "태그 병합",
            description = "저장된 병합 후보를 승인해 source 태그의 업무일지 연결을 target 태그로 병합하고 source 태그를 soft-delete 처리한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "태그 병합을 완료한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "병합 후보 또는 태그를 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 처리된 병합 후보다.", content = @Content)
    })
    void mergeCandidate(
            @Parameter(description = "태그 병합 후보 ID") Long mergeCandidateId
    );
}
