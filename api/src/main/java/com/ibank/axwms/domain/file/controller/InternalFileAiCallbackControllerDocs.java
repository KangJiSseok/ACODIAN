package com.ibank.axwms.domain.file.controller;

import com.ibank.axwms.domain.file.dto.UpdateFileAiApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "InternalFileAiCallback", description = "파일 AI 콜백 내부 API")
public interface InternalFileAiCallbackControllerDocs {

    @Operation(summary = "내부용 파일 AI 요약 수정 API", description = "AI 서버가 파일 1건 요약을 마친 뒤 결과를 적용하기 위해 호출하는 시스템 내부 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 AI 요약 반영 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 파일을 찾을 수 없다.", content = @Content),
    })
    EmptyResponse updateFileAiResult(Long fileId, UpdateFileAiApiDto.Request request);

}
