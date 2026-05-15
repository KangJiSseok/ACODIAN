package com.ibank.axwms.domain.tag.controller;

import com.ibank.axwms.domain.tag.dto.SearchTagApiDto;
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

@Tag(name = "Tag", description = "메타 태그 API")
public interface TagControllerDocs {

    @Operation(summary = "메타 태그 검색",
            description = "메타 태그를 태그명 LIKE (대소문자 무시) 로 검색해 페이지로 반환한다. "
                    + "worklog 등록/수정 화면의 태그 picker, file 목록의 태그 필터 등 공통 lookup 으로 사용된다. "
                    + "query 가 비어 있으면 전체 조회와 동일하며, 정렬은 사용 횟수 desc + 태그명 asc 로 고정된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "태그 검색 결과 페이지를 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content)
    })
    PageResponse<SearchTagApiDto.Response.Item> searchTag(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject SearchTagApiDto.Request request
    );
}
