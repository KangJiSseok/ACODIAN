package com.ibank.axwms.domain.worklog.service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityPolicy;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import com.ibank.axwms.global.enums.PeriodOption;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class KeywordWorklogSearchServiceTest {

    @Mock private WorklogRepository worklogRepository;
    @Mock private WorklogVisibilityPolicy worklogVisibilityPolicy;
    @Mock private TeamRepository teamRepository;
    @Mock private TagRepository tagRepository;

    @InjectMocks private KeywordWorklogSearchService keywordWorklogSearchService;

    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 21L;
    private static final Long AUTHOR_ID = 7L;
    private static final Long TAG_ID = 33L;
    private static final Long WORKLOG_ID = 501L;
    private static final LocalDate INSTRUCTION_DATE = LocalDate.of(2026, 4, 22);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 4, 25);

    @Nested
    @DisplayName("Repository 위임")
    class RepositoryDelegation {

        @Test
        @DisplayName("정책이 결정한 Scope 와 정규화된 Query 를 Repository 에 위임하고 응답을 매핑한다")
        void 정책이_결정한_Scope_와_정규화된_Query_를_Repository_에_위임한다() {
            // given
            CustomUserPrincipal principal = principal();
            WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);
            SearchWorklogsApiDto.Request request = new SearchWorklogsApiDto.Request(
                    "결산",
                    TEAM_ID,
                    null,
                    WorklogStatus.IN_PROGRESS,
                    WorklogImportance.HIGH,
                    AUTHOR_ID,
                    TAG_ID,
                    PeriodOption.LAST_30,
                    2,
                    50
            );
            Page<WorklogSearchProjection> projectionPage = new PageImpl<>(
                    List.of(sampleProjection()),
                    PageRequest.of(1, 50),
                    51
            );

            given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
            given(worklogRepository.searchWorklogPage(eq(scope), any(WorklogSearchQuery.class)))
                    .willReturn(projectionPage);

            // when
            // service 내부 LocalDate.now() 와 같은 날 안에서 today 를 캡처해 createdFrom 을 비교한다.
            LocalDate today = LocalDate.now();
            PageResponse<SearchWorklogsApiDto.Response.Item> response =
                    keywordWorklogSearchService.searchWorklogs(principal, request);

            // then
            WorklogSearchQuery captured = captureQuery(scope);
            assertThat(captured.keyword()).isEqualTo("결산");
            assertThat(captured.teamId()).isEqualTo(TEAM_ID);
            assertThat(captured.statusCode()).isEqualTo(WorklogStatus.IN_PROGRESS);
            assertThat(captured.importanceCode()).isEqualTo(WorklogImportance.HIGH);
            assertThat(captured.authorId()).isEqualTo(AUTHOR_ID);
            assertThat(captured.tagId()).isEqualTo(TAG_ID);
            assertThat(captured.createdFrom()).isEqualTo(today.minusDays(30));
            assertThat(captured.page()).isEqualTo(2);
            assertThat(captured.pageSize()).isEqualTo(50);

            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).worklogId()).isEqualTo(WORKLOG_ID);
            assertThat(response.items().get(0).teamName()).isEqualTo("물류혁신TF");
            assertThat(response.items().get(0).predecessorCount()).isEqualTo(2);
            assertThat(response.totalCount()).isEqualTo(51);
            assertThat(response.page()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Query 정규화")
    class QueryNormalization {

        @Test
        @DisplayName("Request 가 null 이면 모든 필터를 null 로 두고 page=1, pageSize=20 기본값으로 위임한다")
        void Request_가_null_이면_기본값_Query_를_사용한다() {
            // given
            CustomUserPrincipal principal = principal();
            WorklogVisibilityScope scope = new WorklogVisibilityScope.All();

            given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
            given(worklogRepository.searchWorklogPage(eq(scope), any(WorklogSearchQuery.class)))
                    .willReturn(emptyPage());

            // when
            keywordWorklogSearchService.searchWorklogs(principal, null);

            // then
            WorklogSearchQuery captured = captureQuery(scope);
            assertThat(captured.keyword()).isNull();
            assertThat(captured.teamId()).isNull();
            assertThat(captured.teamStatus()).isNull();
            assertThat(captured.statusCode()).isNull();
            assertThat(captured.importanceCode()).isNull();
            assertThat(captured.authorId()).isNull();
            assertThat(captured.tagId()).isNull();
            assertThat(captured.createdFrom()).isNull();
            assertThat(captured.page()).isEqualTo(1);
            assertThat(captured.pageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("keyword 의 좌우 공백은 trim 하고 빈 문자열은 null 로 정규화한다")
        void keyword_의_공백은_trim_되고_빈_문자열은_null_로_정규화된다() {
            // given
            CustomUserPrincipal principal = principal();
            WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

            given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
            given(worklogRepository.searchWorklogPage(eq(scope), any(WorklogSearchQuery.class)))
                    .willReturn(emptyPage());

            // when
            keywordWorklogSearchService.searchWorklogs(principal, requestWithKeyword("   "));
            keywordWorklogSearchService.searchWorklogs(principal, requestWithKeyword("  결산  "));

            // then
            ArgumentCaptor<WorklogSearchQuery> captor = ArgumentCaptor.forClass(WorklogSearchQuery.class);
            verify(worklogRepository, times(2)).searchWorklogPage(eq(scope), captor.capture());
            List<WorklogSearchQuery> captured = captor.getAllValues();

            assertThat(captured.get(0).keyword()).isNull();
            assertThat(captured.get(1).keyword()).isEqualTo("결산");
        }

        @Test
        @DisplayName("period 가 null 이면 createdFrom 도 null 이다")
        void period_가_null_이면_createdFrom_도_null_이다() {
            // given
            CustomUserPrincipal principal = principal();
            WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);

            given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
            given(worklogRepository.searchWorklogPage(eq(scope), any(WorklogSearchQuery.class)))
                    .willReturn(emptyPage());

            // when
            keywordWorklogSearchService.searchWorklogs(principal, requestWithKeyword(null));

            // then
            WorklogSearchQuery captured = captureQuery(scope);
            assertThat(captured.createdFrom()).isNull();
        }

        @Test
        @DisplayName("period 가 LAST_7 이면 createdFrom 은 today.minusDays(7) 이다")
        void period_가_LAST_7_이면_createdFrom_은_today_minusDays_7_이다() {
            // given
            CustomUserPrincipal principal = principal();
            WorklogVisibilityScope scope = new WorklogVisibilityScope.MyTeams(USER_ID);
            SearchWorklogsApiDto.Request request = new SearchWorklogsApiDto.Request(
                    null, null, null, null, null, null, null, PeriodOption.LAST_7, null, null
            );

            given(worklogVisibilityPolicy.resolve(principal)).willReturn(scope);
            given(worklogRepository.searchWorklogPage(eq(scope), any(WorklogSearchQuery.class)))
                    .willReturn(emptyPage());

            // when
            // service 내부 LocalDate.now() 와 같은 날 안에서 today 를 캡처한다.
            LocalDate today = LocalDate.now();
            keywordWorklogSearchService.searchWorklogs(principal, request);

            // then
            WorklogSearchQuery captured = captureQuery(scope);
            assertThat(captured.createdFrom()).isEqualTo(today.minusDays(7));
        }
    }

    private WorklogSearchQuery captureQuery(WorklogVisibilityScope scope) {
        ArgumentCaptor<WorklogSearchQuery> captor = ArgumentCaptor.forClass(WorklogSearchQuery.class);
        verify(worklogRepository).searchWorklogPage(eq(scope), captor.capture());
        return captor.getValue();
    }

    private static Page<WorklogSearchProjection> emptyPage() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    }

    private static SearchWorklogsApiDto.Request requestWithKeyword(String keyword) {
        return new SearchWorklogsApiDto.Request(
                keyword, null, null, null, null, null, null, null, null, null
        );
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
