"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import {
  Activity,
  AlertTriangle,
  BarChart3,
  BrainCircuit,
  CalendarClock,
  CheckCircle2,
  Gauge,
  ListChecks,
  type LucideIcon,
  TrendingUp,
} from "lucide-react";
import { Pagination } from "@/app/_common/components/data-display/pagination";
import { usePagination } from "@/app/_common/hooks/usePagination";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { cn } from "@/lib/utils";
import type {
  DashboardBlockedWorklog,
  DashboardProgress,
  DashboardWorklogBrief,
  DepartmentDashboard,
  DirectorDashboard,
  MyDashboard,
  TeamDashboard,
} from "../_types/dashboard.types";
import {
  clampRate,
  formatCount,
  formatDueDate,
  formatIndex,
  formatOverdueDays,
  formatPercent,
} from "../_utils/dashboardFormat";

const IMMINENT_WORKLOG_PAGE_SIZE = 3;
const TODAY_WORKLOG_PAGE_SIZE = 3;

interface CompletionItem {
  id: number;
  href: string;
  label: string;
  completed: number;
  total: number;
  rate: number;
}

interface WorkloadItem {
  id: number;
  href: string;
  label: string;
  activeWorklogCount: number;
}

interface ComparisonDashboardViewModel {
  totalProgress: DashboardProgress;
  loadBalanceIndex: number;
  loadMetricLabel: string;
  loadMetricDetail: string;
  weeklyCompleted: number;
  weeklyMetricDetail: string;
  aiPipelineSuccessRate: number;
  aiMetricDetail: string;
  completionTitle: string;
  completionDescription: string;
  completionCountLabel: string;
  completionItems: CompletionItem[];
  workloadTitle: string;
  workloadDescription: string;
  workloadCountLabel: string;
  workloadItems: WorkloadItem[];
  imminentDescription: string;
  imminentAndOverdue: DashboardWorklogBrief[];
  showWorklogContext?: boolean;
}

export function DirectorDashboardView({
  dashboard,
}: {
  dashboard: DirectorDashboard;
}) {
  return (
    <ComparisonDashboardView
      viewModel={{
        totalProgress: dashboard.totalProgress,
        loadBalanceIndex: dashboard.departmentLoadBalanceIndex,
        // 전체 부서 비교에서는 부서 간 업무량 분산도를 같은 카드 위치에 표시합니다.
        loadMetricLabel: "부서 부하 편중 지수",
        loadMetricDetail: "부서 간 업무량 분산도, 1에 가까울수록 균형",
        weeklyCompleted: dashboard.weeklyCompleted,
        weeklyMetricDetail: "전체 부서 비교 최근 7일 완료 산출",
        aiPipelineSuccessRate: dashboard.aiPipelineSuccessRate,
        aiMetricDetail: "전체 부서 비교 업무·파일 통합",
        completionTitle: "부서별 완료율 비교",
        completionDescription: "부서별 완료 업무 비중을 비교합니다.",
        completionCountLabel: `${dashboard.departmentCompletionRates.length}개 부서`,
        completionItems: dashboard.departmentCompletionRates.map((item) => ({
          id: item.departmentId,
          href: `/department/detail/${item.departmentId}`,
          label: item.departmentName,
          completed: item.completed,
          total: item.total,
          rate: item.rate,
        })),
        workloadTitle: "부서별 업무 부하",
        workloadDescription: "완료되지 않은 업무량의 분포를 확인합니다.",
        workloadCountLabel: `${dashboard.departmentWorkload.length}개 부서`,
        workloadItems: dashboard.departmentWorkload.map((item) => ({
          id: item.departmentId,
          href: `/department/detail/${item.departmentId}`,
          label: item.departmentName,
          activeWorklogCount: item.activeWorklogCount,
        })),
        imminentDescription:
          "관리자 뷰와 동일한 D-3 이내 + 지연 기준입니다.",
        // <ImminentWorklogPanel items={dashboard.imminentAndOverdue} />를 scope별 viewModel로 일반화합니다.
        imminentAndOverdue: dashboard.imminentAndOverdue,
      }}
    />
  );
}

