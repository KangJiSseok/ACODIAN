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
public final class UpdateTeamApiDto {

    @Schema(description = "팀 부분 수정 요청 DTO. null 필드는 변경하지 않는다.")
    public record Request(
            @Schema(description = "팀명", example = "물류혁신TF")
            @Size(max = 100, message = "teamName 은 100자 이하여야 합니다.")
            String teamName,
            @Schema(description = "팀 설명", example = "창고 자동화 및 운영 고도화")
            String description,
            @Schema(description = "추가할 팀 관리자 userId", example = "110")
            Long addAdmin,
            @Schema(description = "회수할 팀 관리자 userId", example = "100")
            Long removeAdmin,
            @Schema(description = "추가 또는 재활성화할 팀 사용자 목록")
            List<@NotNull(message = "addUsers 항목은 null 일 수 없습니다.") @Valid AddUser> addUsers,
            @Schema(description = "LEFT 상태로 전환할 사용자 ID 목록", example = "[101, 100]")
            List<@NotNull(message = "removeUsers 항목은 null 일 수 없습니다.") Long> removeUsers,
            @Schema(description = "ACTIVE membership 의 팀 내 업무 역할 수정 목록")
            List<@NotNull(message = "editUsers 항목은 null 일 수 없습니다.") @Valid EditUser> editUsers,
            @Schema(description = "팀 상태 코드", example = "ACTIVE")
            TeamStatus statusCode,
            @Schema(description = "팀 시작일", example = "2026-04-01")
            LocalDate startDate,
            @Schema(description = "팀 종료 예정일", example = "2026-12-31")
            LocalDate expectedEndDate
    ) {
    }

    @Schema(description = "추가 또는 재활성화할 팀 사용자")
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

    @Schema(description = "수정할 팀 사용자")
    public record EditUser(
            @Schema(description = "사용자 ID", example = "105")
            @NotNull(message = "editUsers.userId 는 필수입니다.")
            Long userId,
            @Schema(description = "팀 내 업무 역할", example = "WMS 운영")
            @NotBlank(message = "editUsers.teamRole 은 필수입니다.")
            @Size(max = 50, message = "editUsers.teamRole 은 50자 이하여야 합니다.")
            String teamRole
    ) {
    }
}
