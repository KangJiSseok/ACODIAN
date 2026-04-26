package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamDetailProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamDetailApiDto {

    @Schema(description = "팀 상세 응답 DTO")
    public record Response(
            Long teamId,
            String teamName,
            Long departmentId,
            String departmentName,
            TeamStatus statusCode,
            String description,
            LocalDate startDate,
            LocalDate expectedEndDate,
            LocalDateTime deletedAt,
            Leader leader
    ) {

        /** skeleton 단계에서 팀 상세 응답 시그니처를 고정하기 위한 placeholder 응답이다. */
        public static Response placeholder(Long teamId) {
            return new Response(teamId, null, null, null, null, null, null, null, null, null);
        }

        /** repository projection 을 API 응답 DTO 로 변환한다. */
        public static Response from(TeamDetailProjection projection) {
            return new Response(
                    projection.teamId(),
                    projection.teamName(),
                    projection.departmentId(),
                    projection.departmentName(),
                    projection.statusCode(),
                    projection.description(),
                    projection.startDate(),
                    projection.expectedEndDate(),
                    projection.deletedAt(),
                    new Leader(
                            projection.leaderUserId(),
                            projection.leaderUserName(),
                            projection.leaderPositionName(),
                            projection.leaderTitleName(),
                            projection.leaderTeamRole(),
                            projection.leaderAllocation(),
                            projection.leaderIsPrimary()
                    )
            );
        }

        @Schema(description = "대표 membership 요약")
        public record Leader(
                Long userId,
                String userName,
                String positionName,
                String titleName,
                String teamRole,
                String allocation,
                Boolean isPrimary
        ) {
        }
    }
}
