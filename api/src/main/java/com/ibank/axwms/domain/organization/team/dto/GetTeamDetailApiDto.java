package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamDetailApiDto {

    @Schema(description = "팀 상세 응답 DTO")
    public record Response(
            Long teamId,
            String teamName,
            TeamStatus statusCode,
            String description,
            Long departmentId,
            String departmentName,
            Long teamLeaderId,
            String teamLeaderName,
            LocalDate startDate,
            LocalDate expectedEndDate,
            Long totalWorklogCount,
            Long completedWorklogCount
    ) {


        /** repository projection 을 API 응답 DTO 로 변환한다. */
        public static Response from(TeamDetailProjection projection) {
            return new Response(
                    projection.teamId(),
                    projection.teamName(),
                    projection.statusCode(),
                    projection.description(),
                    projection.departmentId(),
                    projection.departmentName(),
                    projection.teamLeaderId(),
                    projection.teamLeaderName(),
                    projection.startDate(),
                    projection.expectedEndDate(),
                    projection.totalWorklogCount(),
                    projection.completedWorklogCount()
            );
        }
    }
}
