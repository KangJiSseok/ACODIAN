package com.ibank.axwms.domain.organization.user.controller;

import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetDepartmentCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUserApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateUserApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "사용자 API")
public interface UserControllerDocs {

    @Operation(
            summary = "현재 로그인 사용자 조회",
            description = "JWT access token 으로 인증된 현재 사용자의 프로필/이메일/전화번호/입사일/재직 상태/부서/소속 팀 문맥을 반환한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "현재 로그인 사용자 정보를 반환한다.",
                    content = @Content(schema = @Schema(implementation = GetMyProfileApiDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "현재 사용자 문맥에 해당하는 사용자를 찾을 수 없다.", content = @Content)
    })
    GetMyProfileApiDto.Response getMyProfile(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(
            summary = "현재 로그인 사용자 부분 수정",
            description = "JWT access token 으로 인증된 현재 사용자만 principal.userId 기준으로 부분 수정한다. "
                    + "multipart/form-data 요청의 JSON part(`request`) null 필드는 기존 값을 유지하고, "
                    + "선택 file part(`profile_image`)가 있으면 프로필 이미지를 final key 로 직접 업로드해 교체한다. "
                    + "department_id, title_name, employment_status 는 현재 정책상 self mutation 으로 허용하며, "
                    + "title_name 은 signup 과 같은 매핑으로 roleCode 를 동기화해 다음 토큰 발급부터 roleCode claim 에 반영될 수 있다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "현재 사용자 정보를 부분 수정하고 빈 객체를 반환한다.",
                    content = @Content(schema = @Schema(implementation = EmptyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않거나 지원하지 않는 직책명이다.", content = @Content),
            @ApiResponse(responseCode = "413", description = "multipart 업로드 크기 제한을 초과했다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "현재 사용자 또는 활성 부서를 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "503", description = "프로필 이미지 업로드를 처리할 수 없다. 잠시 후 다시 시도해야 한다.", content = @Content)
    })
    EmptyResponse updateMyProfile(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "현재 사용자 부분 수정 JSON part. multipart part name 은 `request` 이다.")
            @RequestPart("request")
            UpdateMyProfileApiDto.Request request,
            @Parameter(description = "선택 프로필 이미지 file part. multipart part name 은 `profile_image` 이다. 미첨부 시 기존 이미지를 유지한다.")
            @RequestPart(value = "profile_image", required = false)
            MultipartFile profileImage
    );

    @Operation(
            summary = "사용자 상세 조회",
            description = "인증된 사용자가 단일 사용자의 기본 정보, 소속 부서, 전체 ACTIVE 팀 membership 문맥을 조회한다. "
                    + "teams 는 대표 소속 팀, 팀 대표 membership 순서로 정렬하며 top-level teamId/teamName 은 반환하지 않는다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 상세 정보를 반환한다.",
                    content = @Content(schema = @Schema(implementation = GetUserApiDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "조회 대상 사용자를 찾을 수 없다.", content = @Content)
    })
    GetUserApiDto.Response getUser(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long id
    );

    @Operation(
            summary = "관리자 선택 후보 조회",
            description = "JWT access token 의 인증된 role 기준으로 관리자 선택 후보를 반환한다. "
                    + "DIRECTOR 는 DEPT_HEAD 사용자 전체를, DEPT_HEAD 는 자기 자신만 조회한다. "
                    + "요청 query/body 로 role 값을 받지 않는다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "관리자 선택 후보 목록을 배열로 반환한다.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GetAdminCandidatesApiDto.Response.class)))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 선택 후보 조회 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "현재 사용자 문맥에 해당하는 사용자를 찾을 수 없다.", content = @Content)
    })
    List<GetAdminCandidatesApiDto.Response> getAdminCandidates(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(
            summary = "부서 배정 후보 조회",
            description = "DIRECTOR 가 아직 부서에 소속되지 않은 DEPT_HEAD 사용자를 부서 배정 후보로 조회한다. "
                    + "페이지네이션 없이 userId, userName 만 배열로 반환한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "부서 배정 후보 목록을 배열로 반환한다.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GetDepartmentCandidatesApiDto.Response.class)))
            ),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "부서 배정 후보 조회 권한이 없다.", content = @Content)
    })
    List<GetDepartmentCandidatesApiDto.Response> getDepartmentCandidates(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );

    @Operation(
            summary = "사용자 목록 조회",
            description = "DIRECTOR 또는 DEPT_HEAD 가 사용자 목록을 페이지네이션으로 조회한다. "
                    + "userName, departmentId, positionName, employmentStatus optional filter 를 적용하며, "
                    + "employmentStatus 가 없으면 ACTIVE/LEAVE 만 포함하고 RETIRED 는 항상 제외한다. "
                    + "page 기본값은 1, pageSize 기본값은 20이고 최대 100이다. "
                    + "정렬은 DIRECTOR, DEPT_HEAD, TEAM_LEAD, MEMBER 순서다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 목록을 PageResponse 로 반환한다."
            ),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "사용자 목록 조회 권한이 없다.", content = @Content)
    })
    PageResponse<GetUsersApiDto.Response> getUsers(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject GetUsersApiDto.Request request
    );

    @Operation(
            summary = "사용자 부분 수정",
            description = "DIRECTOR 또는 DEPT_HEAD 가 사용자 기본 정보와 대표 소속 팀을 부분 수정한다. "
                    + "multipart/form-data 요청의 JSON part(`request`) null 필드는 기존 값을 유지하고, "
                    + "선택 file part(`profile_image`)가 있으면 프로필 이미지를 새 파일로 교체한다. "
                    + "primaryTeamId 는 대상 사용자의 기존 team membership 만 지정할 수 있다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보를 부분 수정하고 빈 객체를 반환한다.",
                    content = @Content(schema = @Schema(implementation = EmptyResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "413", description = "multipart 업로드 크기 제한을 초과했다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "access token 이 없거나 유효하지 않다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "사용자 수정 권한이 없다.", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자, 부서 또는 팀을 찾을 수 없다.", content = @Content),
            @ApiResponse(responseCode = "503", description = "프로필 이미지 업로드를 처리할 수 없다. 잠시 후 다시 시도해야 한다.", content = @Content)
    })
    EmptyResponse updateUser(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @Parameter(description = "사용자 ID", example = "101") Long id,
            @Parameter(description = "사용자 부분 수정 JSON part. multipart part name 은 `request` 이다.")
            @RequestPart("request")
            UpdateUserApiDto.Request request,
            @Parameter(description = "선택 프로필 이미지 file part. multipart part name 은 `profile_image` 이다. 미첨부 시 기존 이미지를 유지한다.")
            @RequestPart(value = "profile_image", required = false)
            MultipartFile profileImage
    );
}
