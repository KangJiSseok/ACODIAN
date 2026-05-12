import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import vm from "node:vm";
import ts from "../node_modules/typescript/lib/typescript.js";

const userRoot = new URL("../src/app/(protected)/user/", import.meta.url);

function readUserFile(path) {
  return readFileSync(new URL(path, userRoot), "utf8");
}

function loadAccessUtils() {
  const source = readUserFile("_utils/userAccess.utils.ts");
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  }).outputText;
  const cjsModule = { exports: {} };

  vm.runInNewContext(transpiled, {
    exports: cjsModule.exports,
    module: cjsModule,
    require: (name) => {
      throw new Error(`Unexpected require: ${name}`);
    },
  });

  return cjsModule.exports;
}

function user(overrides = {}) {
  return {
    userId: 1,
    departmentId: 10,
    positionName: null,
    titleName: "팀원",
    ...overrides,
  };
}

test("user insight write access only allows lower roles in scope", () => {
  const {
    canUseUserDepartmentFilter,
    canWriteUserInsight,
    getUserRoleRank,
    resolveUserListDepartmentId,
  } = loadAccessUtils();
  const director = user({ userId: 1, titleName: "본부장" });
  const directorWithoutDepartment = user({
    userId: 8,
    departmentId: null,
    titleName: "본부장",
  });
  const departmentHead = user({ userId: 2, titleName: "사업부장" });
  const otherDepartmentHead = user({
    userId: 3,
    departmentId: 20,
    titleName: "사업부장",
  });
  const teamLead = user({ userId: 4, titleName: "팀장" });
  const member = user({ userId: 5, titleName: "사원" });
  const otherDepartmentMember = user({
    userId: 6,
    departmentId: 20,
    titleName: "사원",
  });

  assert.equal(getUserRoleRank(director), 4);
  assert.equal(getUserRoleRank(departmentHead), 3);
  assert.equal(getUserRoleRank(teamLead), 2);
  assert.equal(getUserRoleRank(member), 1);

  assert.equal(canWriteUserInsight(director, departmentHead), true);
  assert.equal(canWriteUserInsight(director, user({ userId: 7, titleName: "본부장" })), false);
  assert.equal(canWriteUserInsight(director, user({ userId: 1, titleName: "본부장" })), false);

  assert.equal(canWriteUserInsight(departmentHead, teamLead), true);
  assert.equal(canWriteUserInsight(departmentHead, member), true);
  assert.equal(canWriteUserInsight(departmentHead, otherDepartmentMember), false);
  assert.equal(canWriteUserInsight(departmentHead, otherDepartmentHead), false);
  assert.equal(canWriteUserInsight(teamLead, member), false);

  assert.equal(canUseUserDepartmentFilter(directorWithoutDepartment), true);
  assert.equal(canUseUserDepartmentFilter(departmentHead), false);
  assert.equal(
    resolveUserListDepartmentId(directorWithoutDepartment, "all"),
    null,
  );
  assert.equal(resolveUserListDepartmentId(directorWithoutDepartment, "12"), 12);
  assert.equal(resolveUserListDepartmentId(departmentHead, "all"), 10);
  assert.equal(resolveUserListDepartmentId(departmentHead, "12"), 10);
});

test("user detail gates skill and evaluation create forms by insight access", () => {
  const component = readUserFile("_components/userDetail.tsx");

  assert.match(component, /selectAuthUser/);
  assert.match(component, /useAuthStore\(selectAuthUser\)/);
  assert.match(component, /canWriteUserInsight\(currentUser, user\)/);
  assert.match(component, /<SkillsTab[\s\S]*canCreate=\{canCreateUserInsights\}/);
  assert.match(component, /<EvaluationsTab[\s\S]*canCreate=\{canCreateUserInsights\}/);
  assert.match(component, /disabled=\{!canCreate \|\| isSaving\}/);
  assert.match(component, /자신보다 낮은 직책/);
});
