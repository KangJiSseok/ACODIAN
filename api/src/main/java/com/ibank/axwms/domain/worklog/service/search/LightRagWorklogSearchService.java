package com.ibank.axwms.domain.worklog.service.search;

import com.ibank.axwms.domain.worklog.dto.InternalLightRagWorklogQueryApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchSemanticWorklogsApiDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogSearchProperties;
import com.ibank.axwms.domain.worklog.external.LightRagWorklogSearchClient;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityPolicy;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * LightRAG v3 query API 응답을 업무일지 시맨틱 검색 응답으로 연결하는 서비스다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LightRagWorklogSearchService {

    private final WorklogVisibilityPolicy worklogVisibilityPolicy;
    private final WorklogRepository worklogRepository;
    private final LightRagWorklogSearchClient lightRagWorklogSearchClient;
    private final AiWorklogSearchProperties aiWorklogSearchProperties;

    /**
     * LightRAG가 생성한 answer/reference 계약을 유지해 반환하고, API 권한 범위는 allowedTeamIds로 AI 서버에 전달한다.
     */
    public SearchSemanticWorklogsApiDto.Response searchWorklogs(
            CustomUserPrincipal principal,
            SearchSemanticWorklogsApiDto.Request request
    ) {
        if (!aiWorklogSearchProperties.enabled()) {
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        }

        WorklogVisibilityScope scope = worklogVisibilityPolicy.resolve(principal);
        List<Long> allowedTeamIds = resolveAllowedTeamIds(scope);
        if (allowedTeamIds != null && allowedTeamIds.isEmpty()) {
            return new SearchSemanticWorklogsApiDto.Response("", List.of());
        }

        InternalLightRagWorklogQueryApiDto.Response lightRagResponse =
                lightRagWorklogSearchClient.queryWorklogs(
                        InternalLightRagWorklogQueryApiDto.Request.of(request.query(), allowedTeamIds));
        return SearchSemanticWorklogsApiDto.Response.from(lightRagResponse);
    }

    /**
     * DIRECTOR 전체 범위는 null로 열어 두고, 나머지 역할은 실제 접근 가능한 팀 ID 목록으로 제한한다.
     */
    private List<Long> resolveAllowedTeamIds(WorklogVisibilityScope scope) {
        return switch (scope) {
            case WorklogVisibilityScope.All ignored -> null;
            case WorklogVisibilityScope.Department ignored -> worklogRepository.findVisibleTeamIds(scope);
            case WorklogVisibilityScope.MyTeams ignored -> worklogRepository.findVisibleTeamIds(scope);
        };
    }
}
