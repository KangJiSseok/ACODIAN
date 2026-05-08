package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.AiOutcomeProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.AuthorCountSummaryProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentLoadProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.ProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.TeamLoadProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.TeamProgressProjection;
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
    // 모든 ME 쿼리는 (author = me) AND (team = teamId) 두 조건으로 좁힌다 — 다중 팀 사용자가
    // 특정 팀 컨텍스트 안에서만 자기 worklog 를 보도록 하는 정책. teamId 멤버십 검증은 service 책임.

    /**
     * author 의 ME 카운트 위젯 3종 (in_progress / 기간 내 완료 / AI 실패) 을 한 번의 SELECT 로 동시 집계한다.
     * 같은 (author + team + is_deleted=false) 조건 위에서 CASE WHEN 으로 갈라 round-trip 3회 → 1회로 줄임.
     */
    AuthorCountSummaryProjection aggregateAuthorCounts(Long authorId, Long teamId, LocalDate completedFrom);

    /** author + team, 미완료, due_date 가 today..today+7. 마감 가까운 순으로 limit 만큼. */
    List<WorklogBriefProjection> findAuthorThisWeekDue(Long authorId, Long teamId, LocalDate today, int limit);

    /** author + team, 미완료. 진행중 우선, 그 다음 마감 가까운 순. limit 만큼. */
    List<WorklogBriefProjection> findAuthorTodayItems(Long authorId, Long teamId, int limit);

    /** author + team, 미완료, due_date <= today+3 (지연 포함). 마감 가까운 순으로 limit 만큼. */
    List<WorklogBriefProjection> findAuthorImminentAndOverdue(Long authorId, Long teamId, LocalDate today, int limit);

    /**
     * author + team, 미완료이면서 미완료 선행 worklog 가 하나 이상 있는 worklog 의 ID 들. limit 만큼.
     * 선행 worklog 는 다른 팀일 수도 있음 — 본인 worklog 만 team 으로 좁히고 선행 쪽 제약은 없음.
     * 선행 쌍 자체는 WorklogDependencyJooqRepository.findIncompletePredecessorsByWorklogIds 로 조회한다.
     */
    List<Long> findIncompleteAuthorWorklogIdsBlockedByPredecessor(Long authorId, Long teamId, int limit);

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
     * 부서별 진행 현황. 부서 매핑은 worklog → tb_team → tb_team.department_id 기준
     * (DEPARTMENT_DETAIL 과 동일 정의 — "이 부서가 소유한 팀의 worklog").
     * worklog 가 0 건인 부서도 행에 포함된다 (LEFT JOIN, 그래프 누락 방지). 삭제 팀은 제외.
     */
    List<DepartmentProgressProjection> findDepartmentCompletionRates();

    /**
     * 부서별 활성(미완료) worklog 수. 부서 매핑은 worklog → tb_team → tb_team.department_id 기준.
     * worklog 가 0 건인 부서도 행에 포함된다 (Gini 계산 정확성 위해). 삭제 팀은 제외.
     */
    List<DepartmentLoadProjection> findDepartmentWorkloads();

    /**
     * 전사 마감 임박/지연 (D-3 이내, 미완료) 위젯용. 작성자/팀/부서 이름까지 함께.
     * 부서 표시는 team.department_id 기준 (위 부서별 위젯과 같은 매핑이라 합이 맞는다).
     */
    List<WorklogBriefProjection> findOrgImminentAndOverdue(LocalDate today, int limit);

    // ----- 대시보드 DEPARTMENT_DETAIL 위젯용 -----
    // 부서 매핑은 worklog.team_id → tb_team.department_id 기준 (전사 비교의 author 기반과 다름).
    // "이 부서가 직접 소유한 팀의 worklog" 라는 의미이며, GET /departments/{id}/detail 의 ownership 정의와 일치한다.

    /** 부서 소속 팀들의 worklog (완료, 전체) 카운트. */
    ProgressProjection aggregateDeptProgress(Long departmentId);

    /** 부서 소속 팀들의 COMPLETED worklog 중 completion_date >= from 인 것의 수. */
    int countDeptCompletedSince(Long departmentId, LocalDate from);

    /** 부서 소속 팀들의 AI 처리 결과 (COMPLETED, FAILED) 카운트. */
    AiOutcomeProjection aggregateDeptAiOutcome(Long departmentId);

    /**
     * 부서 소속 팀별 진행 현황. worklog 가 0 건인 팀도 행에 포함된다 (LEFT JOIN, 그래프 누락 방지).
     * 삭제된 팀은 제외.
     */
    List<TeamProgressProjection> findTeamCompletionRatesInDept(Long departmentId);

    /**
     * 부서 소속 팀별 활성(미완료) worklog 수. worklog 가 0 건인 팀도 행에 포함된다 (Gini 계산 정확성 위해).
     * 삭제된 팀은 제외.
     */
    List<TeamLoadProjection> findTeamWorkloadsInDept(Long departmentId);

    /** 부서 소속 팀들의 마감 임박/지연 (D-3 이내, 미완료) 위젯용. 작성자/팀/부서 이름까지 함께. */
    List<WorklogBriefProjection> findDeptImminentAndOverdue(Long departmentId, LocalDate today, int limit);
}