export function DepartmentDashboardView({
  dashboard,
}: {
  dashboard: DepartmentDashboard;
}) {
  return (
    <ComparisonDashboardView
      viewModel={{
        totalProgress: dashboard.totalProgress,
        loadBalanceIndex: dashboard.teamLoadBalanceIndex,
        loadMetricLabel: "팀 부하 편중 지수",
        loadMetricDetail: "팀 간 업무량 분산도, 1에 가까울수록 균형",
        weeklyCompleted: dashboard.weeklyCompleted,
        weeklyMetricDetail: `${dashboard.departmentName} 최근 7일 완료 산출`,
        aiPipelineSuccessRate: dashboard.aiPipelineSuccessRate,
        aiMetricDetail: `${dashboard.departmentName} 업무·파일 통합`,
        completionTitle: "팀별 완료율 비교",
        completionDescription: "선택 부서 안에서 팀별 완료 업무 비중을 비교합니다.",
        completionCountLabel: `${dashboard.teamCompletionRates.length}개 팀`,
        completionItems: dashboard.teamCompletionRates.map((item) => ({
          id: item.teamId,
          href: `/team/detail/${item.teamId}`,
          label: item.teamName,
          completed: item.completed,
          total: item.total,
          rate: item.rate,
        })),
        workloadTitle: "팀별 업무 부하",
        workloadDescription: "선택 부서 안에서 완료되지 않은 업무량을 확인합니다.",
        workloadCountLabel: `${dashboard.teamWorkload.length}개 팀`,
        workloadItems: dashboard.teamWorkload.map((item) => ({
          id: item.teamId,
          href: `/team/detail/${item.teamId}`,
          label: item.teamName,
          activeWorklogCount: item.activeWorklogCount,
        })),
        imminentDescription:
          "선택 부서 기준 D-3 이내 또는 지연된 미완료 업무입니다.",
        imminentAndOverdue: dashboard.imminentAndOverdue,
      }}
    />
  );
}

export function TeamDashboardView({ dashboard }: { dashboard: TeamDashboard }) {
  return (
    <ComparisonDashboardView
      viewModel={{
        totalProgress: dashboard.totalProgress,
        loadBalanceIndex: dashboard.memberLoadBalanceIndex,
        loadMetricLabel: "팀원 부하 편중 지수",
        loadMetricDetail: "팀원 간 업무량 분산도, 1에 가까울수록 균형",
        weeklyCompleted: dashboard.weeklyCompleted,
        weeklyMetricDetail: `${dashboard.teamName} 최근 7일 완료 산출`,
        aiPipelineSuccessRate: dashboard.aiPipelineSuccessRate,
        aiMetricDetail: `${dashboard.teamName} 업무·파일 통합`,
        completionTitle: "팀 완료율",
        completionDescription: "선택 팀의 전체 완료 업무 비중을 확인합니다.",
        completionCountLabel: "1개 팀",
        completionItems: [
          {
            id: dashboard.teamId,
            href: `/team/detail/${dashboard.teamId}`,
            label: dashboard.teamName,
            completed: dashboard.totalProgress.completed,
            total: dashboard.totalProgress.total,
            rate: dashboard.completionRate,
          },
        ],
        workloadTitle: "팀원별 업무 부하",
        workloadDescription: "선택 팀 안에서 완료되지 않은 업무량 분포를 확인합니다.",
        workloadCountLabel: `${dashboard.memberWorkload.length}명`,
        workloadItems: dashboard.memberWorkload.map((item) => ({
          id: item.userId,
          href: `/user/detail/${item.userId}`,
          label: item.userName,
          activeWorklogCount: item.activeWorklogCount,
        })),
        imminentDescription:
          "선택 팀 기준 D-3 이내 또는 지연된 미완료 업무입니다.",
        imminentAndOverdue: dashboard.imminentAndOverdue,
        showWorklogContext: false,
      }}
    />
  );
}

