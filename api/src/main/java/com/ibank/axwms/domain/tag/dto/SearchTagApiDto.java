package com.ibank.axwms.domain.tag.dto;

import com.ibank.axwms.domain.tag.repository.jooq.projection.SearchTagProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchTagApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_QUERY_LENGTH = 100;

    @Schema(description = "메타 태그 검색 요청 DTO")
    public record Request(
            @Schema(description = "태그명 LIKE 검색어 (대소문자 무시). 공백/빈 문자열은 전체 조회와 동일하게 처리된다.", example = "결산")
            @Size(max = MAX_QUERY_LENGTH, message = "query 길이는 100자 이하여야 합니다.")
            String query,
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize
    ) {
        public int pageOrDefault() {
            return page == null ? DEFAULT_PAGE : page;
        }

        public int pageSizeOrDefault() {
            return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Response {

        public static PageResponse<Item> fromPage(Page<SearchTagProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "메타 태그 검색 결과 항목")
        public record Item(
                @Schema(description = "태그 ID", example = "1")
                Long id,
                @Schema(description = "태그 이름", example = "결산")
                String tagName,
                @Schema(description = "사용 횟수", example = "12")
                Integer usageCount
        ) {
            public static Item from(SearchTagProjection projection) {
                return new Item(
                        projection.id(),
                        projection.tagName(),
                        projection.usageCount()
                );
            }
        }
    }
}
