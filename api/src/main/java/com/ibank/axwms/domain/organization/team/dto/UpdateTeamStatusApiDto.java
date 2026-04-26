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
}