export function MyDashboardView({ dashboard }: { dashboard: MyDashboard }) {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={Gauge}
          label="진행 중인 내 업무"
          value={`${formatCount(dashboard.inProgressCount)}건`}
          detail="선택한 소속 팀 기준 진행 중 업무"
        />
        <MetricCard
          icon={CheckCircle2}
          label="최근 30일 완료"
          value={`${formatCount(dashboard.completedInPeriod.count)}건`}
          detail={`${formatDueDate(dashboard.completedInPeriod.from)} - ${formatDueDate(dashboard.completedInPeriod.to)}`}
        />
        <MetricCard
          icon={BrainCircuit}
          label="AI 처리 실패"
          value={`${formatCount(dashboard.aiFailedCount)}건`}
          detail="선택한 소속 팀의 내 업무 기준"
        />
        <MetricCard
          icon={CalendarClock}
          label="7일 내 마감"
          value={`${formatCount(dashboard.thisWeekDue.length)}건`}
          detail="D-7 이내 미완료 업무"
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <WorklogListPanel
          icon={ListChecks}
          title="오늘의 업무"
          description="진행 중인 업무를 우선으로 마감 가까운 순서로 표시합니다."
          emptyMessage="오늘 확인할 업무가 없습니다."
          items={dashboard.todayItems}
          showContext={false}
          pageSize={TODAY_WORKLOG_PAGE_SIZE}
        />
        <WorklogListPanel
          icon={CalendarClock}
          title="7일 내 마감"
          description="D-7 이내 마감 예정인 미완료 업무입니다."
          emptyMessage="7일 내 마감 예정 업무가 없습니다."
          items={dashboard.thisWeekDue}
          showContext={false}
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <ImminentWorklogPanel
          description="선택한 소속 팀 기준 D-3 이내 또는 지연된 미완료 업무입니다."
          items={dashboard.imminentAndOverdue}
          showContext={false}
        />
        <BlockedWorklogPanel items={dashboard.blockedByPredecessors} />
      </div>
    </div>
  );
}

function ComparisonDashboardView({
  viewModel,
}: {
  viewModel: ComparisonDashboardViewModel;
}) {
  return (
    <div className="space-y-6">
      {/* 관리자 scope들은 같은 구조를 쓰고, 라벨/목록 단위만 viewModel에서 바꿉니다. */}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={Gauge}
          label="전체 진행률"
          value={formatPercent(viewModel.totalProgress.rate)}
          detail={`${formatCount(viewModel.totalProgress.completed)} / ${formatCount(viewModel.totalProgress.total)}건 완료`}
        />
        {/* 전체 부서 비교 기준 카드: label="부서 부하 편중 지수" */}
        <MetricCard
          icon={Activity}
          label={viewModel.loadMetricLabel}
          value={formatIndex(viewModel.loadBalanceIndex)}
          detail={viewModel.loadMetricDetail}
        />
        <MetricCard
          icon={CheckCircle2}
          label="주간 처리 업무 수"
          value={`${formatCount(viewModel.weeklyCompleted)}건`}
          detail={viewModel.weeklyMetricDetail}
        />
        <MetricCard
          icon={BrainCircuit}
          label="AI 파이프라인 성공률"
          value={formatPercent(viewModel.aiPipelineSuccessRate)}
          detail={viewModel.aiMetricDetail}
        />
      </div>

      <WorkloadPanel
        title={viewModel.workloadTitle}
        description={viewModel.workloadDescription}
        countLabel={viewModel.workloadCountLabel}
        items={viewModel.workloadItems}
      />

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <CompletionPanel
          title={viewModel.completionTitle}
          description={viewModel.completionDescription}
          countLabel={viewModel.completionCountLabel}
          items={viewModel.completionItems}
        />
        <ImminentWorklogPanel
          description={viewModel.imminentDescription}
          items={viewModel.imminentAndOverdue}
          showContext={viewModel.showWorklogContext ?? true}
        />
      </div>
    </div>
  );
}

export function DirectorDashboardLoading() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <div
            key={index}
            className="workspace-panel-soft h-32 animate-pulse rounded-2xl"
          />
        ))}
      </div>
      <div className="workspace-panel-soft h-80 animate-pulse rounded-2xl" />
      <div className="workspace-panel-soft h-80 animate-pulse rounded-2xl" />
    </div>
  );
}

export function DirectorDashboardError() {
  return (
    <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-5 text-sm text-destructive">
      대시보드를 불러오지 못했습니다.
    </div>
  );
}

function MetricCard({
  icon: Icon,
  label,
  value,
  detail,
}: {
  icon: LucideIcon;
  label: string;
  value: string;
  detail: string;
}) {
  return (
    <CardSpotlight className="rounded-[24px]">
      <CardContent className="min-h-32 p-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-medium uppercase tracking-[0.18em] text-muted-foreground">
              {label}
            </p>
            <p className="mt-3 text-2xl font-semibold text-foreground">
              {value}
            </p>
          </div>
          <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl border border-border/70 bg-muted/40 text-primary">
            <Icon className="size-5" />
          </div>
        </div>
        <p className="mt-4 text-sm text-muted-foreground">{detail}</p>
      </CardContent>
    </CardSpotlight>
  );
}

