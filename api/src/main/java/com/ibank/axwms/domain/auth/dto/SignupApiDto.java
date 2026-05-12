package com.ibank.axwms.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SignupApiDto {

    /** 회원가입 multipart JSON part 용 요청 DTO. snake_case 필드명을 유지하면서 Service 입력을 겸한다. */
    @Schema(description = "회원가입 요청 DTO")
    public record Request(
            @Schema(description = "소속 부서 ID", example = "10")
            @JsonProperty("department_id")
            @NotNull(message = "department_id 는 비어 있을 수 없습니다.")
            Long departmentId,
            @Schema(description = "사용자 이름", example = "홍길동")
            @JsonProperty("user_name")
            @NotBlank(message = "user_name 은 비어 있을 수 없습니다.")
            @Size(max = 50, message = "user_name 은 50자를 초과할 수 없습니다.")
            String userName,
            @Schema(description = "로그인 이메일", example = "hong@axwms.com")
            @NotBlank(message = "email 은 비어 있을 수 없습니다.")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            @Size(max = 100, message = "email 은 100자를 초과할 수 없습니다.")
            String email,
            @Schema(description = "평문 비밀번호", example = "Password123!")
            @NotBlank(message = "password 는 비어 있을 수 없습니다.")
            String password,
            @Schema(description = "직급명", example = "과장", nullable = true)
            @JsonProperty("position_name")
            @Size(max = 50, message = "position_name 은 50자를 초과할 수 없습니다.")
            String positionName,
            @Schema(description = "직책명", example = "팀장")
            @JsonProperty("title_name")
            @NotBlank(message = "title_name 은 비어 있을 수 없습니다.")
            @Size(max = 50, message = "title_name 은 50자를 초과할 수 없습니다.")
            String titleName,
            @Schema(description = "입사일", example = "2026-04-27")
            @JsonProperty("join_date")
            @NotNull(message = "join_date 는 비어 있을 수 없습니다.")
            LocalDate joinDate,
            @Schema(description = "전화번호", example = "010-1234-5678", nullable = true)
            @Size(max = 20, message = "phone 은 20자를 초과할 수 없습니다.")
            String phone,
            @Schema(description = "재직 상태", example = "ACTIVE")
            @JsonProperty("employment_status")
            @NotNull(message = "employment_status 는 비어 있을 수 없습니다.")
            EmploymentStatus employmentStatus
    ) {
    }
}
