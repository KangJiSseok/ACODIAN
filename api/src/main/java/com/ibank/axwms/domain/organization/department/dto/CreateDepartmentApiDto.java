package com.ibank.axwms.domain.organization.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateDepartmentApiDto {

    /** 새 부서 등록 요청 DTO. Controller 바인딩과 DepartmentService 입력을 겸한다. */
    @Schema(description = "부서 등록 요청 DTO")
    public record Request(
            @Schema(description = "부서명", example = "플랫폼전략본부")
            @NotBlank(message = "departmentName 은 비어 있을 수 없습니다.")
            @Size(max = 100, message = "departmentName 은 100자를 초과할 수 없습니다.")
            String departmentName,
            @Schema(description = "부서 설명", example = "전사 플랫폼 전략을 담당하는 본부", nullable = true)
            String description,
            @Schema(description = "부서장 사용자 ID", example = "1001", nullable = true)
            Long departmentHeadUserId
    ) {
    }
}
