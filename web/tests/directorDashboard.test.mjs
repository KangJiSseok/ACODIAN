import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import {
  clampRate,
  formatCount,
  formatDueDate,
  formatIndex,
  formatOverdueDays,
  formatPercent,
} from "../src/app/(protected)/_dashboard/_utils/dashboardFormat.ts";

const serviceFile = readFileSync(
  new URL(
    "../src/app/(protected)/_dashboard/_service/dashboard.service.ts",
    import.meta.url,
  ),
  "utf8",
);

const hookFile = readFileSync(
  new URL(
    "../src/app/(protected)/_dashboard/_hooks/useDirectorDashboard.ts",
    import.meta.url,
  ),
  "utf8",
);

const pageFile = readFileSync(
  new URL("../src/app/(protected)/page.tsx", import.meta.url),
  "utf8",
);

const selectorFile = readFileSync(
  new URL(
    "../src/app/(protected)/_dashboard/_components/dashboardScopeSelector.tsx",
    import.meta.url,
  ),
  "utf8",
);

test("director dashboard service uses the real single dashboard endpoint", () => {
  assert.match(serviceFile, /apiClient\.get<DirectorDashboard>\("\/dashboard"/);
  assert.match(serviceFile, /params:\s*\{\s*scope:\s*"DEPARTMENT_COMPARISON"\s*\}/);
});

test("director dashboard query key is scoped to department comparison", () => {
  assert.match(hookFile, /\["dashboard"\]\s+as const/);
  assert.match(hookFile, /"department-comparison"/);
  assert.match(hookFile, /enabled,/);
});

test("dashboard page keeps shortcut fallback for users without teams", () => {
  assert.match(pageFile, /getDashboardRole\(user\)/);
  assert.match(pageFile, /!canUseDashboard \? <DashboardShortcutGrid user=\{user\} \/> : null/);
  assert.match(pageFile, /useDirectorDashboard\(\s*isDepartmentComparisonSelected,\s*\)/);
});

test("team leaders and members with teams use my dashboard", () => {
  assert.match(pageFile, /const hasTeamDashboard = teams\.length > 0/);
  assert.match(pageFile, /const canUseDashboard = isDashboardAdmin \|\| hasTeamDashboard/);
  assert.match(pageFile, /const isMyDashboardSelected =\s*canUseDashboard && scopeSelection\.view === "ME"/);
  assert.match(pageFile, /const dashboardTitle = getDashboardTitle\(dashboardRole, hasTeamDashboard\)/);
  assert.match(pageFile, /title=\{dashboardTitle\}/);
  assert.match(pageFile, /canUseDashboard\s*\?\s*\(/);
  assert.match(pageFile, /<MyDashboardView dashboard=\{myDashboardQuery\.data\} \/>/);
});

test("department head dashboard uses own department and team detail scopes", () => {
  assert.match(pageFile, /const isDepartmentHead = dashboardRole === "DEPARTMENT_HEAD"/);
  assert.doesNotMatch(pageFile, /useTeamList\(TEAM_LIST_PARAMS, isDepartmentHead\)/);
  assert.match(pageFile, /getDepartmentDashboardTeamOptions\(departmentDashboardQuery\.data\)/);
  assert.match(pageFile, /teamCompletionRates/);
  assert.match(pageFile, /teamWorkload/);
  assert.match(pageFile, /useTeamDashboard\(\s*selectedTeamDetailId,\s*isTeamDetailSelected,\s*\)/);
  assert.match(pageFile, /<TeamDashboardView dashboard=\{teamDashboardQuery\.data\} \/>/);
  assert.match(serviceFile, /apiClient\.get<TeamDashboard>\("\/dashboard"/);
  assert.match(serviceFile, /scope:\s*"TEAM_DETAIL"/);
});

test("department head team detail selector shows loading state until admin teams resolve", () => {
  assert.match(pageFile, /const adminTeamsLoading =\s*isDepartmentHead && departmentDashboardQuery\.isLoading/);
  assert.match(pageFile, /adminTeamsLoading=\{adminTeamsLoading\}/);
  assert.match(selectorFile, /adminTeamsLoading\?: boolean/);
  assert.match(selectorFile, /const isAdminTeamSelectDisabled =\s*adminTeamsLoading \|\| adminTeamOptions\.length === 0/);
  assert.match(selectorFile, /disabled=\{isAdminTeamSelectDisabled\}/);
  assert.match(selectorFile, /const isAdminTeamScopeDisabled =[\s\S]*draftAdminScope === "TEAM_DETAIL"[\s\S]*isAdminTeamSelectDisabled/);
  assert.match(selectorFile, /const canApply = isAdminTeamScopeDisabled\s*\?\s*false\s*:\s*canApplyDraft/);
});

test("director dashboard places workload above completion and imminent panels", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  const workloadIndex = componentFile.indexOf("<WorkloadPanel");
  const completionIndex = componentFile.indexOf("<CompletionPanel");
  const imminentIndex = componentFile.indexOf("<ImminentWorklogPanel", completionIndex);

  assert.ok(workloadIndex > -1, "workload panel should render");
  assert.ok(completionIndex > workloadIndex, "completion panel should be below workload");
  assert.ok(imminentIndex > completionIndex, "imminent panel should share the second row");
  assert.doesNotMatch(componentFile, /RecentNotificationPlaceholder/);
  assert.doesNotMatch(componentFile, /title="최근 알림"/);
});

test("director dashboard panels use the shared card spotlight surface", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  assert.match(componentFile, /<CardSpotlight className="rounded-\[24px\] p-5">/);
  assert.doesNotMatch(
    componentFile,
    /<section className="workspace-panel-soft rounded-2xl p-5">/,
  );
});

test("dashboard panel content reacts to the card spotlight hover state", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  assert.match(componentFile, /group-hover\/card-spotlight:border-primary\/30/);
  assert.match(componentFile, /group-hover\/card-spotlight:bg-primary\/8/);
  assert.match(componentFile, /group-hover\/card-spotlight:text-primary/);
  assert.match(componentFile, /group-hover\/card-spotlight:bg-primary\/90/);
  assert.match(componentFile, /group-hover\/card-spotlight:bg-background\/60/);
  assert.match(componentFile, /group-hover\/card-spotlight:bg-muted\/45/);
});

