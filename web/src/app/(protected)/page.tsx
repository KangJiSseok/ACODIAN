"use client";

import { useState } from "react";
import ScaffoldPage from "@/app/_common/components/layout/scaffoldPage";
import { useAuth } from "@/app/_common/hooks/useAuth";
import type { AuthUserTeam } from "@/app/_common/store/auth.store";
import { isDirectorProfile } from "@/app/_common/utils/organizationAccess.utils";
import type { DepartmentSummary } from "./department/_types/department.types";
import { useDepartmentList } from "./department/_hooks/useDepartmentList";
import {
  DepartmentDashboardView,
  DirectorDashboardError,
  DirectorDashboardLoading,
  DirectorDashboardView,
  MyDashboardView,
} from "./_dashboard/_components/directorDashboard";
import { DashboardScopeSelector } from "./_dashboard/_components/dashboardScopeSelector";
import { DashboardShortcutGrid } from "./_dashboard/_components/dashboardShortcutGrid";
import {
  useDepartmentDashboard,
  useDirectorDashboard,
  useMyDashboard,
} from "./_dashboard/_hooks/useDirectorDashboard";
import type { DashboardScopeSelection } from "./_dashboard/_types/dashboard.types";

// 본부장 대시보드 초기 보기 상태 정의 상수
const DEFAULT_DIRECTOR_SCOPE: DashboardScopeSelection = {
  view: "ADMIN",
  adminScope: "DEPARTMENT_COMPARISON",
};

export default function DashboardPage() {
  const { user } = useAuth();
  const isDirector = isDirectorProfile(user);
  const teams = user?.teams ?? [];
  const [scopeSelection, setScopeSelection] = useState<DashboardScopeSelection>(
    DEFAULT_DIRECTOR_SCOPE,
  );

  const { data: departmentList } = useDepartmentList(isDirector);
  const departments = departmentList?.departments ?? [];
  const selectedDepartmentId =
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "DEPARTMENT_DETAIL"
      ? scopeSelection.departmentId
      : undefined;
  const selectedTeamId =
    scopeSelection.view === "ME" ? scopeSelection.teamId : undefined;

  // 기존 단일 조회(useDirectorDashboard(isDirector))에서 scope별 enabled 플래그로 분리합니다.
  // 선택되지 않은 scope의 API는 호출하지 않아 전환 시 필요한 데이터만 가져옵니다.
  const isDepartmentComparisonSelected =
    isDirector &&
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "DEPARTMENT_COMPARISON";
  const isDepartmentDetailSelected =
    isDirector &&
    scopeSelection.view === "ADMIN" &&
    scopeSelection.adminScope === "DEPARTMENT_DETAIL";
  const isMyDashboardSelected = isDirector && scopeSelection.view === "ME";

  const directorDashboardQuery = useDirectorDashboard(
    isDepartmentComparisonSelected,
  );
  const departmentDashboardQuery = useDepartmentDashboard(
    selectedDepartmentId,
    isDepartmentDetailSelected,
  );
  const myDashboardQuery = useMyDashboard(
    selectedTeamId,
    isMyDashboardSelected,
  );

  const isLoading =
    (isDepartmentComparisonSelected && directorDashboardQuery.isLoading) ||
    (isDepartmentDetailSelected && departmentDashboardQuery.isLoading) ||
    (isMyDashboardSelected && myDashboardQuery.isLoading);
  const hasError =
    (isDepartmentComparisonSelected && directorDashboardQuery.error) ||
    (isDepartmentDetailSelected && departmentDashboardQuery.error) ||
    (isMyDashboardSelected && myDashboardQuery.error);

  return (
    <ScaffoldPage
      title={isDirector ? "본부장 대시보드" : "AX-WMS 대시보드"}
      description={
        isDirector
          ? getDashboardDescription(scopeSelection, departments, teams)
          : "도메인별 라우트와 기본 레이아웃이 연결된 상태입니다."
      }
      actions={
        isDirector ? (
          <DashboardScopeSelector
            value={scopeSelection}
            departments={departments}
            teams={teams}
            onChange={setScopeSelection}
          />
        ) : undefined
      }
      contentVariant={isDirector ? "plain" : "panel"}
    >
      {!isDirector ? <DashboardShortcutGrid user={user} /> : null}

      {isDirector && isLoading ? <DirectorDashboardLoading /> : null}
      {isDirector && hasError ? <DirectorDashboardError /> : null}
      {isDepartmentComparisonSelected && directorDashboardQuery.data ? (
        <DirectorDashboardView dashboard={directorDashboardQuery.data} />
      ) : null}
      {isDepartmentDetailSelected && departmentDashboardQuery.data ? (
        <DepartmentDashboardView dashboard={departmentDashboardQuery.data} />
      ) : null}
      {isMyDashboardSelected && myDashboardQuery.data ? (
        <MyDashboardView dashboard={myDashboardQuery.data} />
      ) : null}
    </ScaffoldPage>
  );
}

function getDashboardDescription(
  value: DashboardScopeSelection,
  departments: DepartmentSummary[],
  teams: AuthUserTeam[],
) {
  if (value.view === "ME") {
    const teamName =
      teams.find((team) => team.teamId === value.teamId)?.teamName ?? "소속 팀";
    return `내 업무 · ${teamName} 기준 - 구성원 관점으로 내가 맡은 업무와 리스크를 확인합니다.`;
  }

  if (value.adminScope === "DEPARTMENT_DETAIL") {
    const departmentName =
      departments.find(
        (department) => department.departmentId === value.departmentId,
      )?.departmentName ?? "선택 부서";
    return `관리자 관점 · ${departmentName} 팀간 비교 기준 - 선택 부서 범위에서 팀 간 편차, 리스크, AI 파이프라인 건강도를 비교합니다.`;
  }

  return "관리자 관점 · 전체 부서 비교 기준 - 전사 범위에서 부서 간 편차, 리스크, AI 파이프라인 건강도를 비교합니다.";
}
