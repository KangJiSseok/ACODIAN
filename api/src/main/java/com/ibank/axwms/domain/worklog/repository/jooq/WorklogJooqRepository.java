package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
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
}