test("workload bars rise sequentially and respect reduced motion", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );
  const globalsCss = readFileSync(
    new URL("../src/app/globals.css", import.meta.url),
    "utf8",
  );

  assert.match(componentFile, /items\.map\(\(item, index\) =>/);
  assert.match(
    componentFile,
    /className="dashboard-workload-bar w-full rounded-t-2xl bg-primary[^"]*group-hover\/card-spotlight:bg-primary\/90"/,
  );
  assert.match(componentFile, /animationDelay: `\$\{index \* 75\}ms`/);
  assert.match(globalsCss, /@keyframes dashboard-workload-rise/);
  assert.match(globalsCss, /prefers-reduced-motion: reduce/);
  assert.match(globalsCss, /\.dashboard-workload-bar/);
});

test("dashboard worklog cards show due date without raw status code", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  assert.match(componentFile, /마감일 \{formatDueDate\(item\.dueDate\)\}/);
  assert.doesNotMatch(componentFile, /· \{item\.statusCode\}/);
});

test("single team dashboard worklog cards hide redundant context metadata", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  assert.match(componentFile, /showWorklogContext:\s*false/);
  assert.match(componentFile, /showContext=\{false\}/);
  assert.match(componentFile, /showContext\?: boolean/);
  assert.match(componentFile, /const contextLabel = \[item\.departmentName, item\.teamName, item\.authorName\]/);
  assert.match(componentFile, /const hasContext = showContext && Boolean\(contextLabel\)/);
  assert.match(componentFile, /hasContext \? "min-h-16" : null/);
  assert.match(componentFile, /hasContext \? "mt-3" : "mt-2"/);
  assert.doesNotMatch(componentFile, /\.join\(".*"\) \|\| "-"/);
});

test("today worklog panel uses frontend pagination with three items per page", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  assert.match(componentFile, /const TODAY_WORKLOG_PAGE_SIZE = 3/);
  assert.match(componentFile, /pageSize=\{TODAY_WORKLOG_PAGE_SIZE\}/);
  assert.match(componentFile, /pageSize\?: number/);
  assert.match(componentFile, /const listPageSize = pageSize \?\? Math\.max\(items\.length, 1\)/);
  assert.match(componentFile, /usePagination\(items,\s*listPageSize\)/);
  assert.match(componentFile, /pagination\.items\.map/);
  assert.match(componentFile, /pagination\.totalPages > 1 \? \(/);
});

test("my dashboard uses rolling seven day due copy", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  assert.match(componentFile, /label="7일 내 마감"/);
  assert.match(componentFile, /title="7일 내 마감"/);
  assert.match(componentFile, /emptyMessage="7일 내 마감 예정 업무가 없습니다\."/);
  assert.match(componentFile, /D-7 이내 마감 예정인 미완료 업무입니다\./);
  assert.doesNotMatch(componentFile, /이번 주 마감/);
});

test("director dashboard metric cards follow requested order and labels", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  const labels = [
    'label="전체 진행률"',
    'label="부서 부하 편중 지수"',
    'label="주간 처리 업무 수"',
    'label="AI 파이프라인 성공률"',
  ];

  let lastIndex = -1;
  for (const label of labels) {
    const index = componentFile.indexOf(label);
    assert.ok(index > lastIndex, `${label} should appear in order`);
    lastIndex = index;
  }

  assert.match(componentFile, /부서 간 업무량 분산도, 1에 가까울수록 균형/);
  assert.match(componentFile, /전체 부서 비교 최근 7일 완료 산출/);
  assert.match(componentFile, /전체 부서 비교 업무·파일 통합/);
});

test("dashboard format helpers clamp percentages and format counts", () => {
  assert.equal(clampRate(-1), 0);
  assert.equal(clampRate(0.424), 0.424);
  assert.equal(clampRate(2), 1);

  assert.equal(formatPercent(0), "0%");
  assert.equal(formatPercent(0.424), "42.4%");
  assert.equal(formatPercent(1), "100%");
  assert.equal(formatCount(12345), "12,345");
  assert.equal(formatIndex(0.9221), "0.92");
});

test("dashboard date and overdue labels are stable", () => {
  assert.equal(formatDueDate(null), "마감일 없음");
  assert.equal(formatDueDate("not-a-date"), "not-a-date");
  assert.equal(formatOverdueDays(null), "임박");
  assert.equal(formatOverdueDays(0), "오늘 마감");
  assert.equal(formatOverdueDays(12), "12일 지연");
});
