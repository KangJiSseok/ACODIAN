package com.ibank.axwms.domain.tag.dto;

import com.ibank.axwms.domain.tag.entity.TagMergeCandidate;
import com.ibank.axwms.domain.tag.entity.TagMergeCandidateItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagMergeCandidateApiDto {

    public static final int MAX_MERGE_CANDIDATE_TAG_COUNT = 5;
    public static final int MIN_MERGE_CANDIDATE_TAG_COUNT = 2;
    public static final int DESCRIPTION_MAX_LENGTH = 150;
    public static final int DEFAULT_MAX_GROUP_COUNT = 20;
    public static final int DEFAULT_MIN_USAGE_COUNT = 0;
    public static final int DEFAULT_TAG_LIMIT = 200;

    @Schema(description = "태그 병합 후보 저장 요청")
    public record Request(
            @NotEmpty
            List<@NotNull @Valid CandidateItem> items
    ) {
    }

    @Schema(description = "AI 태그 병합 후보 생성 요청")
    public record GenerateRequest(
            @Min(1)
            @Max(MAX_MERGE_CANDIDATE_TAG_COUNT)
            Integer maxCandidateCount,

            @Min(1)
            @Max(100)
            Integer maxGroupCount,

            @Min(0)
            Integer minUsageCount,

            @Min(10)
            @Max(1000)
            Integer tagLimit
    ) {
        /**
         * 후보 생성 정확도와 비용의 기본 균형을 맞추기 위해 후보 태그는 그룹당 최대 5개로 제한한다.
         */
        public int normalizedMaxCandidateCount() {
            return maxCandidateCount == null ? MAX_MERGE_CANDIDATE_TAG_COUNT : maxCandidateCount;
        }

        /**
         * 한 번의 운영 검토 화면에 과도한 후보가 쌓이지 않도록 기본 그룹 수를 제한한다.
         */
        public int normalizedMaxGroupCount() {
            return maxGroupCount == null ? DEFAULT_MAX_GROUP_COUNT : maxGroupCount;
        }

        /**
         * usageCount 가 낮은 태그도 초기 정리 대상에 포함할 수 있도록 기본값은 0으로 둔다.
         */
        public int normalizedMinUsageCount() {
            return minUsageCount == null ? DEFAULT_MIN_USAGE_COUNT : minUsageCount;
        }

        /**
         * AI 프롬프트 비용을 통제하면서 운영 DB의 현재 태그 풀을 충분히 반영하는 기본 조회 크기다.
         */
        public int normalizedTagLimit() {
            return tagLimit == null ? DEFAULT_TAG_LIMIT : tagLimit;
        }
    }

    @Schema(description = "태그 병합 후보 그룹")
    public record CandidateItem(
            @NotNull
            @Valid
            TagItem mergeTargetTag,

            @Size(max = DESCRIPTION_MAX_LENGTH)
            String resultDescription,

            @NotEmpty
            @Size(min = MIN_MERGE_CANDIDATE_TAG_COUNT, max = MAX_MERGE_CANDIDATE_TAG_COUNT)
            List<@NotNull @Valid TagItem> mergeCandidateTags
    ) {
        /**
         * 결과 태그가 후보에 섞이면 병합 방향이 모호해져 저장 단계에서 차단한다.
         */
        @AssertTrue(message = "병합 결과 태그는 후보 태그 목록에 포함될 수 없습니다.")
        public boolean hasNoSelfCandidate() {
            if (mergeTargetTag == null || mergeCandidateTags == null) {
                return true;
            }
            return mergeCandidateTags.stream()
                    .noneMatch(candidate -> candidate != null && candidate.tagId().equals(mergeTargetTag.tagId()));
        }
    }

    @Schema(description = "태그 병합 후보 수정 요청")
    public record UpdateRequest(
            @NotNull
            @Positive
            Long mergeTargetTagId,

            @Size(max = DESCRIPTION_MAX_LENGTH)
            String resultDescription,

            @NotEmpty
            @Size(min = MIN_MERGE_CANDIDATE_TAG_COUNT, max = MAX_MERGE_CANDIDATE_TAG_COUNT)
            List<@NotNull @Positive Long> mergeCandidateTagIds
    ) {
        /**
         * 결과 태그가 source 후보에 포함되면 병합 방향이 모호해지므로 요청 단계에서 차단한다.
         */
        @AssertTrue(message = "병합 결과 태그는 후보 태그 목록에 포함될 수 없습니다.")
        public boolean hasNoSelfCandidate() {
            if (mergeTargetTagId == null || mergeCandidateTagIds == null) {
                return true;
            }
            return mergeCandidateTagIds.stream()
                    .noneMatch(tagId -> Objects.equals(tagId, mergeTargetTagId));
        }

        /**
         * 중복 ID 제거 후에도 최소 source 개수를 만족해야 실제 병합 후보로 의미가 있다.
         */
        @AssertTrue(message = "병합 후보 태그는 중복 제거 후 최소 2개 이상 필요합니다.")
        public boolean hasEnoughDistinctCandidates() {
            if (mergeCandidateTagIds == null) {
                return true;
            }
            return mergeCandidateTagIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .count() >= MIN_MERGE_CANDIDATE_TAG_COUNT;
        }
    }

    @Schema(description = "태그 병합 후보의 태그 항목")
    public record TagItem(
            @NotNull
            @Positive
            Long tagId,

            @NotBlank
            @Size(max = 100)
            String tagName,

            Integer usageCount
    ) {
    }

    @Schema(description = "태그 병합 후보 목록 응답")
    public record Response(
            List<Item> items
    ) {
        /**
         * 저장된 후보와 source 항목을 API 응답 shape 로 조립한다.
         */
        public static Response of(List<TagMergeCandidate> candidates,
                                  Map<Long, List<TagMergeCandidateItem>> itemsByCandidateId,
                                  Map<Long, Integer> usageCountByTagId) {
            return new Response(
                    candidates.stream()
                            .map(candidate -> Item.of(
                                    candidate,
                                    itemsByCandidateId.getOrDefault(candidate.getId(), List.of()),
                                    usageCountByTagId
                            ))
                            .toList()
            );
        }
    }

    @Schema(description = "태그 병합 후보 항목")
    public record Item(
            Long mergeCandidateId,
            String statusCode,
            String resultDescription,
            TagItem mergeTargetTag,
            List<TagItem> mergeCandidateTags
    ) {
        /**
         * 후보 저장 당시 이름 snapshot 과 현재 사용 횟수 캐시를 함께 노출한다.
         */
        public static Item of(TagMergeCandidate candidate,
                              List<TagMergeCandidateItem> items,
                              Map<Long, Integer> usageCountByTagId) {
            return new Item(
                    candidate.getId(),
                    candidate.getStatusCode().name(),
                    candidate.getResultDescription(),
                    new TagItem(
                            candidate.getTargetTagId(),
                            candidate.getTargetTagName(),
                            usageCountByTagId.getOrDefault(candidate.getTargetTagId(), 0)
                    ),
                    items.stream()
                            .map(item -> new TagItem(
                                    item.getSourceTagId(),
                                    item.getSourceTagName(),
                                    usageCountByTagId.getOrDefault(item.getSourceTagId(), 0)
                            ))
                            .toList()
            );
        }
    }
}
