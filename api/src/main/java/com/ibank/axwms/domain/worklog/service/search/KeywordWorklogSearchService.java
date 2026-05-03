package com.ibank.axwms.domain.worklog.service.search;

import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityPolicy;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 1단계 키워드(LIKE) + 필터 기반 업무일지 검색 구현체.
 * 가시 범위 결정은 {@link WorklogVisibilityPolicy} 가, SQL 조립은 repository 가 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordWorklogSearchService implements WorklogSearchService {

    private final WorklogRepository worklogRepository;
    private final WorklogVisibilityPolicy worklogVisibilityPolicy;

    @Override
    public PageResponse<SearchWorklogsApiDto.Response.Item> searchWorklogs(
            CustomUserPrincipal principal,
            SearchWorklogsApiDto.Request request
    ) {
        WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);
        WorklogSearchQuery query = WorklogSearchQuery.from(request, LocalDate.now());
        return SearchWorklogsApiDto.Response.fromPage(
                worklogRepository.searchWorklogPage(scope, query));
    }
}
