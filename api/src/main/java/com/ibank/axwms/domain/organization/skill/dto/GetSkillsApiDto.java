package com.ibank.axwms.domain.organization.skill.dto;

import com.ibank.axwms.domain.organization.skill.repository.jooq.projection.UserSkillListItemProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetSkillsApiDto {

    @Schema(description = "사용자 스킬 목록 응답")
    public record Response(
            @Schema(description = "사용자 ID", example = "101")
            Long userId,
            @Schema(description = "보유 스킬 목록")
            List<Item> skills
    ) {

        /** 사용자 ID와 repository projection 목록을 사용자 스킬 목록 응답으로 변환한다. */
        public static Response of(Long userId, List<UserSkillListItemProjection> projections) {
            return new Response(
                    userId,
                    projections.stream()
                            .map(Item::from)
                            .toList()
            );
        }
    }

    @Schema(description = "사용자 스킬 목록 항목")
    public record Item(
            @Schema(description = "스킬 레코드 ID", example = "1")
            Long skillId,
            @Schema(description = "스킬명", example = "WMS")
            String skillName,
            @Schema(description = "스킬 레벨", example = "5")
            @Max(value = 5, message = "skillLevel 은 5이하여야 합니다.")
            Short skillLevel,
            @Schema(description = "수정 일시", example = "2026-04-20T09:00:00")
            LocalDateTime updatedAt
    ) {

        /** repository projection 한 행을 사용자 스킬 목록 항목으로 변환한다. */
        public static Item from(UserSkillListItemProjection projection) {
            return new Item(
                    projection.skillId(),
                    projection.skillName(),
                    projection.skillLevel(),
                    projection.updatedAt()
            );
        }
    }
}
