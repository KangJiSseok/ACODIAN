package com.ibank.axwms.domain.organization.user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.team.UserTeamAuthority;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetMyProfileApiDto {

    @Schema(description = "현재 로그인 사용자 정보 응답")
    public record Response(
            Long userId,
            String userName,
            String email,
            String phone,
            Long departmentId,
            String departmentName,
            String positionName,
            String titleName,
            LocalDate joinDate,
            EmploymentStatus employmentStatus,
            String profileImageUrl,
            List<TeamSummary> teams
    ) {
        public static Response of(User user, Department department, List<TeamSummary> teams) {
            return new Response(
                    user.getId(),
                    user.getUserName(),
                    user.getEmail(),
                    user.getPhone(),
                    department.getId(),
                    department.getDepartmentName(),
                    user.getPositionName(),
                    user.getTitleName(),
                    user.getJoinDate(),
                    user.getEmploymentStatus(),
                    user.getProfileImageUrl(),
                    teams
            );
        }

        @Schema(description = "사용자 소속 팀 요약")
        @JsonPropertyOrder({"isPrimary", "teamId", "teamName", "teamAuthority", "teamRole", "allocation"})
        public record TeamSummary(
                boolean isPrimary,
                Long teamId,
                String teamName,
                UserTeamAuthority teamAuthority,
                String teamRole,
                String allocation
        ) {
        }
    }
}
