package com.ibank.axwms.domain.organization.user.dto;

import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.repository.jooq.projection.UserSummaryProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetUsersApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "사용자 목록 조회 요청 DTO")
    public record Request(
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize,
            @Schema(description = "사용자명", example = "한과장")
            @Size(max = 50, message = "userName 은 50자 이하여야 합니다.")
            String userName,
            @Schema(description = "부서 ID", example = "10")
            Long departmentId,
            @Schema(description = "직급명", example = "과장")
            @Size(max = 50, message = "positionName 은 50자 이하여야 합니다.")
            String positionName,
            @Schema(description = "재직 상태", example = "ACTIVE")
            EmploymentStatus employmentStatus
    ) {
        /** page query 가 없을 때 ADR 페이지네이션 기본값인 1페이지를 적용한다. */
        public int pageOrDefault() {
            return page == null ? DEFAULT_PAGE : page;
        }

        /** pageSize query 가 없을 때 목록 API 공통 기본 크기 20을 적용한다. */
        public int pageSizeOrDefault() {
            return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        }
    }

    @Schema(description = "사용자 목록 항목")
    public record Response(
            @Schema(description = "사용자 ID", example = "101")
            Long userId,
            @Schema(description = "사용자명", example = "홍길동")
            String userName,
            @Schema(description = "이메일", example = "hong@axwms.com")
            String email,
            @Schema(description = "전화번호", example = "010-1234-1234")
            String phone,
            @Schema(description = "부서 ID", example = "10")
            Long departmentId,
            @Schema(description = "부서명", example = "물류본부")
            String departmentName,
            @Schema(description = "프로필 이미지 URL", example = "https://cdn.axwms.com/profile/101.png")
            String profileImageUrl,
            @Schema(description = "대표 소속 팀 ID", example = "21")
            Long teamId,
            @Schema(description = "대표 소속 팀명", example = "물류혁신TF")
            String teamName,
            @Schema(description = "직급명", example = "과장")
            String positionName,
            @Schema(description = "직책명", example = "팀장")
            String titleName,
            @Schema(description = "재직 상태", example = "ACTIVE")
            EmploymentStatus employmentStatus
    ) {

        /** repository projection 페이지를 사용자 목록 API 응답 페이지로 변환한다. */
        public static PageResponse<Response> fromPage(Page<UserSummaryProjection> page) {
            return PageResponse.from(page.map(Response::from));
        }

        /** repository projection 한 행을 사용자 목록 응답 항목으로 변환한다. */
        public static Response from(UserSummaryProjection projection) {
            return new Response(
                    projection.userId(),
                    projection.userName(),
                    projection.email(),
                    projection.phone(),
                    projection.departmentId(),
                    projection.departmentName(),
                    projection.profileImageUrl(),
                    projection.teamId(),
                    projection.teamName(),
                    projection.positionName(),
                    projection.titleName(),
                    projection.employmentStatus()
            );
        }
    }
}
