package com.ibank.axwms.domain.worklog.controller;

import com.ibank.axwms.domain.worklog.dto.ApplyWorklogAiTagsApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "InternalWorklogAiCallback", description = "업무일지 AI 콜백 내부 API")
public interface InternalWorklogAiCallbackControllerDocs {

    @Operation(summary = "내부용 업무일지 AI 요약 수정용 API", description = "업무일지의 AI 요약을 내부적으로 적용하기 위한 시스템 내부 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업무일지 AI 관련 내용 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
    })
    EmptyResponse updateWorklogAiResult(Long worklogId, UpdateWorklogAiApiDto.Request request);

    @Operation(summary = "내부용 업무일지 AI 태그 적용 API", description = "AI가 선택한 기존 태그와 새로 제안한 태그를 특정 업무일지에 AI 생성 태그로 적용한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업무일지 AI 태그 적용 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 업무일지를 찾을 수 없다.", content = @Content),
    })
    EmptyResponse applyAiGeneratedTags(Long worklogId, ApplyWorklogAiTagsApiDto.Request request);

}
