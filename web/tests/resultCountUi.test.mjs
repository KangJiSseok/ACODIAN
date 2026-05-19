import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

function readSource(path) {
  return readFileSync(new URL(`../src/${path}`, import.meta.url), "utf8");
}

const resultCount = readSource(
  "app/_common/components/data-display/resultCount.tsx",
);
const dashboardPage = readSource("app/(protected)/page.tsx");
const worklogPage = readSource("app/(protected)/worklog/page.tsx");
const filePage = readSource("app/(protected)/file/page.tsx");
const departmentPage = readSource("app/(protected)/department/page.tsx");
const teamPage = readSource("app/(protected)/team/page.tsx");
const userPage = readSource("app/(protected)/user/page.tsx");
const tagManager = readSource("app/(protected)/tag/_components/tagManager.tsx");
const notificationPage = readSource("app/(protected)/notification/page.tsx");

test("list result counts share one text style and count format", () => {
  assert.match(resultCount, /text-sm font-medium text-muted-foreground/);
  assert.match(resultCount, /ml-1 font-semibold text-foreground/);
  assert.match(resultCount, /toLocaleString\("ko-KR"\)/);
});

test("workspace list sections use 조회된 result count labels", () => {
  assert.match(worklogPage, /<ResultCount\s+label="조회된 업무"\s+count=\{worklogPage\?\.totalCount \?\? 0\}\s+unit="개"/);
  assert.match(filePage, /<ResultCount\s+label="조회된 파일"\s+count=\{visibleFiles\.length\}\s+unit="개"/);
  assert.match(departmentPage, /<ResultCount\s+label="조회된 부서"\s+count=\{departments\.length\}\s+unit="개"/);
  assert.match(teamPage, /<ResultCount\s+label="조회된 팀"\s+count=\{filteredTeams\.length\}\s+unit="개"/);
  assert.match(userPage, /<ResultCount\s+label="조회된 사용자"\s+count=\{userPage\?\.totalCount \?\? 0\}\s+unit="명"/);
  assert.match(tagManager, /<ResultCount\s+label="조회된 태그"\s+count=\{tagPage\?\.totalCount \?\? 0\}\s+unit="개"/);
  assert.match(notificationPage, /<ResultCount\s+label="조회된 알림"\s+count=\{notificationPage\?\.totalCount \?\? 0\}\s+unit="개"/);

  for (const source of [
    worklogPage,
    filePage,
    departmentPage,
    teamPage,
    userPage,
    tagManager,
    notificationPage,
  ]) {
    assert.doesNotMatch(source, /표시 중인/);
  }
});

test("file search controls mirror worklog search and omit help icon", () => {
  assert.match(filePage, /const \[searchInput, setSearchInput\] = useState\(""\)/);
  assert.match(filePage, /function submitSearch\(\)/);
  assert.match(filePage, /<form[\s\S]*onSubmit=\{\(event\) => \{[\s\S]*submitSearch\(\)/);
  assert.match(filePage, /type="submit"[\s\S]*variant="default"[\s\S]*className="h-12 min-w-24 justify-center px-5 text-sm font-semibold"[\s\S]*검색/);
  assert.doesNotMatch(filePage, /CircleHelp/);
  assert.doesNotMatch(filePage, /aria-label="AI 상태 안내"/);
});

test("dashboard title uses 업무 대시보드 for dashboard users", () => {
  assert.match(dashboardPage, /return "업무 대시보드"/);
  assert.doesNotMatch(dashboardPage, /본부장 대시보드/);
  assert.doesNotMatch(dashboardPage, /사업부장 대시보드/);
});
