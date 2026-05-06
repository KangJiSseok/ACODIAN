package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.AiOutcomeProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentLoadProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.ProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogBriefProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDetailProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogPageQuery;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorklogJooqRepository {

    Page<WorklogListProjection> findWorklogPage(Long userId, WorklogPageQuery worklogPageQuery);

    Optional<WorklogDetailProjection> findWorklogDetail(Long userId, Long worklogId);

    /** 가시 범위와 정규화된 검색 query 로 업무일지 페이지를 조회한다. */
    Page<WorklogSearchProjection> searchWorklogPage(WorklogVisibilityScope scope, WorklogSearchQuery query);

    // ----- 대시보드 ME 위젯용 -----

    /** author 의 IN_PROGRESS worklog 수. */
    int countAuthorInProgress(Long authorId);

    /** author 의 COMPLETED worklog 중 completion_date >= from 인 것의 수. */
    int countAuthorCompletedSince(Long authorId, LocalDate from);

    /** author 의 ai_processing_status = FAILED worklog 수. */
    int countAuthorAiFailed(Long authorId);

    /** author, 미완료, due_date 가 today..today+7. 마감 가까운 순으로 limit 만큼. */
    List<WorklogBriefProjection> findAuthorThisWeekDue(Long authorId, LocalDate today, int limit);

    /** author, 미완료. 진행중 우선, 그 다음 마감 가까운 순. limit 만큼. */
    List<WorklogBriefProjection> findAuthorTodayItems(Long authorId, int limit);

    /** author, 미완료, due_date <= today+3 (지연 포함). 마감 가까운 순으로 limit 만큼. */
    List<WorklogBriefProjection> findAuthorImminentAndOverdue(Long authorId, LocalDate today, int limit);

    /**
     * author, 미완료이면서 미완료 선행 worklog 가 하나 이상 있는 worklog 의 ID 들. limit 만큼.
     * 선행 쌍 자체는 WorklogDependencyJooqRepository.findIncompletePredecessorsByWorklogIds 로 조회한다.
     */
    List<Long> findIncompleteAuthorWorklogIdsBlockedByPredecessor(Long authorId, int limit);

    /**
     * 시맨틱 검색 결과 ID 목록을 기존 검색 응답 projection 으로 재조회한다.
     */
    List<WorklogSearchProjection> findSearchWorklogsByIds(WorklogVisibilityScope scope, List<Long> worklogIds);

    /**
     * AI 서버에 넘길 수 있도록 현재 검색 가시 범위의 팀 ID 목록만 조회한다.
     */
    List<Long> findVisibleTeamIds(WorklogVisibilityScope scope);

    // ----- 대시보드 DEPARTMENT_COMPARISON 위젯용 -----

    /** 전사 (완료, 전체) worklog 카운트. */
    ProgressProjection aggregateOrgProgress();

    /** 전사 COMPLETED worklog 중 completion_date >= from 인 것의 수. */
    int countOrgCompletedSince(LocalDate from);

    /** 전사 AI 처리 결과 (COMPLETED, FAILED) 카운트. */
    AiOutcomeProjection aggregateOrgAiOutcome();

    /**
     * 부서별 진행 현황. 부서는 worklog 의 작성자(author) 부서 기준으로 집계한다.
     * worklog 가 0 건인 부서도 행에 포함된다 (LEFT JOIN, 그래프 누락 방지).
     */
    List<DepartmentProgressProjection> findDepartmentCompletionRates();

    /**
     * 부서별 활성(미완료) worklog 수. 작성자 부서 기준.
     * worklog 가 0 건인 부서도 행에 포함된다 (Gini 계산 정확성 위해).
     */
    List<DepartmentLoadProjection> findDepartmentWorkloads();

    /** 전사 마감 임박/지연 (D-3 이내, 미완료) 위젯용. 작성자/팀/부서 이름까지 함께. */
    List<WorklogBriefProjection> findOrgImminentAndOverdue(LocalDate today, int limit);
}
