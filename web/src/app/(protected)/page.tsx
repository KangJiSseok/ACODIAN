"use client";

import { useMemo, useState } from "react";
import ScaffoldPage from "@/app/_common/components/layout/scaffoldPage";
import { useAuth } from "@/app/_common/hooks/useAuth";
import type { AuthUser, AuthUserTeam } from "@/app/_common/store/auth.store";
import type { DepartmentSummary } from "./department/_types/department.types";
import { useDepartmentList } from "./department/_hooks/useDepartmentList";
import {
  DepartmentDashboardView,
  DirectorDashboardError,
  DirectorDashboardLoading,
  DirectorDashboardView,
  MyDashboardView,
  TeamDashboardView,
} from "./_dashboard/_components/directorDashboard";
import {
  DashboardScopeSelector,
  type DashboardTeamOption,
} from "./_dashboard/_components/dashboardScopeSelector";
import { DashboardShortcutGrid } from "./_dashboard/_components/dashboardShortcutGrid";
import {
  useDepartmentDashboard,
  useDirectorDashboard,
  useMyDashboard,
  useTeamDashboard,
} from "./_dashboard/_hooks/useDirectorDashboard";
import type {
  DepartmentDashboard,
  DashboardRole,
  DashboardScopeSelection,
} from "./_dashboard/_types/dashboard.types";
import {
  getDashboardRole,
  getDefaultDashboardScope,
  isDashboardScopeAllowedForRole,
} from "./_dashboard/_utils/dashboardAccess";

export default function DashboardPage() {
  const { user } = useAuth();
  const teams = user?.teams ?? [];
  const dashboardRole = getDashboardRole(user);
  const isDirector = dashboardRole === "DIRECTOR";
  const isDepartmentHead = dashboardRole === "DEPARTMENT_HEAD";
  const isDashboardAdmin = dashboardRole !== "NONE";
  const hasTeamDashboard = teams.length > 0;
  const canUseDashboard = isDashboardAdmin || hasTeamDashboard;
  const defaultScope = useMemo(() => getDefaultDashboardScope(user), [user]);
  const [selectedScope, setSelectedScope] =
    useState<DashboardScopeSelection | null>(null);
  const scopeSelection =
    selectedScope &&
    isDashboardScopeAllowedForRole(selectedScope, dashboardRole)
      ? selectedScope
      : defaultScope;

  const { data: departmentList } = useDepartmentList(isDirector);
  const departments = useMemo(
    () =>
      isDirector
        ? departmentList?.departments ?? []
        : getCurrentDepartmentOption(user),
    [departmentList?.departments, isDirector, user],
  );

  const selectedDepartmentId =
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "DEPARTMENT_DETAIL"
      ? scopeSelection.departmentId
      : undefined;
  const departmentDashboardId =
    isDepartmentHead && Number.isFinite(user?.departmentId)
      ? (user?.departmentId as number)
      : selectedDepartmentId;
  const selectedTeamDetailId =
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "TEAM_DETAIL"
      ? scopeSelection.teamId
      : undefined;
  const selectedTeamId =
    scopeSelection.view === "ME" ? scopeSelection.teamId : undefined;

  const isDepartmentComparisonSelected =
    isDirector &&
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "DEPARTMENT_COMPARISON";
  const isDepartmentDetailSelected =
    isDashboardAdmin &&
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "DEPARTMENT_DETAIL";
  const isTeamDetailSelected =
    isDashboardAdmin &&
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "TEAM_DETAIL";
  const isMyDashboardSelected =
    canUseDashboard && scopeSelection.view === "ME";

  const directorDashboardQuery = useDirectorDashboard(
    isDepartmentComparisonSelected,
  );
  const departmentDashboardQuery = useDepartmentDashboard(
    departmentDashboardId,
    isDepartmentDetailSelected || isDepartmentHead,
  );
  const teamDashboardQuery = useTeamDashboard(
    selectedTeamDetailId,
    isTeamDetailSelected,
  );
  const myDashboardQuery = useMyDashboard(
    selectedTeamId,
    isMyDashboardSelected,
  );
  const adminTeams = useMemo(
    () =>
      isDepartmentHead
        ? getDepartmentDashboardTeamOptions(departmentDashboardQuery.data)
        : [],
    [isDepartmentHead, departmentDashboardQuery.data],
  );
  const adminTeamsLoading = isDepartmentHead && departmentDashboardQuery.isLoading;

  const isLoading =
    (isDepartmentComparisonSelected && directorDashboardQuery.isLoading) ||
    (isDepartmentDetailSelected && departmentDashboardQuery.isLoading) ||
    (isTeamDetailSelected && teamDashboardQuery.isLoading) ||
    (isMyDashboardSelected && myDashboardQuery.isLoading);
  const hasError =
    (isDepartmentComparisonSelected && directorDashboardQuery.error) ||
    (isDepartmentDetailSelected && departmentDashboardQuery.error) ||
    (isTeamDetailSelected && teamDashboardQuery.error) ||
    (isMyDashboardSelected && myDashboardQuery.error);
  const dashboardTitle = getDashboardTitle(dashboardRole, hasTeamDashboard);
  const dashboardIntroDescription = canUseDashboard
    ? getDashboardIntroDescription(
        scopeSelection,
        departments,
        adminTeams,
        teams,
        dashboardRole,
      )
    : "도메인별 라우트와 기본 레이아웃이 연결된 상태입니다.";

  return (
    <ScaffoldPage
      title={dashboardTitle}
      description=""
      contentVariant={canUseDashboard ? "plain" : "panel"}
    >
      <section className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div className="min-w-0 space-y-2">
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            {dashboardTitle}
          </h2>
          <p className="max-w-3xl text-sm leading-6 text-muted-foreground">
            {dashboardIntroDescription}
          </p>
        </div>
        {canUseDashboard ? (
          <div className="flex shrink-0 md:justify-end">
            <DashboardScopeSelector
              value={scopeSelection}
              dashboardRole={dashboardRole}
              departments={departments}
              adminTeams={adminTeams}
              adminTeamsLoading={adminTeamsLoading}
              teams={teams}
              onChange={setSelectedScope}
            />
          </div>
        ) : null}
      </section>

      {!canUseDashboard ? <DashboardShortcutGrid user={user} /> : null}

      {canUseDashboard && isLoading ? <DirectorDashboardLoading /> : null}
      {canUseDashboard && hasError ? <DirectorDashboardError /> : null}
      {isDepartmentComparisonSelected && directorDashboardQuery.data ? (
        <DirectorDashboardView dashboard={directorDashboardQuery.data} />
      ) : null}
      {isDepartmentDetailSelected && departmentDashboardQuery.data ? (
        <DepartmentDashboardView dashboard={departmentDashboardQuery.data} />
      ) : null}
      {isTeamDetailSelected && teamDashboardQuery.data ? (
        <TeamDashboardView dashboard={teamDashboardQuery.data} />
      ) : null}
      {isMyDashboardSelected && myDashboardQuery.data ? (
        <MyDashboardView dashboard={myDashboardQuery.data} />
      ) : null}
    </ScaffoldPage>
  );
}