function CompletionPanel({
  title,
  description,
  countLabel,
  items,
}: {
  title: string;
  description: string;
  countLabel: string;
  items: CompletionItem[];
}) {
  return (
    <DashboardPanel
      icon={TrendingUp}
      title={title}
      description={description}
      countLabel={countLabel}
    >
      <div className="space-y-4">
        {items.map((item) => (
          <RateRow
            key={item.id}
            href={item.href}
            label={item.label}
            value={formatPercent(item.rate)}
            detail={`${formatCount(item.completed)} / ${formatCount(item.total)}건`}
            rate={item.rate}
          />
        ))}
      </div>
    </DashboardPanel>
  );
}

function WorkloadPanel({
  title,
  description,
  countLabel,
  items,
}: {
  title: string;
  description: string;
  countLabel: string;
  items: WorkloadItem[];
}) {
  const maxCount = Math.max(
    ...items.map((item) => item.activeWorklogCount),
    1,
  );

  return (
    <DashboardPanel
      icon={BarChart3}
      title={title}
      description={description}
      countLabel={countLabel}
    >
      {items.length === 0 ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          표시할 업무 부하 데이터가 없습니다.
        </div>
      ) : (
        <div className="overflow-x-auto pb-1">
          <div
            className="grid min-w-[34rem] items-end gap-4"
            style={{
              gridTemplateColumns: `repeat(${items.length}, minmax(7rem, 1fr))`,
            }}
          >
            {items.map((item) => {
              const rate = item.activeWorklogCount / maxCount;
              const barHeight = `${Math.max(clampRate(rate) * 100, 3)}%`;

              return (
                <Link
                  key={item.id}
                  href={item.href}
                  className="group flex min-h-72 flex-col justify-end rounded-2xl border border-border/60 bg-background/35 px-4 py-4 transition hover:border-primary/30 hover:bg-background/55 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <div className="mb-3 text-center">
                    <p className="text-sm font-semibold text-primary">
                      {formatCount(item.activeWorklogCount)}건
                    </p>
                    <p className="mt-1 truncate text-xs text-muted-foreground">
                      미완료 업무
                    </p>
                  </div>
                  <div className="flex h-44 items-end rounded-2xl bg-muted/40 px-3 pt-3">
                    <div
                      className="w-full rounded-t-2xl bg-primary transition-all"
                      style={{ height: barHeight }}
                    />
                  </div>
                  <p className="mt-3 truncate text-center text-sm font-semibold text-foreground">
                    {item.label}
                  </p>
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </DashboardPanel>
  );
}

function ImminentWorklogPanel({
  description,
  items,
  showContext = true,
}: {
  description: string;
  items: DashboardWorklogBrief[];
  showContext?: boolean;
}) {
  const pagination = usePagination(items, IMMINENT_WORKLOG_PAGE_SIZE);

  return (
    <DashboardPanel
      icon={AlertTriangle}
      title="마감 임박 및 지연 업무"
      description={description}
      countLabel={`${items.length}건`}
    >
      {items.length === 0 ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          마감 임박 또는 지연 업무가 없습니다.
        </div>
      ) : (
        <div className="space-y-4">
          <div className="grid gap-3">
            {pagination.items.map((item) => (
              <WorklogBriefLink
                key={item.worklogId}
                item={item}
                showContext={showContext}
              />
            ))}
          </div>
          <Pagination
            page={pagination.page}
            totalPages={pagination.totalPages}
            onPageChange={pagination.setPage}
          />
        </div>
      )}
    </DashboardPanel>
  );
}

function WorklogListPanel({
  icon,
  title,
  description,
  emptyMessage,
  items,
  showContext = true,
  pageSize,
}: {
  icon: LucideIcon;
  title: string;
  description: string;
  emptyMessage: string;
  items: DashboardWorklogBrief[];
  showContext?: boolean;
  pageSize?: number;
}) {
  const listPageSize = pageSize ?? Math.max(items.length, 1);
  const pagination = usePagination(items, listPageSize);

  return (
    <DashboardPanel
      icon={icon}
      title={title}
      description={description}
      countLabel={`${items.length}건`}
    >
      {items.length === 0 ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          {emptyMessage}
        </div>
      ) : (
        <div className="space-y-4">
          <div className="grid gap-3">
            {pagination.items.map((item) => (
              <WorklogBriefLink
                key={item.worklogId}
                item={item}
                showContext={showContext}
              />
            ))}
          </div>
          {pagination.totalPages > 1 ? (
            <Pagination
              page={pagination.page}
              totalPages={pagination.totalPages}
              onPageChange={pagination.setPage}
            />
          ) : null}
        </div>
      )}
    </DashboardPanel>
  );
}

function WorklogBriefLink({
  item,
  showContext = true,
}: {
  item: DashboardWorklogBrief;
  showContext?: boolean;
}) {
  const contextLabel = [item.departmentName, item.teamName, item.authorName]
    .filter(Boolean)
    .join(" · ");
  const hasContext = showContext && Boolean(contextLabel);

  return (
    <Link
      href={`/worklog/detail/${item.worklogId}`}
      className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-4 transition hover:border-primary/30 hover:bg-muted/45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div
        className={cn(
          "flex items-start justify-between gap-3",
          hasContext ? "min-h-16" : null,
        )}
      >
        <div className="min-w-0">
          <p className="line-clamp-2 text-sm font-semibold leading-6 text-foreground">
            {item.title}
          </p>
          {hasContext ? (
            <p className="mt-2 truncate text-xs text-muted-foreground">
              {contextLabel}
            </p>
          ) : null}
        </div>
        <span className="shrink-0 rounded-full border border-warning/30 bg-warning/10 px-2.5 py-1 text-xs font-semibold text-warning">
          {formatOverdueDays(item.daysOverdue)}
        </span>
      </div>
      <p
        className={cn(
          "text-xs font-medium text-muted-foreground",
          hasContext ? "mt-3" : "mt-2",
        )}
      >
        마감일 {formatDueDate(item.dueDate)}
      </p>
    </Link>
  );
}

function BlockedWorklogPanel({ items }: { items: DashboardBlockedWorklog[] }) {
  return (
    <DashboardPanel
      icon={AlertTriangle}
      title="선행 업무 대기"
      description="완료되지 않은 선행 업무 때문에 막힌 내 업무입니다."
      countLabel={`${items.length}건`}
    >
      {items.length === 0 ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          선행 업무 대기 항목이 없습니다.
        </div>
      ) : (
        <div className="grid gap-3">
          {items.map((item) => (
            <Link
              key={item.worklogId}
              href={`/worklog/detail/${item.worklogId}`}
              className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-4 transition hover:border-primary/30 hover:bg-muted/45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <p className="line-clamp-2 text-sm font-semibold leading-6 text-foreground">
                {item.title}
              </p>
              <p className="mt-2 text-xs text-muted-foreground">
                선행 {formatCount(item.predecessors.length)}건
              </p>
            </Link>
          ))}
        </div>
      )}
    </DashboardPanel>
  );
}

function DashboardPanel({
  icon: Icon,
  title,
  description,
  countLabel,
  children,
}: {
  icon: LucideIcon;
  title: string;
  description: string;
  countLabel: string;
  children: ReactNode;
}) {
  return (
    <section className="workspace-panel-soft rounded-2xl p-5">
      <div className="mb-5 flex flex-col gap-3 border-b border-border/70 pb-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex min-w-0 items-start gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl border border-border/70 bg-background/60 text-primary">
            <Icon className="size-5" />
          </div>
          <div className="min-w-0">
            <h2 className="text-base font-semibold text-foreground">{title}</h2>
            {description ? (
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                {description}
              </p>
            ) : null}
          </div>
        </div>
        {countLabel ? (
          <p className="shrink-0 text-sm font-semibold text-muted-foreground">
            {countLabel}
          </p>
        ) : null}
      </div>
      {children}
    </section>
  );
}

function RateRow({
  href,
  label,
  value,
  detail,
  rate,
}: {
  href: string;
  label: string;
  value: string;
  detail: string;
  rate: number;
}) {
  return (
    <Link
      href={href}
      className="block rounded-2xl border border-border/70 bg-background/45 px-4 py-3 transition hover:border-primary/30 hover:bg-background/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-foreground">
            {label}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">{detail}</p>
        </div>
        <p className="shrink-0 text-sm font-semibold text-primary">{value}</p>
      </div>
      <div className="mt-3 h-2 overflow-hidden rounded-full bg-muted">
        <div
          className="h-full rounded-full bg-primary"
          style={{ width: `${clampRate(rate) * 100}%` }}
        />
      </div>
    </Link>
  );
}
