package com.ibank.axwms.domain.worklog.repository.jooq.query;

import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import java.time.LocalDate;

/**
 * 업무일지 검색 repository 호출에 필요한 입력을 정규화한 query 이다.
 * page/pageSize 기본값 보정과 keyword trim, period -> createdFrom 변환은
 * service 가 책임지고 repository 는 받은 값을 그대로 SQL 조건으로 매핑한다.
 */
public record WorklogSearchQuery(
        String keyword,
        Long teamId,
        WorklogStatus statusCode,
        WorklogImportance importanceCode,
        Long authorId,
        Long tagId,
        LocalDate createdFrom,
        int page,
        int pageSize
) {

    /**
     * API 요청 DTO 의 기본값과 기간 변환을 보정해 repository 전용 query 로 변환한다.
     *
     * @param request 검색 요청 DTO. null 이면 모든 필터 미지정으로 처리한다.
     * @param today 기간 환산 기준 일자. service 가 LocalDate.now() 로 주입한다.
     */
    public static WorklogSearchQuery from(SearchWorklogsApiDto.Request request, LocalDate today) {
        SearchWorklogsApiDto.Request normalized = request == null
                ? new SearchWorklogsApiDto.Request(null, null, null, null, null, null, null, null, null)
                : request;
        LocalDate createdFrom = normalized.period() == null ? null : normalized.period().fromDate(today);
        return new WorklogSearchQuery(
                trimToNull(normalized.keyword()),
                normalized.teamId(),
                normalized.statusCode(),
                normalized.importanceCode(),
                normalized.authorId(),
                normalized.tagId(),
                createdFrom,
                normalized.pageOrDefault(),
                normalized.pageSizeOrDefault()
        );
    }

    public int pageIndex() {
        return page - 1;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
