package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.tag.policy.TagMergePolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApplyWorklogAiTagsApiDto {

    @Schema(description = "AI 태그 결과를 특정 업무일지에 적용하는 내부 API")
    public record Request(

            @NotNull
            @Schema(description = "AI가 기존 태그 목록에서 선택한 태그 ID 목록")
            List<@Positive Long> existingTagIds,

            @Schema(description = "AI가 새로 제안한 태그명 목록. newTags 적용 전 FastAPI 하위 호환용 필드")
            List<@NotBlank @Size(max = 100) String> newTagNames,

            @Schema(description = "AI가 새로 제안한 태그와 설명 목록")
            List<@Valid NewTag> newTags
    ) {

        public Request(List<Long> existingTagIds, List<NewTag> newTags) {
            this(existingTagIds, null, newTags);
        }

        /**
         * 신규 태그 설명을 포함하는 newTags 를 우선 사용하고, 구버전 newTagNames 는 태그명 기반 설명으로 변환한다.
         */
        public List<NewTag> effectiveNewTags() {
            if (newTags != null && !newTags.isEmpty()) {
                return newTags;
            }
            if (newTagNames == null || newTagNames.isEmpty()) {
                return List.of();
            }

            return newTagNames.stream()
                    .map(tagName -> new NewTag(tagName, tagName))
                    .toList();
        }

        /**
         * AI 태그 결과는 기존 태그 또는 신규 태그 중 최소 하나 이상이어야 한다.
         */
        @AssertTrue(message = "기존 태그 또는 신규 태그가 최소 1개 이상 필요합니다.")
        public boolean hasAnyTag() {
            return TagMergePolicy.hasAnyAiTag(existingTagIds, effectiveNewTags());
        }

        /**
         * AI 태그 생성 정책의 최대 5개 제한을 기존/신규 태그 합산 기준으로 검증한다.
         */
        @AssertTrue(message = "태그는 최대 5개까지 적용할 수 있습니다.")
        public boolean isWithinTagLimit() {
            return TagMergePolicy.isWithinAiTagLimit(existingTagIds, effectiveNewTags());
        }
    }

    @Schema(description = "AI가 새로 생성한 태그")
    public record NewTag(
            @NotBlank
            @Size(max = 100)
            @Schema(description = "태그명", example = "비용 검증")
            String tagName,

            @NotBlank
            @Size(max = 150)
            @Schema(description = "태그 설명", example = "지출 증빙, 금액 정합성, 결재 전 검토 업무에 사용하는 태그")
            String description
    ) {
    }
}
