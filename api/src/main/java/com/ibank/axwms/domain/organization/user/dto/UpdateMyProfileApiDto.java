package com.ibank.axwms.domain.organization.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateMyProfileApiDto {

    /** 현재 사용자 프로필 부분 수정 요청 DTO. null 필드는 기존 값을 유지하고 password/primaryTeamId 는 의도적으로 받지 않는다. */
    @Schema(description = "현재 사용자 프로필 부분 수정 요청 DTO. null 필드는 변경하지 않는다.")
    public record Request(
            @Schema(description = "소속 부서 ID", example = "10")
            @JsonProperty("department_id")
            @Positive(message = "department_id 는 양수여야 합니다.")
            Long departmentId,
            @Schema(description = "사용자 이름", example = "홍길동")
            @JsonProperty("user_name")
            @Size(max = 50, message = "user_name 은 50자를 초과할 수 없습니다.")
            String userName,
            @Schema(description = "로그인 이메일", example = "hong@axwms.com")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            @Size(max = 100, message = "email 은 100자를 초과할 수 없습니다.")
            String email,
            @Schema(description = "직급명", example = "과장", nullable = true)
            @JsonProperty("position_name")
            @Size(max = 50, message = "position_name 은 50자를 초과할 수 없습니다.")
            String positionName,
            @Schema(description = "직책명. 제공되면 roleCode 도 같은 매핑으로 동기화된다.", example = "팀장")
            @JsonProperty("title_name")
            @Size(max = 50, message = "title_name 은 50자를 초과할 수 없습니다.")
            String titleName,
            @Schema(description = "입사일", example = "2026-04-27")
            @JsonProperty("join_date")
            LocalDate joinDate,
            @Schema(description = "전화번호", example = "010-1234-5678", nullable = true)
            @Size(max = 20, message = "phone 은 20자를 초과할 수 없습니다.")
            String phone,
            @Schema(description = "재직 상태", example = "ACTIVE")
            @JsonProperty("employment_status")
            EmploymentStatus employmentStatus
    ) {
    }
}
