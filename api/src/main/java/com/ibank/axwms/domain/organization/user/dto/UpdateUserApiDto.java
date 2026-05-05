package com.ibank.axwms.domain.organization.user.dto;

import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateUserApiDto {

    @Schema(description = "사용자 부분 수정 요청 DTO. null 필드는 변경하지 않는다.")
    public record Request(
            @Schema(description = "사용자명", example = "홍길동")
            @Size(max = 50, message = "userName 은 50자 이하여야 합니다.")
            String userName,
            @Schema(description = "이메일", example = "hong@axwms.com")
            @Email(message = "email 형식이 올바르지 않습니다.")
            @Size(max = 100, message = "email 은 100자 이하여야 합니다.")
            String email,
            @Schema(description = "프로필 이미지 URL", example = "https://cdn.axwms.com/profile/101.png")
            @Size(max = 500, message = "profileImageUrl 은 500자 이하여야 합니다.")
            String profileImageUrl,
            @Schema(description = "직급명", example = "차장")
            @Size(max = 50, message = "positionName 은 50자 이하여야 합니다.")
            String positionName,
            @Schema(description = "직책명", example = "팀장")
            @Size(max = 50, message = "titleName 은 50자 이하여야 합니다.")
            String titleName,
            @Schema(description = "부서 ID", example = "10")
            @Positive(message = "departmentId 는 양수여야 합니다.")
            Long departmentId,
            @Schema(description = "전화번호", example = "010-1234-5678")
            @Size(max = 20, message = "phone 은 20자 이하여야 합니다.")
            String phone,
            @Schema(description = "재직 상태", example = "ACTIVE")
            EmploymentStatus employmentStatus,
            @Schema(description = "입사일", example = "2024-03-01")
            LocalDate joinDate,
            @Schema(description = "대표 소속 팀 ID", example = "21")
            @Positive(message = "primaryTeamId 는 양수여야 합니다.")
            Long primaryTeamId
    ) {
    }
}
