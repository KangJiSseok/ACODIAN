import type { AuthUser, AuthUserTeam } from "@/app/_common/store/auth.store";
import type {
  DashboardAdminScope,
  DashboardRole,
  DashboardScopeSelection,
} from "../_types/dashboard.types";

const DIRECTOR_KEYWORD = "본부장";
const DEPARTMENT_HEAD_KEYWORD = "사업부장";

function profileTexts(user: AuthUser | null | undefined) {
  return [user?.titleName, user?.positionName].filter(Boolean) as string[];
}

export function getDashboardRole(user: AuthUser | null | undefined): DashboardRole {
  const texts = profileTexts(user);

  if (texts.some((text) => text.includes(DIRECTOR_KEYWORD))) {
    return "DIRECTOR";
  }

  if (texts.some((text) => text.includes(DEPARTMENT_HEAD_KEYWORD))) {
    return "DEPARTMENT_HEAD";
  }

  return "NONE";
}

export function getAvailableDashboardAdminScopes(
  role: DashboardRole,
): DashboardAdminScope[] {
  if (role === "DIRECTOR") {
    return ["DEPARTMENT_COMPARISON", "DEPARTMENT_DETAIL"];
  }

  if (role === "DEPARTMENT_HEAD") {
    return ["DEPARTMENT_DETAIL", "TEAM_DETAIL"];
  }

  return [];
}

export function isDashboardScopeAllowedForRole(
  value: DashboardScopeSelection,
  role: DashboardRole,
) {
  if (value.view === "ME") {
    return role !== "NONE";
  }

  return getAvailableDashboardAdminScopes(role).includes(value.adminScope);
}

export function getDefaultDashboardScope(
  user: AuthUser | null | undefined,
): DashboardScopeSelection {
  const role = getDashboardRole(user);

  if (role === "DIRECTOR") {
    return {
      view: "ADMIN",
      adminScope: "DEPARTMENT_COMPARISON",
    };
  }

  if (role === "DEPARTMENT_HEAD" && Number.isFinite(user?.departmentId)) {
    return {
      view: "ADMIN",
      adminScope: "DEPARTMENT_DETAIL",
      departmentId: user?.departmentId as number,
    };
  }

  const defaultTeamId = getDefaultDashboardTeamId(user?.teams ?? []);
  if (Number.isFinite(defaultTeamId)) {
    return {
      view: "ME",
      teamId: defaultTeamId as number,
    };
  }

  return {
    view: "ADMIN",
    adminScope: "DEPARTMENT_COMPARISON",
  };
}

export function getDefaultDashboardTeamId(teams: AuthUserTeam[]) {
  return teams.find((team) => team.isPrimary)?.teamId ?? teams[0]?.teamId ?? null;
}
