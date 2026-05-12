package com.ibank.axwms.domain.organization.user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetUserApiDto {

    @Schema(description = "사용자 상세 조회 응답")
    public record Response(
            @Schema(description = "사용자 ID", example = "101")
            Long userId,
            @Schema(description = "사용자명", example = "홍길동")
            String userName,
            @Schema(description = "이메일", example = "hong@axwms.com")
            String email,
            @Schema(description = "부서 ID", example = "10")
            Long departmentId,
            @Schema(description = "부서명", example = "물류본부")
            String departmentName,
            @Schema(description = "직급명", example = "과장")
            String positionName,
            @Schema(description = "직책명", example = "팀장")
            String titleName,
            @Schema(description = "입사일", example = "2024-03-01")
            LocalDate joinDate,
            @Schema(description = "프로필 이미지 URL", example = "https://cdn.axwms.com/profile/101.png")
            String profileImageUrl,
            @Schema(description = "전화번호", example = "010-1234-5678")
            String phone,
            @Schema(description = "재직 상태", example = "ACTIVE")
            EmploymentStatus employmentStatus,
            @Schema(description = "사용자 전체 소속 팀 문맥")
            List<TeamSummary> teams
    ) {

        /** 사용자/부서/팀 membership 문맥을 사용자 상세 응답으로 조립한다. */
        public static Response of(User user, Department department, List<TeamSummary> teams) {
            return new Response(
                    user.getId(),
                    user.getUserName(),
                    user.getEmail(),
                    department != null ? department.getId() : null,
                    department != null ? department.getDepartmentName() : null,
                    user.getPositionName(),
                    user.getTitleName(),
                    user.getJoinDate(),
                    user.getProfileImageUrl(),
                    user.getPhone(),
                    user.getEmploymentStatus(),
                    teams
            );
        }

        @Schema(description = "사용자 소속 팀 상세")
        @JsonPropertyOrder({"isPrimary", "teamId", "teamName", "isLeader", "teamRole"})
        public record TeamSummary(
                @Schema(description = "대표 소속 팀 여부", example = "true")
                boolean isPrimary,
                @Schema(description = "팀 ID", example = "21")
                Long teamId,
                @Schema(description = "팀명", example = "웹서비스 개발")
                String teamName,
                @Schema(description = "팀 대표 membership 여부", example = "true")
                boolean isLeader,
                @Schema(description = "팀 내 업무 역할명", example = "플랫폼 총괄")
                String teamRole
        ) {
        }
    }
}
