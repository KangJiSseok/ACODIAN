package com.ibank.axwms.domain.dashboard.service;

import com.ibank.axwms.domain.dashboard.DashboardScope;
import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto;
import com.ibank.axwms.domain.organization.department.service.DepartmentService;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.service.UserService;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DashboardScopeSummaryProjection;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LIST_LIMIT = 10;
    private static final int COMPLETED_PERIOD_DAYS = 30;
    private static final int WEEKLY_DAYS = 7;

    // 룰 없는 read-only 집계 — repo 직접 (dashboard 위젯 전용 쿼리, 다른 도메인이 쓸 일 없음).
    private final WorklogRepository worklogRepository;
    private final WorklogDependencyRepository worklogDependencyRepository;

    // 룰 있는 lookup/검증 — service 통과 (entity import 없이 service contract 만 의존).
    private final DepartmentService departmentService;
    private final UserService userService;
    private final TeamService teamService;

    /**
     * scope 디스크리미네이터에 따라 4가지 응답 중 하나를 반환한다.
     * role 자체로 차단되는 경우 DASHBOARD_FORBIDDEN, 자원 권한 외이거나 미존재 시 DASHBOARD_NOT_FOUND.
     */
    public GetDashboardApiDto.Response getDashboard(CustomUserPrincipal principal,
                                                    GetDashboardApiDto.Request request) {
        DashboardScope scope = request.scope();
        return switch (scope) {
            // ME 는 role 가드 없음 — 자기 데이터만 조회. 단 teamId 와 팀 멤버십은 service 안에서 검증.
            case ME -> myDashboard(principal, request);
            // 전사 비교는 director 전용 (exact match) — 더 상위 role 이 추가되면 명시적으로 허용 결정 필요.
            case DEPARTMENT_COMPARISON -> {
                requireRole(principal, UserRole.DIRECTOR);
                yield departmentComparison();
            }
            case DEPARTMENT_DETAIL -> {
                requireRoleAtLeast(principal, UserRole.DEPT_HEAD);
                yield departmentDetail(principal, request);
            }
            case TEAM_DETAIL -> {
                requireRoleAtLeast(principal, UserRole.TEAM_LEAD);
                yield teamDetail(principal, request);
            }
        };
    }

    // ===== ME =====

    /**
     * teamId 가 null 이거나 사용자가 그 팀의 ACTIVE 멤버가 아니면 DASHBOARD_NOT_FOUND.
     * 멤버 확인 후 (author = me) AND (team = teamId) 두 조건으로 좁혀 ME 위젯 데이터 조회.
     * 다중 팀 사용자는 team 별로 별도 호출 (request.teamId 변경) 해서 본다.
     */
    private GetDashboardApiDto.MyDashboard myDashboard(CustomUserPrincipal principal,
                                                       GetDashboardApiDto.Request request) {
        Long userId = principal.userId();
        Long teamId = request.teamId();
        // canAccessActiveTeam: ACTIVE 멤버이면서 팀 자체도 활성 — 두 조건 동시 검증.
        if (teamId == null || !teamService.canAccessActiveTeam(userId, teamId)) {
            throw new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();
        LocalDate periodFrom = today.minusDays(COMPLETED_PERIOD_DAYS);

        return GetDashboardApiDto.MyDashboard.of(
                worklogRepository.aggregateAuthorCounts(userId, teamId, periodFrom),
                periodFrom, today,
                worklogRepository.findAuthorThisWeekDue(userId, teamId, today, LIST_LIMIT),
                worklogRepository.findAuthorTodayItems(userId, teamId, LIST_LIMIT),
                worklogRepository.findAuthorImminentAndOverdue(userId, teamId, today, LIST_LIMIT),
                blockedByPredecessors(userId, teamId),
                today
        );
    }

    /**
     * 두 repository (worklog, dependency) 호출을 묶어 BlockedWorklog 그룹핑 입력을 준비한다.
     * 본인 worklog 만 (author + team) 으로 좁히고 선행 worklog 는 다른 팀일 수 있다.
     * grouping 과 응답 record 조립은 BlockedWorklog.fromRows 가 담당. 쿼리 수: 2개 (limit 와 무관, N+1 없음).
     */
    private List<GetDashboardApiDto.BlockedWorklog> blockedByPredecessors(Long userId, Long teamId) {
        List<Long> myIds = worklogRepository.findIncompleteAuthorWorklogIdsBlockedByPredecessor(userId, teamId, LIST_LIMIT);
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

        DashboardScopeSummaryProjection summary = worklogRepository.aggregateOrgSummary(weekFrom);
        return GetDashboardApiDto.DepartmentComparisonDashboard.of(
                summary.progress(),
                summary.weeklyCompleted(),
                summary.aiOutcome(),
                worklogRepository.findDepartmentCompletionRates(),
                worklogRepository.findDepartmentWorkloads(),
                worklogRepository.findOrgImminentAndOverdue(today, LIST_LIMIT),
                today
        );
    }

    // ===== DEPARTMENT_DETAIL =====

    /**
     * DEPT_HEAD 또는 DIRECTOR 가 단일 부서의 상세 대시보드를 조회한다.
     * 부서 매핑은 worklog → tb_team → tb_team.department_id 기준 (DEPARTMENT_COMPARISON 과 동일 정의).
     *
     * <p>가드 순서 — 모든 실패 케이스를 동일하게 DASHBOARD_NOT_FOUND 로 정규화해 자원 존재 여부 leak 을 막는다:
     * <ol>
     *   <li>request.departmentId 가 null 이면 NOT_FOUND</li>
     *   <li>활성(ACTIVE) 부서가 아니면 NOT_FOUND (없거나 INACTIVE)</li>
     *   <li>DEPT_HEAD 인 경우 자기 부서가 아니면 NOT_FOUND (DIRECTOR 는 모든 부서 통과)</li>
     * </ol>
     * 권한 검증을 통과한 후 6개 raw 결과를 모아 DTO 의 of() 가 부하 편중 지수/AI 성공률 계산과 nested 변환을 수행.
     */
    private GetDashboardApiDto.DepartmentDetailDashboard departmentDetail(CustomUserPrincipal principal,
                                                                          GetDashboardApiDto.Request request) {
        Long departmentId = request.departmentId();
        if (departmentId == null) {
            throw new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND);
        }
        String departmentName = departmentService.findActiveDepartmentName(departmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND));

        if (parseRole(principal) == UserRole.DEPT_HEAD
                && !Objects.equals(departmentId, userService.getDepartmentIdOrThrow(principal.userId()))) {
            throw new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();
        LocalDate weekFrom = today.minusDays(WEEKLY_DAYS);

        DashboardScopeSummaryProjection summary = worklogRepository.aggregateDeptSummary(departmentId, weekFrom);
        return GetDashboardApiDto.DepartmentDetailDashboard.of(
                departmentId,
                departmentName,
                summary.progress(),
                summary.weeklyCompleted(),
                summary.aiOutcome(),
                worklogRepository.findTeamCompletionRatesInDept(departmentId),
                worklogRepository.findTeamWorkloadsInDept(departmentId),
                worklogRepository.findDeptImminentAndOverdue(departmentId, today, LIST_LIMIT),
                today
        );
    }

    // ===== TEAM_DETAIL =====

    /**
     * TEAM_LEAD 이상이 단일 팀의 상세 대시보드를 조회한다.
     * 팀 매핑은 worklog.team_id 기준 (DEPARTMENT_DETAIL 와 정합).
     *
     * <p>가드 순서 — 모든 실패 케이스를 동일하게 DASHBOARD_NOT_FOUND 로 정규화 (보안 leak 방지):
     * <ol>
     *   <li>request.teamId 가 null 이면 NOT_FOUND</li>
     *   <li>활성(soft-delete 안 된) 팀이 아니면 NOT_FOUND</li>
     *   <li>TEAM_LEAD 인 경우 자기가 ACTIVE 멤버인 팀이 아니면 NOT_FOUND</li>
     *   <li>DEPT_HEAD 인 경우 그 팀의 부서가 자기 부서가 아니면 NOT_FOUND</li>
     *   <li>DIRECTOR 는 모든 팀 통과</li>
     * </ol>
     * 권한 검증을 통과한 후 5개 raw 결과를 모아 DTO 의 of() 가 부하 편중 지수/AI 성공률 계산과 nested 변환을 수행.
     */
    private GetDashboardApiDto.TeamDetailDashboard teamDetail(CustomUserPrincipal principal,
                                                              GetDashboardApiDto.Request request) {
        Long teamId = request.teamId();
        if (teamId == null) {
            throw new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND);
        }
        String teamName = teamService.findActiveTeamName(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND));

        UserRole role = parseRole(principal);
        if (role == UserRole.TEAM_LEAD) {
            // TEAM_LEAD 는 본인이 ACTIVE 멤버인 팀만 — 다른 팀 탐색 차단
            if (!teamService.isMember(principal.userId(), teamId)) {
                throw new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND);
            }
        } else if (role == UserRole.DEPT_HEAD) {
            // DEPT_HEAD 는 본인 부서 소속 팀만
            Long teamDeptId = teamService.findActiveTeamDepartmentId(teamId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND));
            if (!Objects.equals(teamDeptId, userService.getDepartmentIdOrThrow(principal.userId()))) {
                throw new BusinessException(ErrorCode.DASHBOARD_NOT_FOUND);
            }
        }
        // DIRECTOR 는 모든 팀 통과

        LocalDate today = LocalDate.now();
        LocalDate weekFrom = today.minusDays(WEEKLY_DAYS);

        DashboardScopeSummaryProjection summary = worklogRepository.aggregateTeamSummary(teamId, weekFrom);
        return GetDashboardApiDto.TeamDetailDashboard.of(
                teamId,
                teamName,
                summary.progress(),
                summary.weeklyCompleted(),
                summary.aiOutcome(),
                worklogRepository.findMemberWorkloadsInTeam(teamId),
                worklogRepository.findTeamImminentAndOverdue(teamId, today, LIST_LIMIT),
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
