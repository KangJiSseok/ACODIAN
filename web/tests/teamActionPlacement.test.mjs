import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const teamPage = readFileSync(
  new URL("../src/app/(protected)/team/page.tsx", import.meta.url),
  "utf8",
);
const teamDetail = readFileSync(
  new URL("../src/app/(protected)/team/_components/teamDetail.tsx", import.meta.url),
  "utf8",
);
const teamService = readFileSync(
  new URL("../src/app/(protected)/team/_service/team.service.ts", import.meta.url),
  "utf8",
);
const teamMutation = readFileSync(
  new URL("../src/app/(protected)/team/_hooks/useTeamMutation.ts", import.meta.url),
  "utf8",
);
const teamTypes = readFileSync(
  new URL("../src/app/(protected)/team/_types/team.types.ts", import.meta.url),
  "utf8",
);

test("team list cards navigate to detail without inline actions", () => {
  assert.match(teamPage, /href=\{`\/team\/detail\/\$\{team\.teamId\}`\}/);
  assert.doesNotMatch(teamPage, /useDeleteTeam/);
  assert.doesNotMatch(teamPage, /handleDelete/);
  assert.doesNotMatch(teamPage, /Trash2/);
  assert.doesNotMatch(teamPage, /variant="destructive"/);
  assert.doesNotMatch(teamPage, /href=\{`\/team\/edit\/\$\{team\.teamId\}`\}/);
});

test("team management page omits helper description copy", () => {
  assert.doesNotMatch(teamPage, /역할에 따라/);
  assert.doesNotMatch(teamPage, /올바른 범위/);
  assert.doesNotMatch(teamPage, /팀 상태, 책임자/);
  assert.doesNotMatch(teamPage, /한 화면에서 확인/);
});

test("team detail owns edit and delete actions", () => {
  assert.match(teamDetail, /href=\{`\/team\/edit\/\$\{team\.teamId\}`\}/);
  assert.match(teamDetail, /Pencil/);
  assert.match(teamDetail, /useDeleteTeam/);
  assert.match(teamDetail, /Trash2/);
  assert.match(teamDetail, /handleDelete/);
});

test("team manage actions use frontend access utility without changing api shape", () => {
  assert.match(teamDetail, /canManageTeam\(user, team\)/);
  assert.doesNotMatch(teamPage, /team\.myCanManage/);
  assert.doesNotMatch(teamDetail, /team\.myCanManage/);
  assert.doesNotMatch(teamTypes, /myCanManage/);
  assert.doesNotMatch(teamPage, /currentUserId === team\.deptHeadAdminUserId/);
  assert.doesNotMatch(teamDetail, /user\?\.userId === team\.deptHeadAdminUserId/);
});

test("team delete api is exposed through service and mutation hook", () => {
  assert.match(teamService, /deleteTeam:\s*\(teamId:\s*number\)/);
  assert.match(teamService, /apiClient\.delete<EmptyResponse>\(`\/teams\/\$\{teamId\}`\)/);
  assert.match(teamMutation, /export function useDeleteTeam\(\)/);
  assert.match(teamMutation, /mutationFn:\s*teamService\.deleteTeam/);
});
