package com.ibank.axwms.domain.worklog.repository.jooq.query;

import com.ibank.axwms.domain.worklog.dto.SearchPredecessorApiDto;

/**
 * 선행 업무 후보 검색용 정규화된 query 파라미터.
 * page 는 1-based 로 들어오며 pageIndex() 로 0-based 변환된다.
 */
public record PredecessorCandidateSearchQuery(
        Long teamId,
        String query,
        Long excludeWorklogId,
        int page,
        int pageSize
) {
    public static PredecessorCandidateSearchQuery from(SearchPredecessorApiDto.Request request) {
        return new PredecessorCandidateSearchQuery(
                request.teamId(),
                normalizeQuery(request.query()),
                request.excludeWorklogId(),
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
