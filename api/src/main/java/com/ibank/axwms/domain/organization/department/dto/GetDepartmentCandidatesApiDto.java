package com.ibank.axwms.domain.organization.department.dto;

import com.ibank.axwms.domain.organization.department.entity.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetDepartmentCandidatesApiDto {

    /** 부서 선택 UI 에 필요한 최소 부서 후보 목록 응답 DTO. */
    @Schema(description = "부서 선택 후보 목록 응답")
    public record Response(
            @Schema(description = "권한 범위에 맞는 활성 부서 후보 목록")
            List<DepartmentCandidate> departments
    ) {

        /** 후보가 없을 때도 빈 배열 계약을 유지하도록 방어적 복사로 응답을 조립한다. */
        public static Response of(List<DepartmentCandidate> departments) {
            return new Response(departments == null ? List.of() : List.copyOf(departments));
        }

        /** 부서 선택 후보 한 행에서 노출하는 최소 표시 정보다. */
        @Schema(description = "부서 선택 후보")
        public record DepartmentCandidate(
                @Schema(description = "부서 ID", example = "10")
                Long departmentId,
                @Schema(description = "부서명", example = "물류본부")
                String departmentName
        ) {

            /** 부서 엔티티에서 선택 UI 에 노출할 최소 식별 정보만 변환한다. */
            public static DepartmentCandidate from(Department department) {
                return new DepartmentCandidate(department.getId(), department.getDepartmentName());
            }
        }
    }
}
