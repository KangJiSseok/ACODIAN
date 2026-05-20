package com.ibank.axwms.domain.tag.repository.jooq.query;

import com.ibank.axwms.domain.tag.dto.SearchTagApiDto;

public record SearchTagQuery(
        String query,
        int page,
        int pageSize
){
    public static SearchTagQuery from(SearchTagApiDto.Request request) {
        return new SearchTagQuery(
                normalizeQuery(request.query()),
                request.pageOrDefault(),
                request.pageSizeOrDefault()
        );
    }

    public int pageIndex() {
        return page - 1;
    }

    private static String normalizeQuery(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
