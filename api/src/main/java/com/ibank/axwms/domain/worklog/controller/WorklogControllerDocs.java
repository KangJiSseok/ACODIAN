package com.ibank.axwms.domain.worklog.controller;

import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogDetailApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogFilterOptionsApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchPredecessorApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogStatusApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
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

    @Operation(summary = "업무 등록",
            description = "로그인 사용자의 권한으로 업무를 등록한다. "
                    + "files part 는 첨부 파일이 있을 때만 전달한다.")
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

    @Operation(summary = "업무 상세 조회",
            description = "로그인 사용자가 접근 가능한 업무 한 건의 상세 정보를 조회한다. "
                    + "본문, 첨부 파일 목록, 태그 이름 목록, 직접 연결된 선행 업무(depth 1), 상태 변경 이력을 함께 반환한다. "
                    + "권한 밖이거나 존재하지 않는 worklog 는 404 로 응답한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업무 상세를 반환한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "업무가 없거나 가시 범위 밖이다.", content = @Content)
    })
    GetWorklogDetailApiDto.Response getWorklogDetail(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "업무 ID", example = "501") Long worklogId
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

    @Operation(summary = "업무일지 검색 필터 옵션 조회",
            description = "검색 화면 진입 시 사용할 필터 옵션을 한 번에 반환한다. "
                    + "사용자가 볼 수 있는 팀 목록, 각 팀의 ACTIVE 멤버, 전체 태그를 포함한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "필터 옵션을 반환한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content)
    })
    GetWorklogFilterOptionsApiDto.Response getFilterOptions(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(summary = "업무일지 부분 수정 (본문 + 선행 + 첨부 파일 통합)",
            description = "작성자 본인이 자기 업무일지를 한 번의 multipart 요청으로 부분 수정한다. "
                    + "본문 / 선행 업무 / 첨부 파일 추가/삭제를 단일 트랜잭션으로 처리한다. "
                    + "request part: null 필드는 변경 없음, predecessorWorklogIds 는 null=변경없음/[]=모두제거/[...]=replace, "
                    + "removeFileIds 는 삭제할 기존 첨부 파일 ID. "
                    + "files part: 새로 추가할 파일들 (선택). "
                    + "teamId 는 수정 불가. dueDate 는 instructionDate 보다 빠를 수 없다. "
                    + "선행 업무 변경 시 자기참조 / 접근 권한 / 순환 의존을 검증한다. "
                    + "aiSummary 가 들어오면 aiSummaryEdited 가 true 로 표시된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정에 성공한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않거나 일자/선행 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "업무가 없거나 소프트 삭제됨, 또는 삭제 대상 첨부 파일을 찾을 수 없음", content = @Content)
    })
    EmptyResponse updateWorklog(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "수정 대상 업무 ID", example = "501") Long worklogId,
            UpdateWorklogApiDto.Request request,
            List<MultipartFile> files
    );

    @Operation(summary = "업무 상태 변경",
            description = "작성자 본인이 업무 상태만 변경한다. "
                    + "서버 상태 전이 정책을 통과한 경우에만 저장하며, 실제 변경 시 상태 이력을 기록한다. "
                    + "COMPLETED 로 전환되면 완료일을 서버 현재 날짜로 기록한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경에 성공한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않거나 허용되지 않은 상태 전이", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "업무가 없거나 소프트 삭제됨", content = @Content)
    })
    EmptyResponse updateWorklogStatus(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "상태 변경 대상 업무 ID", example = "501") Long worklogId,
            UpdateWorklogStatusApiDto.Request request
    );

    @Operation(summary = "선행 업무 후보 검색",
            description = "업무 등록/수정 화면에서 선행으로 지정할 worklog 후보를 검색한다. "
                    + "의존성은 같은 팀 한정이라 요청 teamId 의 ACTIVE 멤버여야 하며, "
                    + "후보는 해당 팀의 미삭제 + 미완료 worklog 만 노출된다. "
                    + "query 가 있으면 제목 LIKE 로 좁히고, excludeWorklogId 가 있으면 결과에서 제외한다. "
                    + "정렬은 created_at 내림차순으로 고정된다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선행 후보 페이지를 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "요청 팀의 ACTIVE 멤버가 아니다.", content = @Content)
    })
    PageResponse<SearchPredecessorApiDto.Response.Item> searchPredecessor(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject SearchPredecessorApiDto.Request request
    );
}
