import assert from "node:assert/strict";
import test from "node:test";
import {
  canCreateTeams,
  canAccessOrganizationPath,
  canManageDepartments,
  canManageUsers,
  canViewTeams,
  filterNavItemsForUser,
  isDepartmentHeadProfile,
  isDirectorProfile,
} from "../src/app/_common/utils/organizationAccess.utils.ts";

const baseUser = {
  userId: 101,
  userName: "테스트",
  email: "test@ax-wms.com",
  phone: null,
  departmentId: 1,
  departmentName: "테스트본부",
  positionName: "사원",
  titleName: null,
  joinDate: null,
  employmentStatus: "ACTIVE",
  profileImageUrl: null,
  teams: [],
};

const organizationNav = [
  { label: "대시보드", href: "/" },
  {
    label: "조직",
    submenus: [
      { label: "부서 관리", href: "/department", exact: true },
      { label: "팀 관리", href: "/team", exact: true },
      { label: "사용자 관리", href: "/user", exact: true },
    ],
  },
];

test("본부장은 현재 users/me 직책 필드 조합으로 모든 조직 메뉴를 사용할 수 있다", () => {
  const director = {
    ...baseUser,
    positionName: "본부장",
    titleName: "경영총괄",
  };

  assert.equal(isDirectorProfile(director), true);
  assert.equal(canManageDepartments(director), true);
  assert.equal(canManageUsers(director), true);
  assert.equal(canCreateTeams(director), true);
  assert.equal(canViewTeams(director), true);

  const visibleNav = filterNavItemsForUser(director, organizationNav);
  assert.deepEqual(visibleNav[1].submenus.map((item) => item.href), [
    "/department",
    "/team",
    "/user",
  ]);
});

test("사업부장은 부서 관리 없이 팀 관리와 사용자 관리만 볼 수 있다", () => {
  const departmentHead = {
    ...baseUser,
    positionName: "사업부장",
    titleName: "솔루션사업부장",
  };

  assert.equal(isDepartmentHeadProfile(departmentHead), true);
  assert.equal(canManageDepartments(departmentHead), false);
  assert.equal(canManageUsers(departmentHead), true);
  assert.equal(canCreateTeams(departmentHead), true);
  assert.equal(canViewTeams(departmentHead), true);

  const visibleNav = filterNavItemsForUser(departmentHead, organizationNav);
  assert.deepEqual(visibleNav[1].submenus.map((item) => item.href), [
    "/team",
    "/user",
  ]);
});

test("팀장과 팀원은 팀 관리 조회 메뉴만 볼 수 있다", () => {
  const teamLead = { ...baseUser, titleName: "팀장" };
  const member = { ...baseUser, titleName: "팀원" };

  for (const user of [teamLead, member]) {
    assert.equal(canManageDepartments(user), false);
    assert.equal(canManageUsers(user), false);
    assert.equal(canCreateTeams(user), false);
    assert.equal(canViewTeams(user), true);

    const visibleNav = filterNavItemsForUser(user, organizationNav);
    assert.deepEqual(visibleNav[1].submenus.map((item) => item.href), [
      "/team",
    ]);
  }
});

test("조직 관리 경로 접근 가능 여부도 직책 기준으로 계산한다", () => {
  const departmentHead = {
    ...baseUser,
    positionName: "사업부장",
    titleName: "솔루션사업부장",
  };
  const teamLead = { ...baseUser, titleName: "팀장" };

  assert.equal(canAccessOrganizationPath(departmentHead, "/department"), false);
  assert.equal(canAccessOrganizationPath(departmentHead, "/team"), true);
  assert.equal(canAccessOrganizationPath(departmentHead, "/user"), true);

  assert.equal(canAccessOrganizationPath(teamLead, "/department"), false);
  assert.equal(canAccessOrganizationPath(teamLead, "/team"), true);
  assert.equal(canAccessOrganizationPath(teamLead, "/user"), false);
  assert.equal(canAccessOrganizationPath(teamLead, "/worklog"), true);
});
