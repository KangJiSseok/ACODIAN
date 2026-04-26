package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamsSummaryApiDto {

    @Schema(description = "팀 상단 집계 요약 조회 요청 DTO")
    public record Request(
            @Schema(description = "부서 ID", example = "10")
            Long departmentId
    ) {
    }

    @Schema(description = "팀 상단 집계 요약 응답 DTO")
    public record Response(
            Long activeTeamCount,
            Long totalTeamCount,
            Long activeUserCount,
            Long activeTeamUserCount,
            Long allTeamUserCount
    ) {

        public static Response from(TeamSummaryProjection projection) {
            return new Response(
                    projection.activeTeamCount(),
                    projection.totalTeamCount(),
                    projection.activeUserCount(),
                    projection.activeTeamUserCount(),
                    projection.allTeamUserCount()
            );
        }
    }
}
