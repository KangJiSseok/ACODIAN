package com.ibank.axwms.domain.organization.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateTeamApiDto {

    @Schema(description = "팀 수정 요청 DTO")
    public record Request(
            @NotBlank String teamName,
            String description,
            LocalDate startDate,
            LocalDate expectedEndDate,
            @NotNull Long leaderUserId,
            @NotBlank String teamRole,
            String allocation,
            @NotNull Boolean isPrimary
    ) {
    }

    @Schema(description = "팀 수정 응답 DTO")
    public record Response(Long teamId) {

        public static Response of(Long teamId) {
            return new Response(teamId);
        }
    }
}
