package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BulkUpsertTeamUsersApiDto {

    @Schema(description = "팀 사용자 일괄 반영 요청 DTO")
    public record Request(
            @Valid
            @NotEmpty List<Item> items
    ) {

        public record Item(
                @NotNull Long userId,
                @NotNull Boolean teamLeader,
                @NotBlank String teamRole,
                String allocation,
                @NotNull Boolean isPrimary,
                @NotNull UserTeamStatus statusCode
        ) {
        }
    }

    @Schema(description = "팀 사용자 일괄 반영 응답 DTO")
    public record Response(
            Long teamId,
            int processedCount
    ) {

        public static Response of(Long teamId, int processedCount) {
            return new Response(teamId, processedCount);
        }
    }
}
