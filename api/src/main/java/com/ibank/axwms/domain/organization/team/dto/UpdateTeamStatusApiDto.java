package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateTeamStatusApiDto {

    @Schema(description = "팀 상태 변경 요청 DTO")
    public record Request(
            @NotNull TeamStatus statusCode
    ) {
    }

    @Schema(description = "팀 상태 변경 응답 DTO")
    public record Response(
            Long teamId,
            TeamStatus statusCode
    ) {

        public static Response of(Long teamId, TeamStatus statusCode) {
            return new Response(teamId, statusCode);
        }
    }
}
