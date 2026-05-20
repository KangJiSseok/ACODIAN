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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LightRagWorklogSearchServiceTest {

    @Mock private WorklogVisibilityPolicy worklogVisibilityPolicy;
    @Mock private WorklogRepository worklogRepository;
    @Mock private LightRagWorklogSearchClient lightRagWorklogSearchClient;

    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 21L;
    private static final Long SECOND_TEAM_ID = 22L;

    @Test
    @DisplayName("시맨틱 검색이 꺼져 있으면 LightRAG를 호출하지 않고 예외를 던진다")
    void 시맨틱_검색이_꺼져_있으면_LightRAG를_호출하지_않고_예외를_던진다() {
        LightRagWorklogSearchService service = service(false);

        assertThatThrownBy(() -> service.searchWorklogs(principal(), request()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
        verifyNoInteractions(worklogVisibilityPolicy, worklogRepository, lightRagWorklogSearchClient);
    }

    @Test
    @DisplayName("LightRAG native 응답을 semantic 응답으로 반환한다")
    void LightRAG_native_응답을_semantic_응답으로_반환한다() {
        LightRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        SearchSemanticWorklogsApiDto.Request request = request();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of(TEAM_ID, SECOND_TEAM_ID));
        given(lightRagWorklogSearchClient.queryWorklogs(any())).willReturn(lightRagResponse());

        SearchSemanticWorklogsApiDto.Response response = service.searchWorklogs(principal, request);

        ArgumentCaptor<InternalLightRagWorklogQueryApiDto.Request> lightRagCaptor =
                ArgumentCaptor.forClass(InternalLightRagWorklogQueryApiDto.Request.class);
        verify(lightRagWorklogSearchClient).queryWorklogs(lightRagCaptor.capture());
        assertThat(lightRagCaptor.getValue().query()).isEqualTo("결산 업무 알려줘");
        assertThat(lightRagCaptor.getValue().allowedTeamIds()).containsExactly(TEAM_ID, SECOND_TEAM_ID);

        assertThat(response.answer()).isEqualTo("결산 보고서 작성 업무가 있습니다.");
        assertThat(response.references()).hasSize(2);
        assertThat(response.references().get(0).referenceId()).isEqualTo("ref-a");
        assertThat(response.references().get(0).filePath()).isEqualTo("worklog://502");
        assertThat(response.references().get(1).referenceId()).isEqualTo("ref-b");
        assertThat(response.references().get(1).filePath()).isEqualTo("worklog://501");
    }

    @Test
    @DisplayName("접근 가능한 팀이 없으면 LightRAG 호출 없이 빈 semantic 응답을 반환한다")
    void 접근_가능한_팀이_없으면_LightRAG_호출_없이_빈_semantic_응답을_반환한다() {
        LightRagWorklogSearchService service = service(true);
        CustomUserPrincipal principal = principal();
        WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

        given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
        given(worklogRepository.findVisibleTeamIds(scope)).willReturn(List.of());

        SearchSemanticWorklogsApiDto.Response response = service.searchWorklogs(principal, request());

        assertThat(response.answer()).isEmpty();
        assertThat(response.references()).isEmpty();
        verify(lightRagWorklogSearchClient, never()).queryWorklogs(any());
    }

    private LightRagWorklogSearchService service(boolean enabled) {
        return new LightRagWorklogSearchService(
                worklogVisibilityPolicy,
                worklogRepository,
                lightRagWorklogSearchClient,
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

    private static InternalLightRagWorklogQueryApiDto.Response lightRagResponse() {
        return new InternalLightRagWorklogQueryApiDto.Response(
                "결산 보고서 작성 업무가 있습니다.",
                List.of(
                        new InternalLightRagWorklogQueryApiDto.ReferenceItem("ref-a", "worklog://502"),
                        new InternalLightRagWorklogQueryApiDto.ReferenceItem("ref-b", "worklog://501")
                ),
                "mix",
                true
        );
    }

    private static CustomUserPrincipal principal() {
        return new CustomUserPrincipal(USER_ID, "test@test.com", "MEMBER");
    }
}
