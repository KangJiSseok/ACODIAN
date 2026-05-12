package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamUsersApiDto {

    @Schema(description = "팀 사용자 목록 응답")
    public record Response(
            @Schema(description = "팀 사용자 목록")
            List<Item> items
    ) {

        /** repository projection 목록을 팀 사용자 목록 응답으로 변환한다. */
        public static Response from(List<TeamUserSummaryProjection> projections) {
            return new Response(projections.stream()
                    .map(Item::from)
                    .toList());
        }
    }

    @Schema(description = "팀 사용자 목록 항목")
    public record Item(
            @Schema(description = "팀 리더 여부", example = "true")
            Boolean isLeader,
            @Schema(description = "사용자 ID", example = "101")
            Long userId,
            @Schema(description = "사용자명", example = "홍길동")
            String userName,
            @Schema(description = "직급명", example = "과장")
            String positionName,
            @Schema(description = "팀 내 업무 역할", example = "플랫폼 총괄")
            String teamRole
    ) {

        /** repository projection 한 행을 팀 사용자 목록 항목으로 변환한다. */
        public static Item from(TeamUserSummaryProjection projection) {
            return new Item(
                    projection.isLeader(),
                    projection.userId(),
                    projection.userName(),
                    projection.positionName(),
                    projection.teamRole()
            );
        }
    }
}
