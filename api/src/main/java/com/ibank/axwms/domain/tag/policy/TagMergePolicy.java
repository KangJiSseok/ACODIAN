package com.ibank.axwms.domain.tag.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagMergePolicy {

    private static final int MAX_AI_TAG_COUNT = 5;

    /**
     * Bean Validation 의 필드 검증이 먼저 실패할 수 있도록 null 입력은 조합 정책에서 통과시킨다.
     */
    public static boolean hasAnyAiTag(Collection<?> existingTagIds, Collection<?> newTagNames) {
        if (existingTagIds == null || newTagNames == null) {
            return true;
        }

        return !existingTagIds.isEmpty() || !newTagNames.isEmpty();
    }

    /**
     * AI 태그는 기존 선택 태그와 신규 생성 태그를 합산해 최대 적용 개수를 제한한다.
     */
    public static boolean isWithinAiTagLimit(Collection<?> existingTagIds, Collection<?> newTagNames) {
        if (existingTagIds == null || newTagNames == null) {
            return true;
        }

        return existingTagIds.size() + newTagNames.size() <= MAX_AI_TAG_COUNT;
    }
}
