package com.ibank.axwms.domain.organization.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BulkUpsertTeamUsersApiDto {

    @Schema(description = "팀 사용자 일괄 반영 요청 DTO")
    public record Request(
            @Valid List<AddUser> addUsers,
            List<Long> removeUserIds
    ) {
        public List<AddUser> addUsersOrEmpty() {
            return addUsers == null ? List.of() : addUsers;
        }

        public List<Long> removeUserIdsOrEmpty() {
            return removeUserIds == null ? List.of() : removeUserIds;
        }

        public record AddUser(
                @NotNull Long userId,
                @NotNull Boolean teamLeader,
                @NotBlank String teamRole,
                String allocation,
                @NotNull Boolean isPrimary,
                @NotNull LocalDate joinedAt
        ) {
        }
    }
}
