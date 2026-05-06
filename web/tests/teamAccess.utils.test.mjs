import assert from "node:assert/strict";
import test from "node:test";
import { canManageTeam } from "../src/app/(protected)/team/_utils/teamAccess.utils.ts";

const baseUser = {
  userId: 101,
  userName: "테스트",
  email: "test@ax-wms.com",
  phone: null,
  departmentId: 1,
  departmentName: "테스트본부",
  positionName: null,
  titleName: "팀원",
  joinDate: null,
  employmentStatus: "ACTIVE",
  profileImageUrl: null,
  teams: [],
};

test("director can manage every team from the existing profile title", () => {
  const director = { ...baseUser, userId: 999, titleName: "본부장" };

  assert.equal(canManageTeam(director, { deptHeadAdminUserId: 101 }), true);
  assert.equal(canManageTeam(director, { deptHeadAdminUserId: null }), true);
});

test("non-director can manage only teams where they are the displayed department head admin", () => {
  const departmentHead = { ...baseUser, userId: 101, titleName: "사업부장" };

  assert.equal(canManageTeam(departmentHead, { deptHeadAdminUserId: 101 }), true);
  assert.equal(canManageTeam(departmentHead, { deptHeadAdminUserId: 202 }), false);
  assert.equal(canManageTeam(null, { deptHeadAdminUserId: 101 }), false);
});
