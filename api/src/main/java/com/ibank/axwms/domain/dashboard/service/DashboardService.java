package com.ibank.axwms.domain.dashboard.service;

import com.ibank.axwms.domain.dashboard.DashboardScope;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
            case ME -> myDashboard(principal);
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

    private GetDashboardApiDto.MyDashboard myDashboard(CustomUserPrincipal principal) {
        Long userId = principal.userId();
        LocalDate today = LocalDate.now();
        LocalDate periodFrom = today.minusDays(COMPLETED_PERIOD_DAYS);

        return GetDashboardApiDto.MyDashboard.of(
                worklogRepository.countAuthorInProgress(userId),
                periodFrom, today, worklogRepository.countAuthorCompletedSince(userId, periodFrom),
                worklogRepository.countAuthorAiFailed(userId),
                worklogRepository.findAuthorThisWeekDue(userId, today, LIST_LIMIT),
                worklogRepository.findAuthorTodayItems(userId, LIST_LIMIT),
                worklogRepository.findAuthorImminentAndOverdue(userId, today, LIST_LIMIT),
                blockedByPredecessors(userId),
                today
        );
    }

    /**
     * 두 repository (worklog, dependency) 호출을 묶어 BlockedWorklog 그룹핑 입력을 준비한다.
     * grouping 과 응답 record 조립은 BlockedWorklog.fromRows 가 담당.
     * 쿼리 수: 2개 (limit 와 무관, N+1 없음).
     */
    private List<GetDashboardApiDto.BlockedWorklog> blockedByPredecessors(Long userId) {
        List<Long> myIds = worklogRepository.findIncompleteAuthorWorklogIdsBlockedByPredecessor(userId, LIST_LIMIT);
        if (myIds.isEmpty()) {
            return List.of();
        }
        return GetDashboardApiDto.BlockedWorklog.fromRows(
                myIds,
                worklogDependencyRepository.findIncompletePredecessorsByWorklogIds(myIds)
        );
    }

    // ===== DEPARTMENT_COMPARISON =====

    /**
     * DIRECTOR 전용 전사 비교 대시보드. 6개 raw 결과를 모아 DTO 의 of() 에 넘기면
     * 부하 편중 지수/AI 성공률 계산과 nested record 변환이 모두 DTO 안에서 수행된다.
     * 클래스 레벨 readOnly 트랜잭션 안에서 실행되어 6개 쿼리 사이의 read-view 일관성이 보장된다.
     */
    private GetDashboardApiDto.DepartmentComparisonDashboard departmentComparison() {
        LocalDate today = LocalDate.now();
        LocalDate weekFrom = today.minusDays(WEEKLY_DAYS);

        return GetDashboardApiDto.DepartmentComparisonDashboard.of(
                worklogRepository.aggregateOrgProgress(),
                worklogRepository.countOrgCompletedSince(weekFrom),
                worklogRepository.aggregateOrgAiOutcome(),
                worklogRepository.findDepartmentCompletionRates(),
                worklogRepository.findDepartmentWorkloads(),
                worklogRepository.findOrgImminentAndOverdue(today, LIST_LIMIT),
                today
        );
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
