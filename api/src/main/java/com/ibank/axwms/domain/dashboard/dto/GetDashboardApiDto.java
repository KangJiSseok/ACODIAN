package com.ibank.axwms.domain.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ibank.axwms.domain.dashboard.DashboardScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetDashboardApiDto {

    @Schema(description = "대시보드 조회 요청 DTO")
    public record Request(
            @Schema(description = "조회 범위", example = "ME")
            @NotNull(message = "scope 는 필수입니다.")
            DashboardScope scope,
            @Schema(description = "scope=DEPARTMENT_DETAIL 일 때 필수", example = "1")
            Long departmentId,
            @Schema(description = "scope=TEAM_DETAIL 일 때 필수", example = "21")
            Long teamId
    ) {}

    /**
     * 대시보드 응답. scope 디스크리미네이터로 4가지 타입 중 하나가 직렬화된다.
     * Jackson 다형성 직렬화를 위해 @JsonTypeInfo / @JsonSubTypes 를 부착했고,
     * sealed interface 로 미허가 구현체 추가를 막는다.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "scope")
    @JsonSubTypes({
            @Type(value = MyDashboard.class,                   name = "ME"),
            @Type(value = DepartmentComparisonDashboard.class, name = "DEPARTMENT_COMPARISON"),
            @Type(value = DepartmentDetailDashboard.class,     name = "DEPARTMENT_DETAIL"),
            @Type(value = TeamDetailDashboard.class,           name = "TEAM_DETAIL")
    })
    @Schema(description = "대시보드 응답 (scope 디스크리미네이터로 4가지 타입 중 하나)")
    public sealed interface Response permits
            MyDashboard, DepartmentComparisonDashboard,
            DepartmentDetailDashboard, TeamDetailDashboard {}

    @Schema(description = "내 대시보드 응답")
    public record MyDashboard(
            @Schema(description = "현재 진행 중인 내 업무 수", example = "5")
            int inProgressCount,
            @Schema(description = "기간 내 완료된 내 업무 (최근 30일)")
            CompletedInPeriod completedInPeriod,
            @Schema(description = "AI 처리 실패한 내 업무 수", example = "2")
            int aiFailedCount,
            @Schema(description = "이번 주 마감 (D-7 이내, 미완료, 최대 10건)")
            List<WorklogBrief> thisWeekDue,
            @Schema(description = "오늘의 업무 (진행중 우선, 마감 가까운 순, 최대 10건)")
            List<WorklogBrief> todayItems,
            @Schema(description = "마감 임박 및 지연 (D-3 이내 또는 지연, 최대 10건)")
            List<WorklogBrief> imminentAndOverdue,
            @Schema(description = "선행 업무 대기 (미완료 선행이 있는 내 업무, 최대 10건)")
            List<BlockedWorklog> blockedByPredecessors
    ) implements Response {}

    @Schema(description = "전체 부서 비교 대시보드 응답 (DIRECTOR 전용)")
    public record DepartmentComparisonDashboard(
            @Schema(description = "전체 진행 현황")
            Progress totalProgress,
            @Schema(description = "부서 부하 편중 지수 (1-Gini, 1 에 가까울수록 균형)", example = "0.82")
            double departmentLoadBalanceIndex,
            @Schema(description = "최근 7일 완료된 업무 수", example = "38")
            int weeklyCompleted,
            @Schema(description = "AI 파이프라인 성공률", example = "0.91")
            double aiPipelineSuccessRate,
            @Schema(description = "부서별 완료율 (그래프용)")
            List<DepartmentCompletionRate> departmentCompletionRates,
            @Schema(description = "부서별 활성 업무 부하")
            List<DepartmentLoad> departmentWorkload,
            @Schema(description = "마감 임박 및 지연 (D-3 이내 또는 지연, 최대 10건)")
            List<WorklogBrief> imminentAndOverdue
    ) implements Response {}

    @Schema(description = "단일 부서 상세 대시보드 응답")
    public record DepartmentDetailDashboard(
            @Schema(description = "부서 ID", example = "1")
            Long departmentId,
            @Schema(description = "부서명", example = "물류본부")
            String departmentName,
            @Schema(description = "부서 전체 진행 현황")
            Progress totalProgress,
            @Schema(description = "팀 부하 편중 지수 (1-Gini)", example = "0.78")
            double teamLoadBalanceIndex,
            @Schema(description = "최근 7일 완료된 업무 수", example = "12")
            int weeklyCompleted,
            @Schema(description = "AI 파이프라인 성공률", example = "0.95")
            double aiPipelineSuccessRate,
            @Schema(description = "팀별 완료율")
            List<TeamCompletionRate> teamCompletionRates,
            @Schema(description = "팀별 활성 업무 부하")
            List<TeamLoad> teamWorkload,
            @Schema(description = "마감 임박 및 지연 (D-3 이내 또는 지연, 최대 10건)")
            List<WorklogBrief> imminentAndOverdue
    ) implements Response {}

    @Schema(description = "단일 팀 상세 대시보드 응답")
    public record TeamDetailDashboard(
            @Schema(description = "팀 ID", example = "21")
            Long teamId,
            @Schema(description = "팀명", example = "물류혁신TF")
            String teamName,
            @Schema(description = "팀 전체 진행 현황")
            Progress totalProgress,
            @Schema(description = "팀원 부하 편중 지수 (1-Gini)", example = "0.85")
            double memberLoadBalanceIndex,
            @Schema(description = "최근 7일 완료된 업무 수", example = "8")
            int weeklyCompleted,
            @Schema(description = "AI 파이프라인 성공률", example = "0.92")
            double aiPipelineSuccessRate,
            @Schema(description = "팀 전체 완료율 (= totalProgress.rate)", example = "0.428")
            double completionRate,
            @Schema(description = "팀원별 활성 업무 부하")
            List<MemberLoad> memberWorkload,
            @Schema(description = "마감 임박 및 지연 (D-3 이내 또는 지연, 최대 10건)")
            List<WorklogBrief> imminentAndOverdue
    ) implements Response {}

    // ===== 공통/공유 record =====

    @Schema(description = "기간 내 완료 집계")
    public record CompletedInPeriod(
            LocalDate from,
            LocalDate to,
            int count
    ) {}

    @Schema(description = "진행 현황 (완료/전체/비율)")
    public record Progress(
            int completed,
            int total,
            double rate
    ) {}

    @Schema(description = "업무 간단 정보")
    public record WorklogBrief(
            Long worklogId,
            String title,
            String statusCode,
            LocalDate dueDate,
            @Schema(description = "마감일이 오늘 이전이면 며칠 지났는지. 미지연이면 null.", nullable = true)
            Integer daysOverdue,
            @Schema(description = "팀명. 부서/전사 위젯에서만 채워짐.", nullable = true)
            String teamName,
            @Schema(description = "부서명. 전사 비교 위젯에서만 채워짐.", nullable = true)
            String departmentName,
            @Schema(description = "작성자명. 부서/전사 위젯에서만 채워짐.", nullable = true)
            String authorName
    ) {}

    @Schema(description = "선행 업무에 막힌 내 업무")
    public record BlockedWorklog(
            Long worklogId,
            String title,
            List<PredecessorBrief> predecessors
    ) {}

    @Schema(description = "선행 업무 간단 정보")
    public record PredecessorBrief(
            Long worklogId,
            String title,
            String statusCode
    ) {}

    @Schema(description = "부서별 완료율")
    public record DepartmentCompletionRate(
            Long departmentId, String departmentName,
            int completed, int total, double rate
    ) {}

    @Schema(description = "부서별 활성 업무 부하")
    public record DepartmentLoad(
            Long departmentId, String departmentName, int activeWorklogCount
    ) {}

    @Schema(description = "팀별 완료율")
    public record TeamCompletionRate(
            Long teamId, String teamName,
            int completed, int total, double rate
    ) {}

    @Schema(description = "팀별 활성 업무 부하")
    public record TeamLoad(
            Long teamId, String teamName, int activeWorklogCount
    ) {}

    @Schema(description = "팀원별 활성 업무 부하")
    public record MemberLoad(
            Long userId, String userName, int activeWorklogCount
    ) {}
}