function getDashboardTitle(role: DashboardRole, hasTeamDashboard: boolean) {
  if (role === "NONE" && !hasTeamDashboard) {
    return "ACODIAN 대시보드";
  }

  return "업무 대시보드";
}

function getCurrentDepartmentOption(user: AuthUser | null | undefined) {
  if (!Number.isFinite(user?.departmentId)) {
    return [];
  }

  return [
    {
      departmentId: user?.departmentId as number,
      departmentName: user?.departmentName ?? "내 부서",
      description: null,
      departmentHeadUserId: user?.userId ?? null,
      departmentHeadUserName: user?.userName ?? null,
      createdAt: "",
      updatedAt: "",
    },
  ];
}

function getDashboardDescription(
  value: DashboardScopeSelection,
  departments: DepartmentSummary[],
  adminTeams: DashboardTeamOption[],
  teams: AuthUserTeam[],
  role: DashboardRole,
) {
  if (value.view === "ME") {
    const teamName =
      teams.find((team) => team.teamId === value.teamId)?.teamName ?? "소속 팀";
    return `내 업무 · ${teamName} 기준`;
  }

  if (value.adminScope === "TEAM_DETAIL") {
    const teamName =
      adminTeams.find((team) => team.teamId === value.teamId)?.teamName ??
      "선택 팀";
    return `관리자 관점 · ${teamName} 상세 기준`;
  }

  if (value.adminScope === "DEPARTMENT_DETAIL") {
    const departmentName =
      departments.find(
        (department) => department.departmentId === value.departmentId,
      )?.departmentName ?? "선택 부서";
    const scopeLabel =
      role === "DEPARTMENT_HEAD" ? "내 부서 팀간 비교" : "팀간 비교";
    return `관리자 관점 · ${departmentName} ${scopeLabel} 기준`;
  }

  return "관리자 관점 · 전체 부서 비교 기준";
}

function getDashboardIntroDescription(
  value: DashboardScopeSelection,
  departments: DepartmentSummary[],
  adminTeams: DashboardTeamOption[],
  teams: AuthUserTeam[],
  role: DashboardRole,
) {
  return `${getDashboardDescription(
    value,
    departments,
    adminTeams,
    teams,
    role,
  )}으로 업무 현황과 완료율, 마감 임박 업무를 확인합니다.`;
}

function getDepartmentDashboardTeamOptions(
  dashboard: DepartmentDashboard | undefined,
): DashboardTeamOption[] {
  if (!dashboard) {
    return [];
  }

  const teamsById = new Map<number, string>();

  dashboard.teamCompletionRates.forEach((team) => {
    teamsById.set(team.teamId, team.teamName);
  });
  dashboard.teamWorkload.forEach((team) => {
    teamsById.set(team.teamId, team.teamName);
  });

  return Array.from(teamsById, ([teamId, teamName]) => ({ teamId, teamName }));
}
