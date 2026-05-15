import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

function readSource(path) {
  return readFileSync(new URL(`../src/${path}`, import.meta.url), "utf8");
}

const dashboardPage = readSource("app/(protected)/page.tsx");
const dashboardScopeSelector = readSource(
  "app/(protected)/_dashboard/_components/dashboardScopeSelector.tsx",
);
const filePage = readSource("app/(protected)/file/page.tsx");
const userPage = readSource("app/(protected)/user/page.tsx");
const notificationPage = readSource("app/(protected)/notification/page.tsx");
const notificationList = readSource(
  "app/(protected)/notification/_components/notificationList.tsx",
);
const notificationCenterPopover = readSource(
  "app/_common/components/layout/notificationCenterPopover.tsx",
);
const gnb = readSource("app/_common/components/layout/gnb.tsx");
const sidebar = readSource("app/_common/components/layout/sidebar.tsx");
const globals = readSource("app/globals.css");
const cardSpotlight = readSource("components/ui/card-spotlight.tsx");
const authService = readSource("app/_common/service/auth.ts");
const myPage = readSource("app/(protected)/my-page/page.tsx");

test("dashboard scope selector uses compact filter sizing and short header copy", () => {
  assert.match(dashboardScopeSelector, /variant="default"/);
  assert.match(dashboardScopeSelector, /className="h-10 max-w-\[22rem\] min-w-32 px-6 text-sm font-semibold"/);
  assert.match(dashboardScopeSelector, /<span className="min-w-0 truncate">\{selectedLabel\}<\/span>/);
  assert.doesNotMatch(dashboardPage, /선택 부서 범위에서/);
  assert.doesNotMatch(dashboardPage, /전사 범위에서/);
  assert.doesNotMatch(dashboardPage, /AI 파이프라인 건강도를 비교합니다/);
});

