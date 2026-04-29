package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateTeamApiDto {

    @Schema(description = "팀 생성 요청 DTO")
    public record Request(
            @NotNull Long departmentId,
            @NotBlank String teamName,
            String description,
            @NotNull Long leaderUserId,
            @NotNull @Valid LeaderMembership leaderMembership,
            @NotNull TeamStatus statusCode,
            LocalDate startDate,
            LocalDate expectedEndDate
    ) {
    }

    @Schema(description = "생성/수정 시 대표자 membership 입력")
    public record LeaderMembership(
            @NotBlank String teamRole,
            String allocation,
            @NotNull Boolean isPrimary
    ) {
    }
}
