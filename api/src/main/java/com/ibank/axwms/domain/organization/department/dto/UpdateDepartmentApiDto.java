package com.ibank.axwms.domain.organization.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateDepartmentApiDto {

    /** 활성 부서 수정 요청 DTO. head 는 DIRECTOR 또는 DEPT_HEAD 만 허용하며, null 또는 필드 생략은 해제로 해석한다. */
    @Schema(description = "부서 수정 요청 DTO")
    public record Request(
            @Schema(description = "부서명", example = "플랫폼전략본부")
            @NotBlank(message = "departmentName 은 비어 있을 수 없습니다.")
            @Size(max = 100, message = "departmentName 은 100자를 초과할 수 없습니다.")
            String departmentName,
            @Schema(description = "부서 설명", example = "전사 플랫폼 전략을 담당하는 본부", nullable = true)
            String description,
            @Schema(description = "부서장 사용자 ID. DIRECTOR 또는 DEPT_HEAD 역할만 허용하며, null 이거나 필드가 생략되면 부서장을 해제한다.", example = "1001", nullable = true)
            Long departmentHeadUserId
    ) {
    }
}
