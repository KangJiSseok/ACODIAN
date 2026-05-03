package com.ibank.axwms.domain.organization.user.dto;

import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.repository.jooq.projection.UserSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetUsersApiDto {

    @Schema(description = "사용자 목록 조회 요청 DTO")
    public record Request(
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
