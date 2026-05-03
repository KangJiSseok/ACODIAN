package com.ibank.axwms.domain.worklog.controller;

import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Worklog", description = "업무일지 API")
public interface WorklogControllerDocs {

    @Operation(summary = "업무 등록", description = "로그인 사용자의 권한으로 업무를 등록한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "업무 등록에 성공한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "대상 팀에 대한 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "등록 대상 팀을 찾을 수 없다.", content = @Content)
    })
    CreateWorklogApiDto.Response createWorklog(
            CreateWorklogApiDto.Request request,
            List<MultipartFile> files,
            CustomUserPrincipal principal
    );

    @Operation(summary = "업무 목록 조회",
            description = "로그인 사용자의 역할에 따라 가시 범위가 달라진다. "
                    + "DIRECTOR 는 전체, DEPT_HEAD 는 본인 부서 산하 ACTIVE 팀의 모든 업무, "
                    + "TEAM_LEAD 는 본인이 ACTIVE 멤버인 팀들의 모든 업무, MEMBER 는 본인이 작성한 업무를 조회한다. "
                    + "소프트 삭제된 업무는 제외되며 행마다 선행 업무 개수가 함께 반환된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업무 목록을 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content)
    })
    PageResponse<GetWorklogsApiDto.Response.Item> getWorklogs(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetWorklogsApiDto.Request request
    );

    @Operation(summary = "업무일지 검색",
            description = "업무 제목 LIKE 검색과 팀/상태/중요도/작성자/태그/기간 필터를 조합한다. "
                    + "가시 범위는 업무 목록 조회와 동일하며, 정렬은 created_at 내림차순으로 고정된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 결과 페이지를 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content)
    })
    PageResponse<SearchWorklogsApiDto.Response.Item> searchWorklogs(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject SearchWorklogsApiDto.Request request
    );
}
