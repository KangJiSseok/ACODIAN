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

test("director dashboard service uses the real single dashboard endpoint", () => {
  assert.match(serviceFile, /apiClient\.get<DirectorDashboard>\("\/dashboard"/);
  assert.match(serviceFile, /params:\s*\{\s*scope:\s*"DEPARTMENT_COMPARISON"\s*\}/);
});

test("director dashboard query key is scoped to department comparison", () => {
  assert.match(hookFile, /\["dashboard"\]\s+as const/);
  assert.match(hookFile, /"department-comparison"/);
  assert.match(hookFile, /enabled,/);
});

test("dashboard page keeps non-director shortcut fallback", () => {
  assert.match(pageFile, /isDirectorProfile\(user\)/);
  assert.match(pageFile, /!isDirector \? <DashboardShortcutGrid user=\{user\} \/> : null/);
  assert.match(pageFile, /useDirectorDashboard\(isDirector\)/);
});

test("director dashboard reserves bottom-right recent notification panel", () => {
  const componentFile = readFileSync(
    new URL(
      "../src/app/(protected)/_dashboard/_components/directorDashboard.tsx",
      import.meta.url,
    ),
    "utf8",
  );

  assert.match(componentFile, /<ImminentWorklogPanel items=\{dashboard\.imminentAndOverdue\} \/>/);
  assert.match(componentFile, /<RecentNotificationPlaceholder \/>/);
  assert.match(componentFile, /title="최근 알림"/);
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
