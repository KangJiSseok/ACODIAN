package com.ibank.axwms.domain.organization.user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetMyProfileApiDto {

    /** 현재 로그인 사용자의 프로필/조직/팀 문맥 응답 DTO. */
    @Schema(description = "현재 로그인 사용자 정보 응답")
    public record Response(
            @Schema(description = "사용자 ID", example = "101")
            Long userId,
            @Schema(description = "사용자 이름", example = "홍길동")
            String userName,
            @Schema(description = "부서 ID", example = "10")
            Long departmentId,
            @Schema(description = "부서명", example = "물류본부")
            String departmentName,
            @Schema(description = "직급명", example = "과장")
            String positionName,
            @Schema(description = "직책명", example = "팀장")
            String titleName,
            @Schema(description = "프로필 이미지 URL", example = "https://cdn.axwms.com/profile/101.png")
            String profileImageUrl,
            @Schema(description = "소속 팀 목록")
            List<TeamSummary> teams
    ) {

        /** 사용자/부서/팀 문맥을 현재 사용자 조회 응답으로 조립한다. */
        public static Response of(User user, Department department, List<TeamSummary> teams) {
            return new Response(
                    user.getId(),
                    user.getUserName(),
                    department.getId(),
                    department.getDepartmentName(),
                    user.getPositionName(),
                    user.getTitleName(),
                    user.getProfileImageUrl(),
                    teams
            );
        }

        /** 현재 사용자의 팀 요약 정보. */
        @Schema(description = "사용자 소속 팀 요약")
        @JsonPropertyOrder({"isPrimary", "teamId", "teamName", "teamLeader", "teamRole", "allocation"})
        public record TeamSummary(
                @Schema(description = "주 소속 팀 여부", example = "true")
                boolean isPrimary,
                @Schema(description = "팀 ID", example = "21")
                Long teamId,
                @Schema(description = "팀명", example = "물류혁신TF")
                String teamName,
                @Schema(description = "팀장 여부", example = "true")
                boolean teamLeader,
                @Schema(description = "팀 내 업무 역할명", example = "플랫폼 총괄")
                String teamRole,
                @Schema(description = "참여/배치 성격", example = "주담당")
                String allocation
        ) {
        }
    }
}
