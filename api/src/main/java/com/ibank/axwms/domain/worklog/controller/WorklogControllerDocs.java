package com.ibank.axwms.domain.worklog.controller;

import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
