package com.ibank.axwms.domain.worklog.service.search;

import com.ibank.axwms.domain.worklog.dto.InternalGraphRagWorklogQueryApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchSemanticWorklogsApiDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogSearchProperties;
import com.ibank.axwms.domain.worklog.external.GraphRagWorklogSearchClient;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityPolicy;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GraphRAG v1 query API 응답을 업무일지 시맨틱 검색 응답으로 연결하는 서비스다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphRagWorklogSearchService {

    private final WorklogVisibilityPolicy worklogVisibilityPolicy;
    private final WorklogRepository worklogRepository;
    private final GraphRagWorklogSearchClient graphRagWorklogSearchClient;
    private final AiWorklogSearchProperties aiWorklogSearchProperties;

    /**
     * API 권한 범위를 scopeMode로 변환해 GraphRAG에 전달하고
     * native answer/reference 계약을 공개 응답으로 바꾼다.
     */
    public SearchSemanticWorklogsApiDto.Response searchWorklogs(
            CustomUserPrincipal principal,
            SearchSemanticWorklogsApiDto.Request request
    ) {
        if (!aiWorklogSearchProperties.enabled()) {
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        }

        WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);
        Optional<InternalGraphRagWorklogQueryApiDto.Request> graphRagRequest =
                buildGraphRagRequest(scope, request.query());
        if (graphRagRequest.isEmpty()) {
            return new SearchSemanticWorklogsApiDto.Response("", List.of());
        }

        InternalGraphRagWorklogQueryApiDto.Response graphRagResponse =
                graphRagWorklogSearchClient.queryWorklogs(graphRagRequest.get());
        validatePublicReferences(scope, graphRagResponse.references());
        return SearchSemanticWorklogsApiDto.Response.from(graphRagResponse);
    }

    /** AI reference가 검증 가능한 현재 사용자 가시 worklog가 아니면 공개 응답 전체를 차단한다. */
    private void validatePublicReferences(
            WorklogVisibilityScope scope,
            List<InternalGraphRagWorklogQueryApiDto.ReferenceItem> references
    ) {
        List<Long> referencedWorklogIds = parseReferencedWorklogIdsOrThrow(references);
        if (referencedWorklogIds.isEmpty()) {
            return;
        }

        Set<Long> visibleWorklogIds = worklogRepository.findSearchWorklogsByIds(scope, referencedWorklogIds).stream()
                .map(WorklogSearchProjection::worklogId)
                .collect(Collectors.toSet());
        if (!visibleWorklogIds.equals(Set.copyOf(referencedWorklogIds))) {
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        }
    }

    /** public semantic response에는 검증 가능한 worklog URI reference만 허용한다. */
    private static List<Long> parseReferencedWorklogIdsOrThrow(
            List<InternalGraphRagWorklogQueryApiDto.ReferenceItem> references
    ) {
        if (references == null || references.isEmpty()) {
            return List.of();
        }
        return references.stream()
                .map(InternalGraphRagWorklogQueryApiDto.ReferenceItem::filePath)
                .map(GraphRagWorklogSearchService::parseWorklogReferenceIdOrThrow)
                .distinct()
                .toList();
    }

    /** malformed 또는 비업무일지 URI가 public response로 나가는 것을 차단한다. */
    private static Long parseWorklogReferenceIdOrThrow(String filePath) {
        if (filePath == null || !filePath.startsWith("worklog://")) {
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        }
        try {
            return Long.parseLong(filePath.substring("worklog://".length()));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        }
    }

    /** 전체 범위는 ALL, 팀 제한 범위는 비어 있지 않은 TEAM_IDS 요청으로만 변환한다. */
    private Optional<InternalGraphRagWorklogQueryApiDto.Request> buildGraphRagRequest(
            WorklogVisibilityScope scope,
            String query
    ) {
        return switch (scope) {
            case WorklogVisibilityScope.All ignored ->
                    Optional.of(InternalGraphRagWorklogQueryApiDto.Request.forAll(query));
            case WorklogVisibilityScope.Department ignored -> buildTeamScopeRequest(scope, query);
            case WorklogVisibilityScope.MyTeams ignored -> buildTeamScopeRequest(scope, query);
        };
    }

    /** 접근 가능한 팀이 없으면 AI 호출 생략을 Optional.empty()로 표현한다. */
    private Optional<InternalGraphRagWorklogQueryApiDto.Request> buildTeamScopeRequest(
            WorklogVisibilityScope scope,
            String query
    ) {
        List<Long> allowedTeamIds = worklogRepository.findVisibleTeamIds(scope);
        if (allowedTeamIds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(InternalGraphRagWorklogQueryApiDto.Request.forTeamIds(query, allowedTeamIds));
    }
}
