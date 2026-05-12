import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const notificationPageFile = readFileSync(
  new URL("../src/app/(protected)/notification/page.tsx", import.meta.url),
  "utf8",
);

const teamListHookFile = readFileSync(
  new URL("../src/app/(protected)/team/_hooks/useTeamList.ts", import.meta.url),
  "utf8",
);

test("notification filters only fetch full organization options for directors", () => {
  assert.match(notificationPageFile, /useAuth\(\)/);
  assert.match(notificationPageFile, /isDirectorProfile\(user\)/);
  assert.match(notificationPageFile, /useDepartmentList\(isDirector\)/);
  assert.match(notificationPageFile, /useTeamList\(\{\s*pageSize:\s*100\s*\},\s*isDirector\)/);
  assert.doesNotMatch(notificationPageFile, /useDepartmentList\(\)/);
  assert.doesNotMatch(notificationPageFile, /useTeamList\(\{\s*pageSize:\s*100\s*\}\)/);
});

test("notification filters use current user's department and teams outside director scope", () => {
  assert.match(notificationPageFile, /getUserDepartmentOptions\(user\)/);
  assert.match(notificationPageFile, /getUserTeamOptions\(user\)/);
  assert.match(notificationPageFile, /user\?\.departmentId/);
  assert.match(notificationPageFile, /user\?\.teams/);
});

test("notification team filters only expose active team options when status is available", () => {
  assert.match(notificationPageFile, /const ACTIVE_TEAM_STATUS = "ACTIVE"/);
  assert.match(notificationPageFile, /teamOptionsSource\.filter\(isActiveTeamOption\)/);
  assert.match(notificationPageFile, /function isActiveTeamOption/);
  assert.match(notificationPageFile, /team\.statusCode \?\? ACTIVE_TEAM_STATUS/);
});

test("team list query can be disabled by callers without changing existing defaults", () => {
  assert.match(teamListHookFile, /export function useTeamList\(params: GetTeamsParams = \{\}, enabled = true\)/);
  assert.match(teamListHookFile, /enabled,/);
});
