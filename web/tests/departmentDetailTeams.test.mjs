import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const departmentService = readFileSync(
  new URL(
    "../src/app/(protected)/department/_service/department.service.ts",
    import.meta.url,
  ),
  "utf8",
);
const departmentTypes = readFileSync(
  new URL(
    "../src/app/(protected)/department/_types/department.types.ts",
    import.meta.url,
  ),
  "utf8",
);
const departmentHooks = readFileSync(
  new URL(
    "../src/app/(protected)/department/_hooks/useDepartmentList.ts",
    import.meta.url,
  ),
  "utf8",
);
const departmentDetailPage = readFileSync(
  new URL(
    "../src/app/(protected)/department/detail/[id]/page.tsx",
    import.meta.url,
  ),
  "utf8",
);

test("department detail service uses GET /departments/{id}/detail", () => {
  assert.match(departmentTypes, /export interface DepartmentDetailTeamSummary/);
  assert.match(departmentTypes, /export interface DepartmentDetail/);
  assert.match(departmentTypes, /teams: DepartmentDetailTeamSummary\[\]/);
  assert.match(
    departmentService,
    /getDepartmentDetail: \(departmentId: number\) =>\s*apiClient\.get<DepartmentDetail>\(\s*`\/departments\/\$\{departmentId\}\/detail`/,
  );
});

test("department detail hook exposes an id-scoped detail query", () => {
  assert.match(
    departmentHooks,
    /detail: \(departmentId: number\) => \[\.\.\.departmentKeys\.all, "detail", departmentId\] as const/,
  );
  assert.match(departmentHooks, /export function useDepartmentDetail/);
  assert.match(departmentHooks, /queryKey: departmentKeys\.detail\(departmentId\)/);
  assert.match(
    departmentHooks,
    /queryFn: \(\) => departmentService\.getDepartmentDetail\(departmentId\)/,
  );
  assert.match(departmentHooks, /enabled: Number\.isFinite\(departmentId\)/);
});

test("department detail page renders teams from the detail response", () => {
  assert.match(
    departmentDetailPage,
    /const \{ data: department, isLoading, error \} = useDepartmentDetail\(departmentId\)/,
  );
  assert.doesNotMatch(departmentDetailPage, /useDepartmentList/);
  assert.match(departmentDetailPage, /department\.teams\.length === 0/);
  assert.match(departmentDetailPage, /department\.teams\.map\(\(team\) =>/);
  assert.match(
    departmentDetailPage,
    /href=\{`\/team\/detail\/\$\{team\.teamId\}`\}/,
  );
  assert.match(departmentDetailPage, /team\.leaderName \?\? ".*"/);
  assert.match(departmentDetailPage, /formatTeamPeriod\(team\)/);
});
