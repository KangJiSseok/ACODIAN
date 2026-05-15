import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const departmentPage = readFileSync(
  new URL("../src/app/(protected)/department/page.tsx", import.meta.url),
  "utf8",
);
const departmentDetailPage = readFileSync(
  new URL("../src/app/(protected)/department/detail/[id]/page.tsx", import.meta.url),
  "utf8",
);

test("department list cards navigate to detail without inline actions", () => {
  assert.match(
    departmentPage,
    /href=\{`\/department\/detail\/\$\{department\.departmentId\}`\}/,
  );
  assert.doesNotMatch(departmentPage, /useDeleteDepartment/);
  assert.doesNotMatch(departmentPage, /handleDelete/);
  assert.doesNotMatch(departmentPage, /Trash2/);
  assert.doesNotMatch(
    departmentPage,
    /href=\{`\/department\/edit\/\$\{department\.departmentId\}`\}/,
  );
});

test("department management page shows helper description copy", () => {
  assert.match(departmentPage, /관리 가능한 부서/);
  assert.match(departmentPage, /운영 현황/);
  assert.match(departmentPage, /활성 팀이 남아 있는 부서/);
  assert.match(departmentPage, /삭제할 수 없습니다/);
  assert.doesNotMatch(departmentPage, /권한 범위/);
  assert.doesNotMatch(departmentPage, /부서 삭제는 비활성화/);
  assert.doesNotMatch(departmentPage, /부서 생성과 삭제 정책/);
  assert.doesNotMatch(departmentPage, /본부장 전용/);
  assert.doesNotMatch(departmentPage, /삭제 제약/);
});

test("department create action is aligned with the department list title", () => {
  assert.match(departmentPage, /부서 목록[\s\S]*\/department\/create[\s\S]*부서 등록/);
  assert.match(
    departmentPage,
    /flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between/,
  );
});

test("department detail owns edit and delete actions", () => {
  assert.match(
    departmentDetailPage,
    /href=\{`\/department\/edit\/\$\{department\.departmentId\}`\}/,
  );
  assert.match(departmentDetailPage, /useDeleteDepartment/);
  assert.match(departmentDetailPage, /Trash2/);
  assert.match(departmentDetailPage, /handleDelete/);
});
