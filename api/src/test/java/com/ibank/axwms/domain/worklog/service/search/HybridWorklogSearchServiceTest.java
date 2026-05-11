package com.ibank.axwms.domain.worklog.service.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.InternalSemanticWorklogSearchApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogSearchProperties;
import com.ibank.axwms.domain.worklog.external.SemanticWorklogSearchClient;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityPolicy;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.global.enums.PeriodOption;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HybridWorklogSearchServiceTest {

    @Mock private KeywordWorklogSearchService keywordWorklogSearchService;
    @Mock private WorklogVisibilityPolicy worklogVisibilityPolicy;
    @Mock private WorklogRepository worklogRepository;
    @Mock private SemanticWorklogSearchClient semanticWorklogSearchClient;

    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 21L;
    private static final Long SECOND_TEAM_ID = 22L;
    private static final Long AUTHOR_ID = 7L;
    private static final Long TAG_ID = 33L;
    private static final Long WORKLOG_ID = 501L;
    private static final LocalDate INSTRUCTION_DATE = LocalDate.of(2026, 4, 22);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 4, 25);

    @Test
    @DisplayName("시맨틱 검색이 꺼져 있으면 기존 키워드 검색으로 위임한다")
    void 시맨틱_검색이_꺼져_있으면_기존_키워드_검색으로_위임한다() {
        // given
        HybridWorklogSearchService service = service(false);
        CustomUserPrincipal principal = principal();
        SearchWorklogsApiDto.Request request = request();
        PageResponse<SearchWorklogsApiDto.Response.Item> fallbackResponse = emptyResponse();

        given(keywordWorklogSearchService.searchWorklogs(principal, request)).willReturn(fallbackResponse);

        // when
        PageResponse<SearchWorklogsApiDto.Response.Item> response = service.searchWorklogs(principal, request);

        // then
        assertThat(response).isSameAs(fallbackResponse);
        verifyNoInteractions(worklogVisibilityPolicy, semanticWorklogSearchClient);
    }

    @Test
    @DisplayName("시맨틱 검색이 성공하면 AI 결과 ID 순서대로 API projection 을 재조회해 응답한다")
    void 시맨틱_검색이_성공하면_AI_결과_ID_순서대로_API_projection_을_재조회한다() {
        // given
        HybridWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        SearchWorklogsApiDto.Request request = request();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);
        InternalSemanticWorklogSearchApiDto.Response semanticResponse = semanticResponse();

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID, SECOND_TEAM_ID));
        given(semanticWorklogSearchClient.searchWorklogs(any())).willReturn(semanticResponse);
        given(worklogRepository.findSearchWorklogsByIds(scope, List.of(WORKLOG_ID)))
                .willReturn(List.of(sampleProjection()));

        // when
        PageResponse<SearchWorklogsApiDto.Response.Item> response = service.searchWorklogs(principal, request);

        // then
        ArgumentCaptor<InternalSemanticWorklogSearchApiDto.Request> captor =
                ArgumentCaptor.forClass(InternalSemanticWorklogSearchApiDto.Request.class);
        verify(semanticWorklogSearchClient).searchWorklogs(captor.capture());
        InternalSemanticWorklogSearchApiDto.Request captured = captor.getValue();
        assertThat(captured.keyword()).isEqualTo("결산");
        assertThat(captured.teamId()).isEqualTo(TEAM_ID);
        assertThat(captured.statusCode()).isEqualTo(WorklogStatus.IN_PROGRESS);
        assertThat(captured.importanceCode()).isEqualTo(WorklogImportance.HIGH);
        assertThat(captured.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(captured.tagId()).isEqualTo(TAG_ID);
        assertThat(captured.allowedTeamIds()).containsExactly(TEAM_ID, SECOND_TEAM_ID);
        assertThat(captured.page()).isEqualTo(1);
        assertThat(captured.pageSize()).isEqualTo(20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).worklogId()).isEqualTo(WORKLOG_ID);
        assertThat(response.items().get(0).teamName()).isEqualTo("물류혁신TF");
        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("접근 가능한 팀이 없으면 AI 호출 없이 빈 페이지를 반환한다")
    void 접근_가능한_팀이_없으면_AI_호출_없이_빈_페이지를_반환한다() {
        // given
        HybridWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        SearchWorklogsApiDto.Request request = request();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of());

        // when
        PageResponse<SearchWorklogsApiDto.Response.Item> response = service.searchWorklogs(principal, request);

        // then
        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
        verify(semanticWorklogSearchClient, never()).searchWorklogs(any());
        verify(keywordWorklogSearchService, never()).searchWorklogs(any(), any());
    }

    @Test
    @DisplayName("AI 호출이 실패하면 기존 키워드 검색으로 fallback 한다")
    void AI_호출이_실패하면_기존_키워드_검색으로_fallback_한다() {
        // given
        HybridWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        SearchWorklogsApiDto.Request request = request();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);
        PageResponse<SearchWorklogsApiDto.Response.Item> fallbackResponse = emptyResponse();

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID));
        given(semanticWorklogSearchClient.searchWorklogs(any())).willThrow(new RestClientException("AI down"));
        given(keywordWorklogSearchService.searchWorklogs(principal, request)).willReturn(fallbackResponse);

        // when
        PageResponse<SearchWorklogsApiDto.Response.Item> response = service.searchWorklogs(principal, request);

        // then
        assertThat(response).isSameAs(fallbackResponse);
    }

    @Test
    @DisplayName("AI 응답 JSON 의 isFirst 와 isLast 를 내부 DTO 로 역직렬화한다")
    void AI_응답_JSON_의_isFirst_와_isLast_를_내부_DTO_로_역직렬화한다() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        String json = """
                {
                  "items": [],
                  "page": 1,
                  "pageSize": 20,
                  "totalCount": 0,
                  "totalPages": 0,
                  "isFirst": true,
                  "isLast": true,
                  "hasNext": false,
                  "hasPrevious": false
                }
                """;

        // when
        InternalSemanticWorklogSearchApiDto.Response response =
                objectMapper.readValue(json, InternalSemanticWorklogSearchApiDto.Response.class);

        // then
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    private HybridWorklogSearchService service(boolean enabled) {
        return new HybridWorklogSearchService(
                keywordWorklogSearchService,
                worklogVisibilityPolicy,
                worklogRepository,
                semanticWorklogSearchClient,
                new AiWorklogSearchProperties(
                        enabled,
                        "http://localhost:8000/ai",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1))
        );
    }

    private static SearchWorklogsApiDto.Request request() {
        return new SearchWorklogsApiDto.Request(
                "결산",
                TEAM_ID,
                null,
                WorklogStatus.IN_PROGRESS,
                WorklogImportance.HIGH,
                AUTHOR_ID,
                TAG_ID,
                PeriodOption.LAST_30,
                1,
                20
        );
    }

    private static InternalSemanticWorklogSearchApiDto.Response semanticResponse() {
        return new InternalSemanticWorklogSearchApiDto.Response(
                List.of(new InternalSemanticWorklogSearchApiDto.Item(
                        WORKLOG_ID,
                        0.91,
                        0,
                        "결산 보고서 작성 chunk",
                        List.of()
                )),
                1,
                20,
                1,
                1,
                true,
                true,
                false,
                false
        );
    }

    private static PageResponse<SearchWorklogsApiDto.Response.Item> emptyResponse() {
        return new PageResponse<>(List.of(), 1, 20, 0, 0, true, true, false, false);
    }

    private static WorklogSearchProjection sampleProjection() {
        return new WorklogSearchProjection(
                WORKLOG_ID,
                "결산 보고서 작성",
                "결산 보고서 초안을 작성하고 검토 의견을 반영했습니다.",
                BigDecimal.valueOf(2.5),
                "AI 요약",
                false,
                "IN_PROGRESS",
                "HIGH",
                "COMPLETED",
                2,
                TEAM_ID,
                "물류혁신TF",
                AUTHOR_ID,
                "홍길동",
                "https://cdn.axwms.com/profile/7.png",
                INSTRUCTION_DATE,
                DUE_DATE
        );
    }

    private static CustomUserPrincipal principal() {
        return new CustomUserPrincipal(USER_ID, "test@test.com", "MEMBER");
    }
}
