package com.ibank.axwms.domain.organization.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DeleteTeamApiDto {

    @Schema(description = "팀 삭제 응답 DTO")
    public record Response(
            Long teamId,
            LocalDateTime deletedAt
    ) {

        public static Response of(Long teamId, LocalDateTime deletedAt) {
            return new Response(teamId, deletedAt);
        }
    }
}
