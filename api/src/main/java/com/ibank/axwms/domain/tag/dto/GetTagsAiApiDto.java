package com.ibank.axwms.domain.tag.dto;

import com.ibank.axwms.domain.tag.repository.jooq.projection.TagInfoProjection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTagsAiApiDto {

    public record TagInfo(
            Long id,
            String tagName,
            Integer usageCount
    ) {
        /**
         * Repository projection 을 FastAPI 내부 응답 계약으로 변환한다.
         */
        public static TagInfo from(TagInfoProjection tagInfo) {
            return new TagInfo(tagInfo.tagId(), tagInfo.tagName(), tagInfo.usageCount());
        }
    }

    public record Response(
            List<TagInfo> tags
    ) {
        /**
         * FastAPI 태그 선택 프롬프트에 필요한 태그 목록 형태로 응답을 조립한다.
         */
        public static Response from(List<TagInfoProjection> tags) {
            return new Response(
                    tags.stream()
                            .map(TagInfo::from)
                            .toList()
            );
        }
    }

}
