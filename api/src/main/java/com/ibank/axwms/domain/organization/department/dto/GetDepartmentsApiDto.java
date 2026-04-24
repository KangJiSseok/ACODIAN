package com.ibank.axwms.domain.organization.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetDepartmentsApiDto {

    /** 활성 부서 목록 화면에 필요한 상단 집계와 부서 목록을 함께 담는 응답 DTO. */
    @Schema(description = "활성 부서 목록 및 집계 응답")
    public record Response(
            @Schema(description = "활성 부서 수", example = "3")
            long activeDepartmentCount,
            @Schema(description = "활성 부서 소속 활성 팀 수", example = "5")
            long activeTeamCount,
            @Schema(description = "활성 팀에 최소 1개 이상 소속된 활성 사용자 수", example = "18")
            long activeUserCount,
            @Schema(description = "활성 부서 목록")
            List<DepartmentSummary> departments
    ) {

        /** 상단 집계와 부서 목록을 방어적 복사와 함께 응답 DTO 로 조립한다. */
        public static Response of(long activeDepartmentCount,
                                  long activeTeamCount,
                                  long activeUserCount,
                                  List<DepartmentSummary> departments) {
            return new Response(
                    activeDepartmentCount,
                    activeTeamCount,
                    activeUserCount,
                    departments == null ? List.of() : List.copyOf(departments)
            );
        }

        /** 활성 부서 목록 한 행에 필요한 부서 표시 정보다. */
        @Schema(description = "활성 부서 요약")
        public record DepartmentSummary(
                @Schema(description = "부서 ID", example = "10")
                Long departmentId,
                @Schema(description = "부서명", example = "물류본부")
                String departmentName,
                @Schema(description = "부서 설명", example = "전사 물류 운영 총괄")
                String description,
                @Schema(description = "부서장 사용자 ID", example = "1001", nullable = true)
                Long departmentHeadUserId,
                @Schema(description = "부서장 사용자명", example = "박본부", nullable = true)
                String departmentHeadUserName,
                @Schema(description = "생성 시각")
                LocalDateTime createdAt,
                @Schema(description = "수정 시각")
                LocalDateTime updatedAt
        ) {
        }
    }
}
