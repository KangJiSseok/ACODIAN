package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamStatusSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamSummaryApiDto {

    @Schema(description = "팀 상태 요약 응답")
    public record Response(
            @Schema(description = "visible scope 내 ACTIVE 팀 수", example = "3")
            Long activeTeamCount,
            @Schema(description = "visible scope 내 INACTIVE 팀 수", example = "2")
            Long inactiveTeamCount,
            @Schema(description = "visible scope 내 전체 팀 수", example = "5")
            Long totalTeamCount
    ) {

        /** repository projection 을 팀 상태 요약 응답으로 변환한다. */
        public static Response from(TeamStatusSummaryProjection projection) {
            return new Response(
                    projection.activeTeamCount(),
                    projection.inactiveTeamCount(),
                    projection.totalTeamCount()
            );
        }
    }
}
