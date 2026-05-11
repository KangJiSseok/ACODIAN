import Link from "next/link";
import type { ReactNode } from "react";
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Bell,
  BrainCircuit,
  CheckCircle2,
  Gauge,
  type LucideIcon,
  TrendingUp,
} from "lucide-react";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import type {
  DashboardWorklogBrief,
  DepartmentCompletionRate,
  DepartmentLoad,
  DirectorDashboard,
} from "../_types/dashboard.types";
import {
  clampRate,
  formatCount,
  formatDueDate,
  formatIndex,
  formatOverdueDays,
  formatPercent,
} from "../_utils/dashboardFormat";

export function DirectorDashboardView({
  dashboard,
}: {
  dashboard: DirectorDashboard;
}) {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={Gauge}
          label="전체 진행률"
          value={formatPercent(dashboard.totalProgress.rate)}
          detail={`${formatCount(dashboard.totalProgress.completed)} / ${formatCount(dashboard.totalProgress.total)}건 완료`}
        />
        <MetricCard
          icon={Activity}
          label="부서 부하 편중 지수"
          value={formatIndex(dashboard.departmentLoadBalanceIndex)}
          detail="부서 간 업무량 분산도, 1에 가까울수록 균형"
        />
        <MetricCard
          icon={CheckCircle2}
          label="주간 처리 업무 수"
          value={`${formatCount(dashboard.weeklyCompleted)}건`}
          detail="전체 부서 비교 최근 7일 완료 산출"
        />
        <MetricCard
          icon={BrainCircuit}
          label="AI 파이프라인 성공률"
          value={formatPercent(dashboard.aiPipelineSuccessRate)}
          detail="전체 부서 비교 업무·파일 통합"
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <CompletionPanel items={dashboard.departmentCompletionRates} />
        <WorkloadPanel items={dashboard.departmentWorkload} />
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <ImminentWorklogPanel items={dashboard.imminentAndOverdue} />
        <RecentNotificationPlaceholder />
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
      <div className="grid gap-4 xl:grid-cols-2">
        <div className="workspace-panel-soft h-80 animate-pulse rounded-2xl" />
        <div className="workspace-panel-soft h-80 animate-pulse rounded-2xl" />
      </div>
      <div className="workspace-panel-soft h-80 animate-pulse rounded-2xl" />
    </div>
  );
}

export function DirectorDashboardError() {
  return (
    <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-5 text-sm text-destructive">
      본부장 대시보드를 불러오지 못했습니다.
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

function CompletionPanel({ items }: { items: DepartmentCompletionRate[] }) {
  return (
    <DashboardPanel
      icon={TrendingUp}
      title="부서별 완료율"
      description="부서별 완료 업무 비중을 비교합니다."
      countLabel={`${items.length}개 부서`}
    >
      <div className="space-y-4">
        {items.map((item) => (
          <RateRow
            key={item.departmentId}
            href={`/department/detail/${item.departmentId}`}
            label={item.departmentName}
            value={formatPercent(item.rate)}
            detail={`${formatCount(item.completed)} / ${formatCount(item.total)}건`}
            rate={item.rate}
          />
        ))}
      </div>
    </DashboardPanel>
  );
}

function WorkloadPanel({ items }: { items: DepartmentLoad[] }) {
  const maxCount = Math.max(
    ...items.map((item) => item.activeWorklogCount),
    1,
  );

  return (
    <DashboardPanel
      icon={BarChart3}
      title="부서별 활성 업무량"
      description="완료되지 않은 업무일지 분포를 확인합니다."
      countLabel={`${items.length}개 부서`}
    >
      <div className="space-y-4">
        {items.map((item) => (
          <RateRow
            key={item.departmentId}
            href={`/department/detail/${item.departmentId}`}
            label={item.departmentName}
            value={`${formatCount(item.activeWorklogCount)}건`}
            detail="미완료 업무"
            rate={item.activeWorklogCount / maxCount}
          />
        ))}
      </div>
    </DashboardPanel>
  );
}

function ImminentWorklogPanel({ items }: { items: DashboardWorklogBrief[] }) {
  return (
    <DashboardPanel
      icon={AlertTriangle}
      title="마감 임박 및 지연 업무"
      description="전사 기준 D-3 이내 또는 지연된 미완료 업무입니다."
      countLabel={`${items.length}건`}
    >
      {items.length === 0 ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          마감 임박 또는 지연 업무가 없습니다.
        </div>
      ) : (
        <div className="grid gap-3">
          {items.map((item) => (
            <Link
              key={item.worklogId}
              href={`/worklog/detail/${item.worklogId}`}
              className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-4 transition hover:border-primary/30 hover:bg-muted/45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <div className="flex min-h-16 items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="line-clamp-2 text-sm font-semibold leading-6 text-foreground">
                    {item.title}
                  </p>
                  <p className="mt-2 truncate text-xs text-muted-foreground">
                    {[item.departmentName, item.teamName, item.authorName]
                      .filter(Boolean)
                      .join(" · ") || "-"}
                  </p>
                </div>
                <span className="shrink-0 rounded-full border border-warning/30 bg-warning/10 px-2.5 py-1 text-xs font-semibold text-warning">
                  {formatOverdueDays(item.daysOverdue)}
                </span>
              </div>
              <p className="mt-3 text-xs font-medium text-muted-foreground">
                마감 {formatDueDate(item.dueDate)} · {item.statusCode}
              </p>
            </Link>
          ))}
        </div>
      )}
    </DashboardPanel>
  );
}

function RecentNotificationPlaceholder() {
  return (
    <DashboardPanel
      icon={Bell}
      title="최근 알림"
      description=""
      countLabel=""
    >
      <div className="min-h-[20rem]" />
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
