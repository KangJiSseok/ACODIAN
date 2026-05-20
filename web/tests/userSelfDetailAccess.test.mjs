import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const userRoot = new URL("../src/app/(protected)/user/", import.meta.url);

function readUserFile(path) {
  return readFileSync(new URL(path, userRoot), "utf8");
}

test("self user detail only loads profile information and hides insight tabs", () => {
  const page = readUserFile("detail/[id]/page.tsx");
  const hooks = readUserFile("_hooks/useUserDetail.ts");
  const component = readUserFile("_components/userDetail.tsx");

  assert.match(page, /useAuth\(\)/);
  assert.match(page, /currentUser\?\.userId === userId/);
  assert.match(page, /shouldLoadUserManagementInsights/);
  assert.match(page, /useUserSkills\(\s*userId,\s*shouldLoadUserManagementInsights,?\s*\)/);
  assert.match(page, /useUserEvaluations\(\s*userId,\s*shouldLoadUserManagementInsights,?\s*\)/);
  assert.match(page, /useDepartmentList\(\s*shouldLoadUserManagementInsights,?\s*\)/);
  assert.match(page, /isSelfProfile=\{isSelfProfile\}/);

  assert.match(hooks, /useUserSkills\(userId: number,\s*enabled = true\)/);
  assert.match(hooks, /useUserEvaluations\(userId: number,\s*enabled = true\)/);
  assert.match(hooks, /enabled: enabled && isValidUserId\(userId\)/);

  assert.match(component, /isSelfProfile/);
  assert.match(component, /visibleTabs/);
  assert.match(component, /!isSelfProfile \|\| tab\.value === "profile"/);
  assert.match(component, /activeDetailTab/);
  assert.match(component, /isReadOnly=\{isSelfProfile\}/);
  assert.match(component, /<ReadOnlyTeamList[\s\S]*label="다른 소속 팀"[\s\S]*teams=\{otherTeams\}/);
  assert.match(component, /function ReadOnlyTeamList/);
  assert.match(component, /teams\.map\(\(team\) =>/);
  assert.match(component, /team\.isLeader \? \(/);
  assert.doesNotMatch(component, /formatOtherTeamNames/);
  assert.match(component, /className="flex flex-wrap gap-2"/);
  assert.doesNotMatch(
    component,
    /function ReadOnlyTeamList[\s\S]*min-h-14 rounded-2xl border border-border\/70 bg-muted\/35 px-4 py-3/,
  );
});
