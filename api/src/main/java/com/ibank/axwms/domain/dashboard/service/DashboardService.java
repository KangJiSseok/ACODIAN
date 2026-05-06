package com.ibank.axwms.domain.dashboard.service;

import com.ibank.axwms.domain.dashboard.DashboardScope;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.BlockedWorklog;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.CompletedInPeriod;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.MyDashboard;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.PredecessorBrief;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto.WorklogBrief;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.BlockedPredecessorRowProjection;
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
                throw new UnsupportedOperationException("DEPARTMENT_COMPARISON 은 다음 단계에서 구현됩니다.");
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

    private WorklogBrief toBrief(WorklogBriefProjection p, LocalDate today) {
        Integer daysOverdue = null;
        if (p.dueDate() != null && p.dueDate().isBefore(today)) {
            daysOverdue = (int) ChronoUnit.DAYS.between(p.dueDate(), today);
        }
        return new WorklogBrief(p.worklogId(), p.title(), p.statusCode(),
                p.dueDate(), daysOverdue,
                null, null, null);
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
