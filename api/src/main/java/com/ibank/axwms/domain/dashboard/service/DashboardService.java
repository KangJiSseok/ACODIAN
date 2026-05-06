package com.ibank.axwms.domain.dashboard.service;

import com.ibank.axwms.domain.dashboard.DashboardScope;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.BlockedWorklog;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.CompletedInPeriod;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.DepartmentCompletionRate;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.DepartmentComparisonDashboard;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.DepartmentLoad;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.MyDashboard;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.PredecessorBrief;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.Progress;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.WorklogBrief;
import com.ibank.axwms.domain.dashboard.util.LoadBalanceIndex;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.AiOutcomeProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.BlockedPredecessorRowProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentLoadProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.ProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogBriefProjection;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LIST_LIMIT = 10;
    private static final int COMPLETED_PERIOD_DAYS = 30;
    private static final int WEEKLY_DAYS = 7;

    private final WorklogRepository worklogRepository;
    private final WorklogDependencyRepository worklogDependencyRepository;

    /**
     * scope 디스크리미네이터에 따라 4가지 응답 중 하나를 반환한다.
     * role 자체로 차단되는 경우 DASHBOARD_FORBIDDEN, 자원 권한 외이거나 미존재 시 DASHBOARD_NOT_FOUND.
     */
    public GetDashboardApiDto.Response getDashboard(CustomUserPrincipal principal,
                                                    GetDashboardApiDto.Request request) {
        DashboardScope scope = request.scope();
        return switch (scope) {
            case ME                    -> myDashboard(principal);
            case DEPARTMENT_COMPARISON -> {
                requireRole(principal, UserRole.DIRECTOR);
                yield departmentComparison();
            }
            case DEPARTMENT_DETAIL -> {
                requireRoleAtLeast(principal, UserRole.DEPT_HEAD);
                throw new UnsupportedOperationException("DEPARTMENT_DETAIL 은 다음 단계에서 구현됩니다.");
            }
            case TEAM_DETAIL -> {
                requireRoleAtLeast(principal, UserRole.TEAM_LEAD);
                throw new UnsupportedOperationException("TEAM_DETAIL 은 다음 단계에서 구현됩니다.");
            }
        };
    }

    // ===== ME =====

    private MyDashboard myDashboard(CustomUserPrincipal principal) {
        Long userId = principal.userId();
        LocalDate today = LocalDate.now();
        LocalDate periodFrom = today.minusDays(COMPLETED_PERIOD_DAYS);

        int inProgressCount = worklogRepository.countAuthorInProgress(userId);
        int completedCount  = worklogRepository.countAuthorCompletedSince(userId, periodFrom);
        int aiFailedCount   = worklogRepository.countAuthorAiFailed(userId);

        List<WorklogBrief> thisWeekDue = worklogRepository
                .findAuthorThisWeekDue(userId, today, LIST_LIMIT)
                .stream().map(p -> toBrief(p, today)).toList();

        List<WorklogBrief> todayItems = worklogRepository
                .findAuthorTodayItems(userId, LIST_LIMIT)
                .stream().map(p -> toBrief(p, today)).toList();

        List<WorklogBrief> imminentAndOverdue = worklogRepository
                .findAuthorImminentAndOverdue(userId, today, LIST_LIMIT)
                .stream().map(p -> toBrief(p, today)).toList();

        List<BlockedWorklog> blocked = blockedByPredecessors(userId);

        return new MyDashboard(
                inProgressCount,
                new CompletedInPeriod(periodFrom, today, completedCount),
                aiFailedCount,
                thisWeekDue,
                todayItems,
                imminentAndOverdue,
                blocked
        );
    }

    /**
     * (1) 내 미완료이면서 미완료 선행이 있는 worklog ID 를 limit 만큼 조회 (worklog repo).
     * (2) 그 ID 들의 미완료 선행 쌍을 한 번에 조회 (dependency repo).
     * (3) myWorklogId 로 그룹핑해 응답 형태로 가공. 쿼리 수: 2개 (limit 와 무관, N+1 없음).
     */
    private List<BlockedWorklog> blockedByPredecessors(Long userId) {
        List<Long> myIds = worklogRepository.findIncompleteAuthorWorklogIdsBlockedByPredecessor(userId, LIST_LIMIT);
        if (myIds.isEmpty()) {
            return List.of();
        }
        List<BlockedPredecessorRowProjection> rows =
                worklogDependencyRepository.findIncompletePredecessorsByWorklogIds(myIds);

        Map<Long, BlockedWorklog> indexed = new LinkedHashMap<>();
        for (Long myId : myIds) {
            indexed.put(myId, null);
        }
        for (BlockedPredecessorRowProjection row : rows) {
            BlockedWorklog existing = indexed.get(row.myWorklogId());
            if (existing == null) {
                List<PredecessorBrief> preds = new ArrayList<>();
                preds.add(new PredecessorBrief(
                        row.predecessorWorklogId(), row.predecessorTitle(), row.predecessorStatusCode()));
                indexed.put(row.myWorklogId(),
                        new BlockedWorklog(row.myWorklogId(), row.myTitle(), preds));
            } else {
                existing.predecessors().add(new PredecessorBrief(
                        row.predecessorWorklogId(), row.predecessorTitle(), row.predecessorStatusCode()));
            }
        }

        return indexed.values().stream().filter(v -> v != null).toList();
    }

    /**
     * projection → 응답 DTO 변환. ME 위젯의 projection 은 author/team/department 가 모두 null 이라
     * DTO 의 "부서/전사 위젯에서만 채움" 정책이 자연스럽게 유지된다 (WorklogBriefProjection.from 참고).
     * daysOverdue 는 due_date 가 오늘 이전일 때만 채워지고, NULL due_date 인 경우 null 로 남는다.
     */
    private WorklogBrief toBrief(WorklogBriefProjection p, LocalDate today) {
        Integer daysOverdue = null;
        if (p.dueDate() != null && p.dueDate().isBefore(today)) {
            daysOverdue = (int) ChronoUnit.DAYS.between(p.dueDate(), today);
        }
        return new WorklogBrief(p.worklogId(), p.title(), p.statusCode(),
                p.dueDate(), daysOverdue,
                p.teamName(), p.departmentName(), p.authorName());
    }

    // ===== DEPARTMENT_COMPARISON =====

    /**
     * DIRECTOR 전용 전사 비교 대시보드. 7개 위젯을 6개의 독립 SELECT 로 조립한다
     * (전사 진행/주간완료/AI성공률/부서완료율/부서부하/임박지연 + 클라이언트 표시용 부하편중지수).
     * 부서 baseline 으로 LEFT JOIN 하므로 worklog 가 0 건인 부서도 그래프와 Gini 입력에 포함된다.
     * 클래스 레벨의 readOnly 트랜잭션 안에서 실행되어 6개 쿼리 사이의 read-view 일관성이 보장된다.
     */
    private DepartmentComparisonDashboard departmentComparison() {
        LocalDate today = LocalDate.now();
        LocalDate weekFrom = today.minusDays(WEEKLY_DAYS);

        ProgressProjection orgProgressRow = worklogRepository.aggregateOrgProgress();
        Progress totalProgress = toProgress(orgProgressRow);

        int weeklyCompleted = worklogRepository.countOrgCompletedSince(weekFrom);

        AiOutcomeProjection aiRow = worklogRepository.aggregateOrgAiOutcome();
        double aiSuccessRate = aiSuccessRate(aiRow);

        List<DepartmentProgressProjection> deptProgressRows =
                worklogRepository.findDepartmentCompletionRates();
        List<DepartmentCompletionRate> departmentCompletionRates = deptProgressRows.stream()
                .map(this::toDepartmentCompletionRate)
                .toList();

        List<DepartmentLoadProjection> deptLoadRows = worklogRepository.findDepartmentWorkloads();
        double departmentLoadBalanceIndex = LoadBalanceIndex.balance(
                deptLoadRows.stream().mapToInt(DepartmentLoadProjection::activeWorklogCount).toArray());
        List<DepartmentLoad> departmentWorkload = deptLoadRows.stream()
                .map(r -> new DepartmentLoad(r.departmentId(), r.departmentName(), r.activeWorklogCount()))
                .toList();

        List<WorklogBrief> imminentAndOverdue = worklogRepository
                .findOrgImminentAndOverdue(today, LIST_LIMIT)
                .stream().map(p -> toBrief(p, today)).toList();

        return new DepartmentComparisonDashboard(
                totalProgress,
                departmentLoadBalanceIndex,
                weeklyCompleted,
                aiSuccessRate,
                departmentCompletionRates,
                departmentWorkload,
                imminentAndOverdue
        );
    }

    /** worklog 가 0 건인 부서도 LEFT JOIN 으로 행에 포함되므로 total=0 인 행은 rate=0.0 으로 안전하게 표현. */
    private DepartmentCompletionRate toDepartmentCompletionRate(DepartmentProgressProjection r) {
        double rate = r.total() == 0 ? 0.0 : (double) r.completed() / r.total();
        return new DepartmentCompletionRate(r.departmentId(), r.departmentName(),
                r.completed(), r.total(), rate);
    }

    /** total 의 정의는 is_deleted=false 인 모든 status 의 worklog (CANCELLED 등 포함). 분모 0 시 0.0. */
    private Progress toProgress(ProgressProjection row) {
        int completed = row.completed();
        int total = row.total();
        double rate = total == 0 ? 0.0 : (double) completed / total;
        return new Progress(completed, total, rate);
    }

    /**
     * AI 성공률의 분모는 (COMPLETED + FAILED) 즉 처리 시도가 끝난 것만 본다.
     * PENDING/PROCESSING 같은 미처리 worklog 는 의도적으로 분모에서 제외 — 처리 종료 시점 기준 성공률을 의미.
     */
    private double aiSuccessRate(AiOutcomeProjection row) {
        int processed = row.success() + row.failed();
        return processed == 0 ? 0.0 : (double) row.success() / processed;
    }

    // ===== 권한 검증 =====

    private void requireRole(CustomUserPrincipal principal, UserRole required) {
        UserRole actual = parseRole(principal);
        if (actual != required) {
            throw new BusinessException(ErrorCode.DASHBOARD_FORBIDDEN);
        }
    }

    private void requireRoleAtLeast(CustomUserPrincipal principal, UserRole minimum) {
        UserRole actual = parseRole(principal);
        // ordinal 작을수록 상위 (DIRECTOR=0, MEMBER=3). 상위 또는 동일이면 통과.
        if (actual.ordinal() > minimum.ordinal()) {
            throw new BusinessException(ErrorCode.DASHBOARD_FORBIDDEN);
        }
    }

    private UserRole parseRole(CustomUserPrincipal principal) {
        try {
            return UserRole.valueOf(principal.roleCode());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }
}
