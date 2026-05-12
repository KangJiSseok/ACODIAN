package com.ibank.axwms.domain.worklog.service.search;

import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamMemberFilterProjection;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagSummaryProjection;
import com.ibank.axwms.domain.worklog.dto.GetWorklogFilterOptionsApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityPolicy;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
    private final TeamRepository teamRepository;
    private final TagRepository tagRepository;

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

    @Override
    public GetWorklogFilterOptionsApiDto.Response getFilterOptions(CustomUserPrincipal principal) {
        List<TeamMemberFilterProjection> teamMemberRows =
                teamRepository.findVisibleTeamsWithMembersForFilter(principal.userId());
        List<TagSummaryProjection> tagRows = tagRepository.findAllTagSummaries();
        return GetWorklogFilterOptionsApiDto.Response.of(teamMemberRows, tagRows);
    }
}
