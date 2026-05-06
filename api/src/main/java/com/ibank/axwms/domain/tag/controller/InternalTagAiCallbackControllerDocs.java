package com.ibank.axwms.domain.tag.controller;

import com.ibank.axwms.domain.tag.dto.GetTagsAiApiDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "InternalTagAiCallback", description = "태그 AI 콜백 내부 API")
public interface InternalTagAiCallbackControllerDocs {

    @Operation(summary = "내부용 태그 목록 조회 API", description = "업무일지 태그의 전체 목록을 조회하는 내부용 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "태그 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자.", content = @Content),
    })
    GetTagsAiApiDto.Response getTags();

}