test("file and user filters follow the worklog filter control size", () => {
  assert.match(filePage, /const activeFilterCount = \[fileType, period, aiStatus\]/);
  assert.match(filePage, /className="h-12 justify-center"/);
  assert.match(filePage, /className="space-y-4 pb-4 pt-3"/);
  assert.match(filePage, /className="grid gap-4 md:grid-cols-2 xl:grid-cols-4"/);

  assert.match(userPage, /const activeFilterCount = \[/);
  assert.match(userPage, /className="h-12 justify-center"/);
  assert.match(userPage, /className="space-y-4 pb-4 pt-3"/);
  assert.match(userPage, /className="grid gap-4 md:grid-cols-2 xl:grid-cols-4"/);
});

test("card spotlight gradient only appears on hover or focus", () => {
  assert.match(cardSpotlight, /opacity-0 transition-opacity/);
  assert.match(cardSpotlight, /group-hover\/card-spotlight:opacity-100/);
  assert.doesNotMatch(cardSpotlight, /radial-gradient\(280px circle at 12% 0%/);
});

test("notification actions use worklog-style variants with distinct colors", () => {
  assert.match(notificationPage, /variant="default"[\s\S]*onClick=\{markAllRead\}/);
  assert.match(notificationPage, /알림 목록[\s\S]*onClick=\{markAllRead\}/);
  assert.match(notificationPage, /className="h-12 justify-center"/);
  assert.match(notificationList, /variant="default"[\s\S]*onClick=\{\(\) => onMarkRead\(notification\.id\)\}/);
  assert.match(notificationList, /variant="secondary"[\s\S]*관련 화면 이동/);
  assert.match(notificationList, /h-11 px-5 text-sm font-semibold/);
  assert.doesNotMatch(notificationList, /shadow-\[/);
});

test("notification summary cards prioritize unread then read before total", () => {
  assert.match(
    notificationPage,
    /label="읽지 않은 알림"[\s\S]*label="읽은 알림"[\s\S]*label="전체 알림"/,
  );
});

test("notification page places status description under the status heading", () => {
  assert.match(notificationPage, /<PageHeader title="알림" \/>/);
  assert.doesNotMatch(notificationPage, /PageHeader title="알림" description=/);
  assert.match(
    notificationPage,
    /알림 현황[\s\S]*업무 마감 알림과 읽음 상태를 확인합니다\.[\s\S]*className="grid gap-3 md:grid-cols-3"/,
  );
});

test("notification center popover uses theme-specific opaque surface and text colors", () => {
  assert.match(notificationCenterPopover, /bg-white text-slate-950/);
  assert.match(notificationCenterPopover, /dark:bg-slate-950 dark:text-slate-50/);
  assert.match(notificationCenterPopover, /text-slate-500 dark:text-slate-400/);
  assert.match(notificationCenterPopover, /dark:hover:text-slate-50/);
  assert.doesNotMatch(notificationCenterPopover, /bg-card text-card-foreground/);
});

test("notification center popover shows compact all-notification cards", () => {
  assert.match(notificationCenterPopover, /max-h-\[420px\] overflow-y-auto/);
  assert.match(notificationCenterPopover, /formatNotificationCenterDate\(notification\.createdAt\)/);
  assert.match(notificationCenterPopover, /notification\.teamName/);
  assert.doesNotMatch(notificationCenterPopover, /typeLabel/);
  assert.doesNotMatch(notificationCenterPopover, /notification\.content/);
});

test("gnb maps notification team ids into notification center items", () => {
  assert.match(gnb, /useAuth/);
  assert.match(gnb, /teamNameById/);
  assert.match(gnb, /teamName: getNotificationCenterTeamName/);
});

test("notification center mark-all action has a visible primary button surface", () => {
  assert.match(notificationCenterPopover, /variant="default"/);
  assert.match(notificationCenterPopover, /h-9 rounded-xl bg-primary px-3\.5/);
  assert.match(notificationCenterPopover, /전체 읽음/);
  assert.doesNotMatch(notificationCenterPopover, /CheckCheck/);
  assert.match(notificationCenterPopover, /disabled=\{unreadCount === 0\}/);
});

test("notification center trigger keeps theme-appropriate text while open on topbar", () => {
  assert.match(gnb, /aria-expanded=\{isNotificationOpen\}/);
  assert.match(gnb, /aria-expanded:!text-slate-900/);
  assert.match(gnb, /dark:aria-expanded:!text-white/);
  assert.match(gnb, /dark:active:!text-white/);
});

test("notification center count uses the worklog red count badge", () => {
  assert.match(gnb, /unreadCount > 0/);
  assert.match(gnb, /group relative h-10 w-10/);
  assert.match(gnb, /absolute -right-1\.5 -top-1\.5/);
  assert.match(gnb, /rounded-full bg-red-600/);
  assert.match(gnb, /ring-2 ring-background/);
});

test("notification center trigger shows only icon and count visually", () => {
  assert.match(gnb, /<Bell className="size-4" \/>/);
  assert.match(gnb, /<span className="sr-only">알림 센터<\/span>/);
  assert.doesNotMatch(gnb, /className="hidden text-left md:inline">알림 센터/);
  assert.match(gnb, /font-bold/);
});

test("workspace shell uses light gray navigation colors outside dark mode", () => {
  assert.match(globals, /--workspace-shell-base: #f1f5f9/);
  assert.match(globals, /#f8fafc 0%, #f1f5f9 48%, #e2e8f0 100%/);
  assert.match(globals, /--workspace-shell-foreground: #0f172a/);
  assert.match(globals, /\.dark \{[\s\S]*--workspace-shell-base: #0f1a35/);
  assert.doesNotMatch(sidebar, /"dark workspace-sidebar/);
  assert.match(sidebar, /text-slate-900 dark:text-white/);
  assert.match(sidebar, /border-slate-200\/80 dark:border-white\/10/);
  assert.match(gnb, /border-slate-200\/80/);
  assert.match(gnb, /bg-slate-100\/70/);
  assert.match(gnb, /dark:border-white\/10/);
});

test("my profile lets users change the primary team through users me", () => {
  assert.match(authService, /primaryTeamId: number \| null/);
  assert.match(authService, /primary_team_id: payload\.primaryTeamId/);
  assert.match(myPage, /primaryTeamId: string/);
  assert.match(myPage, /name="primaryTeamId"/);
  assert.match(myPage, /function ProfileSelectField/);
});
