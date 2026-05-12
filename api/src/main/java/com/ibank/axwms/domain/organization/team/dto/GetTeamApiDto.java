package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamApiDto {

    @Schema(description = "팀 상세 응답")
    public record Response(
            @Schema(description = "팀 ID", example = "21")
            Long teamId,
            @Schema(description = "팀명", example = "물류혁신TF")
            String teamName,
            @Schema(description = "팀 상태 코드", example = "ACTIVE")
            String statusCode,
            @Schema(description = "팀 설명", example = "창고 자동화 개선 전담")
            String description,
            @Schema(description = "팀 대표 사용자 ID", example = "101")
            Long teamLeaderId,
            @Schema(description = "팀 대표 사용자명", example = "홍길동")
            String teamLeaderName,
            @Schema(description = "팀 시작일", example = "2026-04-01")
            LocalDate startDate,
            @Schema(description = "팀 종료 예정일", example = "2026-12-31")
            LocalDate expectedEndDate,
            @Schema(description = "DEPT_HEAD이면서 팀 관리자인 사용자 ID", example = "201")
            Long deptHeadAdminUserId,
            @Schema(description = "DEPT_HEAD이면서 팀 관리자인 사용자명", example = "김사업부장")
            String deptHeadAdminUsername
    ) {

        /** repository projection 한 행을 팀 상세 응답으로 변환한다. */
        public static Response from(TeamDetailProjection projection) {
            return new Response(
                    projection.teamId(),
                    projection.teamName(),
                    projection.statusCode(),
                    projection.description(),
                    projection.teamLeaderId(),
                    projection.teamLeaderName(),
                    projection.startDate(),
                    projection.expectedEndDate(),
                    projection.deptHeadAdminUserId(),
                    projection.deptHeadAdminUsername()
            );
        }
    }
}
