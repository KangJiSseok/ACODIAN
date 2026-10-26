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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GraphRagWorklogSearchServiceTest {

    @Mock private WorklogVisibilityPolicy worklogVisibilityPolicy;
    @Mock private WorklogRepository worklogRepository;
    @Mock private GraphRagWorklogSearchClient graphRagWorklogSearchClient;

    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 21L;
    private static final Long SECOND_TEAM_ID = 22L;

    @Test
    @DisplayName("public semantic 요청 DTO는 teamIds와 allowedTeamIds를 받지 않는다")
    void public_semantic_요청_DTO는_teamIds와_allowedTeamIds를_받지_않는다() {
        Set<String> componentNames = Arrays.stream(SearchSemanticWorklogsApiDto.Request.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(componentNames).containsExactly("query");
        assertThat(componentNames).doesNotContain("teamIds", "allowedTeamIds");
    }

    @Test
    @DisplayName("GraphRAG 내부 요청 DTO는 명시적 scopeMode로 팀 범위를 표현한다")
    void GraphRAG_내부_요청_DTO는_명시적_scopeMode로_팀_범위를_표현한다() {
        InternalGraphRagWorklogQueryApiDto.Request teamScope =
                InternalGraphRagWorklogQueryApiDto.Request.forTeamIds("정산", List.of(7L));
        InternalGraphRagWorklogQueryApiDto.Request allScope =
                InternalGraphRagWorklogQueryApiDto.Request.forAll("정산");

        assertThat(teamScope.scopeMode()).isEqualTo(InternalGraphRagWorklogQueryApiDto.ScopeMode.TEAM_IDS);
        assertThat(teamScope.allowedTeamIds()).containsExactly(7L);
        assertThat(allScope.scopeMode()).isEqualTo(InternalGraphRagWorklogQueryApiDto.ScopeMode.ALL);
        assertThat(allScope.allowedTeamIds()).isNull();
    }

    @Test
    @DisplayName("시맨틱 검색이 꺼져 있으면 GraphRAG를 호출하지 않고 예외를 던진다")
    void 시맨틱_검색이_꺼져_있으면_GraphRAG를_호출하지_않고_예외를_던진다() {
        GraphRagWorklogSearchService service = service(false);

        assertThatThrownBy(() -> service.searchWorklogs(principal(), request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        verifyNoInteractions(worklogVisibilityPolicy, worklogRepository, graphRagWorklogSearchClient);
    }

    @Test
    @DisplayName("팀 제한 범위이면 GraphRAG TEAM_IDS 요청으로 변환하고 응답을 반환한다")
    void 팀_제한_범위이면_GraphRAG_TEAM_IDS_요청으로_변환하고_응답을_공개_DTO로_반환한다() {
        GraphRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID, SECOND_TEAM_ID));
        given(graphRagWorklogSearchClient.queryWorklogs(any())).willReturn(graphRagResponse());
        given(worklogRepository.findSearchWorklogsByIds(scope, List.of(502L, 501L))).willReturn(List.of(
                worklogProjection(502L, TEAM_ID),
                worklogProjection(501L, SECOND_TEAM_ID)
        ));

        SearchSemanticWorklogsApiDto.Response response = service.searchWorklogs(principal, request());

        ArgumentCaptor<InternalGraphRagWorklogQueryApiDto.Request> graphRagCaptor =
                ArgumentCaptor.forClass(InternalGraphRagWorklogQueryApiDto.Request.class);
        verify(graphRagWorklogSearchClient).queryWorklogs(graphRagCaptor.capture());
        assertThat(graphRagCaptor.getValue().query()).isEqualTo("결산 업무 알려줘");
        assertThat(graphRagCaptor.getValue().scopeMode())
                .isEqualTo(InternalGraphRagWorklogQueryApiDto.ScopeMode.TEAM_IDS);
        assertThat(graphRagCaptor.getValue().allowedTeamIds()).containsExactly(TEAM_ID, SECOND_TEAM_ID);

        assertThat(response.answer()).isEqualTo("결산 보고서 작성 업무가 있습니다.");
        assertThat(response.references()).hasSize(2);
        assertThat(response.references().get(0).referenceId()).isEqualTo("ref-a");
        assertThat(response.references().get(0).filePath()).isEqualTo("worklog://502");
        assertThat(response.references().get(1).referenceId()).isEqualTo("ref-b");
        assertThat(response.references().get(1).filePath()).isEqualTo("worklog://501");
    }

    @Test
    @DisplayName("GraphRAG reference가 현재 scope에서 모두 보이면 응답을 반환한다")
    void GraphRAG_reference가_현재_scope에서_모두_보이면_응답을_반환한다() {
        GraphRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID));
        given(graphRagWorklogSearchClient.queryWorklogs(any())).willReturn(graphRagResponseWithReferences(
                List.of(
                        new InternalGraphRagWorklogQueryApiDto.ReferenceItem("ref-a", "worklog://502"),
                        new InternalGraphRagWorklogQueryApiDto.ReferenceItem("ref-duplicate", "worklog://502")
                )
        ));
        given(worklogRepository.findSearchWorklogsByIds(scope, List.of(502L))).willReturn(List.of(
                worklogProjection(502L, TEAM_ID)
        ));

        SearchSemanticWorklogsApiDto.Response response = service.searchWorklogs(principal, request());

        assertThat(response.references()).hasSize(2);
        verify(worklogRepository).findSearchWorklogsByIds(scope, List.of(502L));
    }

    @Test
    @DisplayName("GraphRAG reference가 현재 scope에서 누락되면 응답 전체를 차단한다")
    void GraphRAG_reference가_현재_scope에서_누락되면_응답_전체를_차단한다() {
        GraphRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID));
        given(graphRagWorklogSearchClient.queryWorklogs(any())).willReturn(graphRagResponseWithReferences(
                List.of(new InternalGraphRagWorklogQueryApiDto.ReferenceItem("ref-leak", "worklog://999"))
        ));
        given(worklogRepository.findSearchWorklogsByIds(scope, List.of(999L))).willReturn(List.of());

        assertThatThrownBy(() -> service.searchWorklogs(principal, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
    }

    @Test
    @DisplayName("GraphRAG reference가 비업무일지 URI이면 응답 전체를 차단한다")
    void GraphRAG_reference가_비업무일지_URI이면_응답_전체를_차단한다() {
        GraphRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID));
        given(graphRagWorklogSearchClient.queryWorklogs(any())).willReturn(graphRagResponseWithReferences(
                List.of(
                        new InternalGraphRagWorklogQueryApiDto.ReferenceItem("ref-doc", "document://manual"),
                        new InternalGraphRagWorklogQueryApiDto.ReferenceItem("ref-bad", "worklog://not-a-number")
                )
        ));

        assertThatThrownBy(() -> service.searchWorklogs(principal, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        verify(worklogRepository, never()).findSearchWorklogsByIds(any(), any());
    }

    @Test
    @DisplayName("GraphRAG reference가 비어 있으면 scope 재조회를 생략한다")
    void GraphRAG_reference가_비어_있으면_scope_재조회를_생략한다() {
        GraphRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID));
        given(graphRagWorklogSearchClient.queryWorklogs(any())).willReturn(graphRagResponseWithReferences(List.of()));

        SearchSemanticWorklogsApiDto.Response response = service.searchWorklogs(principal, request());

        assertThat(response.references()).isEmpty();
        verify(worklogRepository, never()).findSearchWorklogsByIds(any(), any());
    }

    @Test
    @DisplayName("전체 범위이면 GraphRAG ALL 요청으로 변환하고 팀 목록 조회를 생략한다")
    void 전체_범위이면_GraphRAG_ALL_요청으로_변환하고_팀_목록_조회를_생략한다() {
        GraphRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(new WorklogVisibilityScope.All());
        given(graphRagWorklogSearchClient.queryWorklogs(any())).willReturn(graphRagResponse());
        given(worklogRepository.findSearchWorklogsByIds(
                new WorklogVisibilityScope.All(),
                List.of(502L, 501L)
        )).willReturn(List.of(
                worklogProjection(502L, TEAM_ID),
                worklogProjection(501L, SECOND_TEAM_ID)
        ));

        service.searchWorklogs(principal, request());

        ArgumentCaptor<InternalGraphRagWorklogQueryApiDto.Request> graphRagCaptor =
                ArgumentCaptor.forClass(InternalGraphRagWorklogQueryApiDto.Request.class);
        verify(graphRagWorklogSearchClient).queryWorklogs(graphRagCaptor.capture());
        assertThat(graphRagCaptor.getValue().scopeMode()).isEqualTo(InternalGraphRagWorklogQueryApiDto.ScopeMode.ALL);
        assertThat(graphRagCaptor.getValue().allowedTeamIds()).isNull();
        verify(worklogRepository, never()).findVisibleTeamIds(any());
    }

    @Test
    @DisplayName("접근 가능한 팀이 없으면 GraphRAG 호출 없이 빈 semantic 응답을 반환한다")
    void 접근_가능한_팀이_없으면_GraphRAG_호출_없이_빈_semantic_응답을_반환한다() {
        GraphRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of());

        SearchSemanticWorklogsApiDto.Response response = service.searchWorklogs(principal, request());

        assertThat(response.answer()).isEmpty();
        assertThat(response.references()).isEmpty();
        verify(graphRagWorklogSearchClient, never()).queryWorklogs(any());
    }

    private GraphRagWorklogSearchService service(boolean enabled) {
        return new GraphRagWorklogSearchService(
                worklogVisibilityPolicy,
                worklogRepository,
                graphRagWorklogSearchClient,
                new AiWorklogSearchProperties(
                        enabled,
                        "http://localhost:8000/ai",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(120))
        );
    }

    private static SearchSemanticWorklogsApiDto.Request request() {
        return new SearchSemanticWorklogsApiDto.Request("결산 업무 알려줘");
    }

    private static InternalGraphRagWorklogQueryApiDto.Response graphRagResponse() {
        return graphRagResponseWithReferences(List.of(
                new InternalGraphRagWorklogQueryApiDto.ReferenceItem("ref-a", "worklog://502"),
                new InternalGraphRagWorklogQueryApiDto.ReferenceItem("ref-b", "worklog://501")
        ));
    }

    private static InternalGraphRagWorklogQueryApiDto.Response graphRagResponseWithReferences(
            List<InternalGraphRagWorklogQueryApiDto.ReferenceItem> references
    ) {
        return new InternalGraphRagWorklogQueryApiDto.Response(
                "결산 보고서 작성 업무가 있습니다.",
                references,
                "local",
                true
        );
    }

    private static WorklogSearchProjection worklogProjection(Long worklogId, Long teamId) {
        return new WorklogSearchProjection(
                worklogId,
                "업무 " + worklogId,
                "수행",
                BigDecimal.ONE,
                null,
                false,
                "COMPLETED",
                "MEDIUM",
                "COMPLETED",
                0,
                teamId,
                "팀 " + teamId,
                USER_ID,
                "김도윤",
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2)
        );
    }

    private static CustomUserPrincipal principal() {
        return new CustomUserPrincipal(USER_ID, "test@test.com", "MEMBER");
    }
}
