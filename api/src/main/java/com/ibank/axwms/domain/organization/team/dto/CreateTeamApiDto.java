package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateTeamApiDto {

    @Schema(description = "팀 생성 요청 DTO")
    public record Request(
            @Schema(description = "팀명", example = "물류혁신TF")
            @NotBlank(message = "teamName 은 필수입니다.")
            @Size(max = 100, message = "teamName 은 100자 이하여야 합니다.")
            String teamName,
            @Schema(description = "팀 설명", example = "창고 자동화 및 운영 고도화")
            String description,
            @Schema(description = "생성될 팀의 관리자 userId", example = "100")
            @NotNull(message = "addAdmin 은 필수입니다.")
            Long addAdmin,
            @Schema(description = "생성될 팀의 사용자 목록")
            @NotNull(message = "addUsers 는 필수입니다.")
            List<@NotNull(message = "addUsers 항목은 null 일 수 없습니다.") @Valid AddUser> addUsers,
            @Schema(description = "팀 상태 코드", example = "ACTIVE")
            @NotNull(message = "statusCode 는 필수입니다.")
            TeamStatus statusCode,
            @Schema(description = "팀 시작일", example = "2026-04-01")
            LocalDate startDate,
            @Schema(description = "팀 종료 예정일", example = "2026-12-31")
            LocalDate expectedEndDate
    ) {
    }

    @Schema(description = "생성될 팀 사용자")
    public record AddUser(
            @Schema(description = "사용자 ID", example = "102")
            @NotNull(message = "addUsers.userId 는 필수입니다.")
            Long userId,
            @Schema(description = "팀 리더 여부", example = "true")
            @NotNull(message = "addUsers.isLeader 는 필수입니다.")
            Boolean isLeader,
            @Schema(description = "팀 내 업무 역할", example = "WMS 운영")
            @NotBlank(message = "addUsers.teamRole 은 필수입니다.")
            @Size(max = 50, message = "addUsers.teamRole 은 50자 이하여야 합니다.")
            String teamRole
    ) {
    }
}
