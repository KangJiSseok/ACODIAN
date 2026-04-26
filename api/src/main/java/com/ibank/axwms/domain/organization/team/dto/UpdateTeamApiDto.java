package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
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
            @NotNull Long departmentId,
            @NotBlank String teamName,
            String description,
            @NotNull Long leaderUserId,
            @NotNull TeamStatus statusCode,
            LocalDate startDate,
            LocalDate expectedEndDate
    ) {
    }
}
