package com.ibank.axwms.domain.file.controller;

import com.ibank.axwms.domain.file.dto.GetFilesApiDto;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "File", description = "파일 API")
public interface FileControllerDocs {

    @Operation(summary = "파일 목록 조회",
            description = "로그인 사용자가 접근 가능한 (admin grant ∪ ACTIVE membership 의 미삭제 team) worklog 에 첨부된 파일을 "
                    + "최신 등록순으로 페이지 조회한다. 각 행에는 소속 업무의 요약(제목/팀/작성자/마감일/업무시간/AI 요약/AI 상태/선행업무수)이 함께 반환된다. "
                    + "소프트 삭제된 파일과 worklog 는 제외된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 목록을 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content)
    })
    PageResponse<GetFilesApiDto.Response.Item> getFiles(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetFilesApiDto.Request request
    );
}
