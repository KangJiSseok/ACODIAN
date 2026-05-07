package com.ibank.axwms.domain.worklog.service.search;

import com.ibank.axwms.domain.worklog.dto.GetWorklogFilterOptionsApiDto;
import com.ibank.axwms.domain.worklog.dto.InternalSemanticWorklogSearchApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogSearchProperties;
import com.ibank.axwms.domain.worklog.external.SemanticWorklogSearchClient;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityPolicy;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HybridWorklogSearchService implements WorklogSearchService {

    private final KeywordWorklogSearchService keywordWorklogSearchService;
    private final WorklogVisibilityPolicy worklogVisibilityPolicy;
    private final WorklogRepository worklogRepository;
    private final SemanticWorklogSearchClient semanticWorklogSearchClient;
    private final AiWorklogSearchProperties aiWorklogSearchProperties;

    /**
     * 검색어가 있으면 AI semantic ranking 을 우선 사용하고, 불가한 경우 기존 keyword 검색으로 대체한다.
     */
    @Override
    public PageResponse<SearchWorklogsApiDto.Response.Item> searchWorklogs(
            CustomUserPrincipal principal,
            SearchWorklogsApiDto.Request request
    ) {
        WorklogSearchQuery query = WorklogSearchQuery.from(request, LocalDate.now());
        if (!shouldUseSemanticSearch(query)) {
            return keywordWorklogSearchService.searchWorklogs(principal, request);
        }

        WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);
        List<Long> allowedTeamIds = resolveAllowedTeamIds(scope);
        if (allowedTeamIds != null && allowedTeamIds.isEmpty()) {
            return emptyPage(query);
        }

        try {
            InternalSemanticWorklogSearchApiDto.Response semanticResponse =
                    semanticWorklogSearchClient.searchWorklogs(
                            InternalSemanticWorklogSearchApiDto.Request.from(query, allowedTeamIds));
            return toApiPage(scope, semanticResponse);
        } catch (RestClientException ex) {
            log.warn("AI semantic search failed. fallback to keyword search. userId={}", principal.userId(), ex);
            return keywordWorklogSearchService.searchWorklogs(principal, request);
        }
    }

    @Override
    public GetWorklogFilterOptionsApiDto.Response getFilterOptions(CustomUserPrincipal principal) {
        return keywordWorklogSearchService.getFilterOptions(principal);
    }

    /**
     * 검색어가 있고 설정이 켜진 경우에만 외부 AI 호출을 시도해 기존 keyword 검색 지연을 피한다.
     */
    private boolean shouldUseSemanticSearch(WorklogSearchQuery query) {
        return aiWorklogSearchProperties.enabled() && query.keyword() != null;
    }

    /**
     * DIRECTOR 전체 범위는 null 로 넘기고, 그 외 역할은 실제 접근 가능한 팀 ID 배열로 제한한다.
     */
    private List<Long> resolveAllowedTeamIds(WorklogVisibilityScope scope) {
        return switch (scope) {
            case WorklogVisibilityScope.All ignored -> null;
            case WorklogVisibilityScope.Department ignored -> worklogRepository.findVisibleTeamIds(scope);
            case WorklogVisibilityScope.MyTeams ignored -> worklogRepository.findVisibleTeamIds(scope);
        };
    }

    /**
     * 접근 가능한 팀이 없을 때 AI 서버를 호출하지 않고 같은 페이지 계약의 빈 응답을 만든다.
     */
    private PageResponse<SearchWorklogsApiDto.Response.Item> emptyPage(WorklogSearchQuery query) {
        return new PageResponse<>(
                List.of(),
                query.page(),
                query.pageSize(),
                0,
                0,
                true,
                true,
                false,
                false
        );
    }

    /**
     * AI ranking 결과는 ID 순서만 신뢰하고, 화면 응답 필드는 API DB에서 최신 projection 으로 재조회한다.
     */
    private PageResponse<SearchWorklogsApiDto.Response.Item> toApiPage(
            WorklogVisibilityScope scope,
            InternalSemanticWorklogSearchApiDto.Response semanticResponse
    ) {
        List<Long> worklogIds = semanticResponse.items().stream()
                .map(InternalSemanticWorklogSearchApiDto.Item::worklogId)
                .toList();
        Map<Long, WorklogSearchProjection> projectionsById = worklogRepository.findSearchWorklogsByIds(scope, worklogIds)
                .stream()
                .collect(Collectors.toMap(WorklogSearchProjection::worklogId, Function.identity()));
        List<SearchWorklogsApiDto.Response.Item> items = semanticResponse.items().stream()
                .map(InternalSemanticWorklogSearchApiDto.Item::worklogId)
                .map(projectionsById::get)
                .filter(projection -> projection != null)
                .map(SearchWorklogsApiDto.Response.Item::from)
                .toList();

        return new PageResponse<>(
                items,
                semanticResponse.page(),
                semanticResponse.pageSize(),
                semanticResponse.totalCount(),
                semanticResponse.totalPages(),
                semanticResponse.isFirst(),
                semanticResponse.isLast(),
                semanticResponse.hasNext(),
                semanticResponse.hasPrevious()
        );
    }
}
