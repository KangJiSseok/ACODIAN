import assert from "node:assert/strict";
import test from "node:test";
import {
  getAvailableDashboardAdminScopes,
  getDashboardRole,
  getDefaultDashboardScope,
} from "../src/app/(protected)/_dashboard/_utils/dashboardAccess.ts";

const baseUser = {
  userId: 101,
  userName: "테스트",
  email: "test@ax-wms.com",
  phone: null,
  departmentId: 2,
  departmentName: "솔루션사업부",
  positionName: "사원",
  titleName: null,
  joinDate: null,
  employmentStatus: "ACTIVE",
  profileImageUrl: null,
  teams: [
    {
      isPrimary: true,
      teamId: 114,
      teamName: "Siemens 스마트팩토리 장애 상담 요약 품질 TF",
      isLeader: false,
      teamRole: "팀원",
      allocation: null,
    },
  ],
};

test("director keeps department comparison as the default dashboard scope", () => {
  const director = {
    ...baseUser,
    positionName: "본부장",
    titleName: "경영총괄",
  };

  assert.equal(getDashboardRole(director), "DIRECTOR");
  assert.deepEqual(getDefaultDashboardScope(director), {
    view: "ADMIN",
    adminScope: "DEPARTMENT_COMPARISON",
  });
  assert.deepEqual(getAvailableDashboardAdminScopes("DIRECTOR"), [
    "DEPARTMENT_COMPARISON",
    "DEPARTMENT_DETAIL",
  ]);
});

test("department head defaults to own department detail and cannot use department comparison", () => {
  const departmentHead = {
    ...baseUser,
    positionName: "사업부장",
    titleName: "솔루션사업부장",
  };

  assert.equal(getDashboardRole(departmentHead), "DEPARTMENT_HEAD");
  assert.deepEqual(getDefaultDashboardScope(departmentHead), {
    view: "ADMIN",
    adminScope: "DEPARTMENT_DETAIL",
    departmentId: 2,
  });
  assert.deepEqual(getAvailableDashboardAdminScopes("DEPARTMENT_HEAD"), [
    "DEPARTMENT_DETAIL",
    "TEAM_DETAIL",
  ]);
});

test("member defaults to my dashboard when a primary team exists", () => {
  assert.equal(getDashboardRole(baseUser), "NONE");
  assert.deepEqual(getDefaultDashboardScope(baseUser), {
    view: "ME",
    teamId: 114,
  });
  assert.deepEqual(getAvailableDashboardAdminScopes("NONE"), []);
});
