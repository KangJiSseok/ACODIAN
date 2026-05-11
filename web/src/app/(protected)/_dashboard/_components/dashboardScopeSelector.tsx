"use client";

import { useMemo, useState } from "react";
import { Check, SlidersHorizontal } from "lucide-react";
import type { AuthUserTeam } from "@/app/_common/store/auth.store";
import type { DepartmentSummary } from "@/app/(protected)/department/_types/department.types";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import type {
  DashboardAdminScope,
  DashboardRole,
  DashboardScopeSelection,
} from "../_types/dashboard.types";
import { getAvailableDashboardAdminScopes } from "../_utils/dashboardAccess";

export interface DashboardTeamOption {
  teamId: number;
  teamName: string;
}

interface DashboardScopeSelectorProps {
  value: DashboardScopeSelection;
  dashboardRole: DashboardRole;
  departments: DepartmentSummary[];
  adminTeams: DashboardTeamOption[];
  adminTeamsLoading?: boolean;
  teams: AuthUserTeam[];
  onChange: (value: DashboardScopeSelection) => void;
}

type DraftView = "ADMIN" | "ME";

export function DashboardScopeSelector({
  value,
  dashboardRole,
  departments,
  adminTeams,
  adminTeamsLoading = false,
  teams,
  onChange,
}: DashboardScopeSelectorProps) {
  const adminScopes = getAvailableDashboardAdminScopes(dashboardRole);
  const defaultAdminScope = adminScopes[0] ?? "DEPARTMENT_DETAIL";
  const [open, setOpen] = useState(false);
  const [draftView, setDraftView] = useState<DraftView>(value.view);
  const [draftAdminScope, setDraftAdminScope] = useState<DashboardAdminScope>(
    getInitialAdminScope(value, adminScopes, defaultAdminScope),
  );
  const [draftDepartmentId, setDraftDepartmentId] = useState<number | null>(
    getInitialDepartmentId(value, departments),
  );
  const [draftAdminTeamId, setDraftAdminTeamId] = useState<number | null>(
    getInitialAdminTeamId(value, adminTeams),
  );
  const [draftTeamId, setDraftTeamId] = useState<number | null>(
    value.view === "ME" ? value.teamId : getDefaultTeamId(teams),
  );

  const handleOpen = () => {
    setDraftView(value.view);
    setDraftAdminScope(
      getInitialAdminScope(value, adminScopes, defaultAdminScope),
    );
    setDraftDepartmentId(getInitialDepartmentId(value, departments));
    setDraftAdminTeamId(getInitialAdminTeamId(value, adminTeams));
    setDraftTeamId(value.view === "ME" ? value.teamId : getDefaultTeamId(teams));
    setOpen(true);
  };

  const departmentOptions = useMemo(
    () =>
      departments.map((department) => ({
        label: department.departmentName,
        value: String(department.departmentId),
      })),
    [departments],
  );

  const adminTeamOptions = useMemo(
    () =>
      adminTeams.map((team) => ({
        label: team.teamName,
        value: String(team.teamId),
      })),
    [adminTeams],
  );

  const teamOptions = useMemo(
    () =>
      teams.map((team) => ({
        label: `${team.teamName}${team.isPrimary ? " · 기본" : ""}`,
        value: String(team.teamId),
      })),
    [teams],
  );

  const adminScopeOptions = useMemo(
    () =>
      adminScopes.map((scope) => ({
        label: getAdminScopeOptionLabel(scope, dashboardRole),
        value: scope,
      })),
    [adminScopes, dashboardRole],
  );

  const selectedLabel = getScopeLabel(value, departments, adminTeams, teams);
  const isAdminTeamSelectDisabled =
    adminTeamsLoading || adminTeamOptions.length === 0;
  const isAdminTeamScopeDisabled =
    draftView === "ADMIN" &&
    draftAdminScope === "TEAM_DETAIL" &&
    isAdminTeamSelectDisabled;
  const canApply = isAdminTeamScopeDisabled
    ? false
    : canApplyDraft(
        draftView,
        draftAdminScope,
        draftDepartmentId,
        draftAdminTeamId,
        draftTeamId,
      );

  const handleApply = () => {
    if (!canApply) {
      return;
    }

    if (draftView === "ME") {
      onChange({
        view: "ME",
        teamId: draftTeamId as number,
      });
      setOpen(false);
      return;
    }

    if (draftAdminScope === "DEPARTMENT_DETAIL") {
      onChange({
        view: "ADMIN",
        adminScope: "DEPARTMENT_DETAIL",
        departmentId: draftDepartmentId as number,
      });
      setOpen(false);
      return;
    }

    if (draftAdminScope === "TEAM_DETAIL") {
      onChange({
        view: "ADMIN",
        adminScope: "TEAM_DETAIL",
        teamId: draftAdminTeamId as number,
      });
      setOpen(false);
      return;
    }

    onChange({
      view: "ADMIN",
      adminScope: "DEPARTMENT_COMPARISON",
    });
    setOpen(false);
  };

  return (
    <>
      <Button
        type="button"
        variant="outline"
        size="lg"
        className="h-12 justify-start gap-3 rounded-2xl px-5 text-left text-base font-semibold"
        onClick={handleOpen}
      >
        <SlidersHorizontal className="size-4" />
        <span>{selectedLabel}</span>
      </Button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>보기 기준 선택</DialogTitle>
            <DialogDescription>
              권한과 팀 역할에 따라 관리자 관점과 내 업무 관점을 전환합니다.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5">
            <div className="space-y-3">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                View
              </p>
              <div className="grid gap-3 sm:grid-cols-2">
                <ScopeChoiceButton
                  active={draftView === "ADMIN"}
                  title="관리자 관점"
                  description="권한 범위 안에서 조직 성과와 리스크를 비교합니다."
                  disabled={adminScopes.length === 0}
                  onClick={() => setDraftView("ADMIN")}
                />
                <ScopeChoiceButton
                  active={draftView === "ME"}
                  title="내 업무"
                  description="소속 팀 기준으로 내가 맡은 업무를 봅니다."
                  disabled={teams.length === 0}
                  onClick={() => setDraftView("ME")}
                />
              </div>
            </div>

            <div className="rounded-2xl border border-border bg-muted/20 p-4">
              {draftView === "ADMIN" ? (
                <div className="space-y-4">
                  <label className="block space-y-2">
                    <span className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                      Scope
                    </span>
                    <Select
                      value={draftAdminScope}
                      options={adminScopeOptions}
                      disabled={adminScopeOptions.length === 0}
                      onChange={(event) =>
                        setDraftAdminScope(
                          event.target.value as DashboardAdminScope,
                        )
                      }
                    />
                  </label>
                  {draftAdminScope === "DEPARTMENT_DETAIL" ? (
                    <label className="block space-y-2">
                      <span className="text-sm font-medium text-foreground">
                        비교할 부서
                      </span>
                      <Select
                        value={
                          draftDepartmentId === null
                            ? ""
                            : String(draftDepartmentId)
                        }
                        options={departmentOptions}
                        disabled={departmentOptions.length === 0}
                        onChange={(event) =>
                          setDraftDepartmentId(Number(event.target.value))
                        }
                      />
                    </label>
                  ) : null}
                  {draftAdminScope === "TEAM_DETAIL" ? (
                    <label className="block space-y-2">
                      <span className="text-sm font-medium text-foreground">
                        조회할 팀
                      </span>
                      <Select
                        value={
                          draftAdminTeamId === null
                            ? ""
                            : String(draftAdminTeamId)
                        }
                        options={adminTeamOptions}
                        disabled={isAdminTeamSelectDisabled}
                        onChange={(event) =>
                          setDraftAdminTeamId(Number(event.target.value))
                        }
                      />
                      {adminTeamsLoading ? (
                        <span className="block text-sm text-muted-foreground">
                          팀 목록을 불러오는 중입니다.
                        </span>
                      ) : null}
                      {!adminTeamsLoading && adminTeamOptions.length === 0 ? (
                        <span className="block text-sm text-muted-foreground">
                          조회 가능한 팀이 없습니다.
                        </span>
                      ) : null}
                    </label>
                  ) : null}
                </div>
              ) : (
                <label className="block space-y-2">
                  <span className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                    소속 팀
                  </span>
                  <Select
                    value={draftTeamId === null ? "" : String(draftTeamId)}
                    options={teamOptions}
                    disabled={teamOptions.length === 0}
                    onChange={(event) =>
                      setDraftTeamId(Number(event.target.value))
                    }
                  />
                  {teamOptions.length === 0 ? (
                    <span className="block text-sm text-muted-foreground">
                      현재 사용자에게 연결된 소속 팀이 없습니다.
                    </span>
                  ) : null}
                </label>
              )}
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>
              취소
            </Button>
            <Button type="button" disabled={!canApply} onClick={handleApply}>
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

function ScopeChoiceButton({
  active,
  title,
  description,
  disabled,
  onClick,
}: {
  active: boolean;
  title: string;
  description: string;
  disabled?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={cn(
        "flex min-h-28 items-start justify-between gap-4 rounded-2xl border px-4 py-4 text-left transition",
        active
          ? "border-primary bg-primary/10 text-foreground"
          : "border-border bg-background/60 text-foreground hover:border-primary/30 hover:bg-background",
        disabled ? "cursor-not-allowed opacity-50" : null,
      )}
      disabled={disabled}
      onClick={onClick}
    >
      <span>
        <span className="block text-base font-semibold">{title}</span>
        <span className="mt-2 block text-sm leading-6 text-muted-foreground">
          {description}
        </span>
      </span>
      {active ? <Check className="mt-0.5 size-4 shrink-0 text-primary" /> : null}
    </button>
  );
}

function getInitialAdminScope(
  value: DashboardScopeSelection,
  adminScopes: DashboardAdminScope[],
  fallback: DashboardAdminScope,
) {
  if (value.view === "ADMIN" && adminScopes.includes(value.adminScope)) {
    return value.adminScope;
  }

  return fallback;
}

function getInitialDepartmentId(
  value: DashboardScopeSelection,
  departments: DepartmentSummary[],
) {
  return value.view === "ADMIN" && value.adminScope === "DEPARTMENT_DETAIL"
    ? value.departmentId
    : departments[0]?.departmentId ?? null;
}

function getInitialAdminTeamId(
  value: DashboardScopeSelection,
  adminTeams: DashboardTeamOption[],
) {
  return value.view === "ADMIN" && value.adminScope === "TEAM_DETAIL"
    ? value.teamId
    : adminTeams[0]?.teamId ?? null;
}

function getDefaultTeamId(teams: AuthUserTeam[]) {
  return teams.find((team) => team.isPrimary)?.teamId ?? teams[0]?.teamId ?? null;
}

function canApplyDraft(
  draftView: DraftView,
  draftAdminScope: DashboardAdminScope,
  draftDepartmentId: number | null,
  draftAdminTeamId: number | null,
  draftTeamId: number | null,
) {
  if (draftView === "ME") {
    return draftTeamId !== null;
  }

  if (draftAdminScope === "DEPARTMENT_DETAIL") {
    return draftDepartmentId !== null;
  }

  if (draftAdminScope === "TEAM_DETAIL") {
    return draftAdminTeamId !== null;
  }

  return draftAdminScope === "DEPARTMENT_COMPARISON";
}

function getAdminScopeOptionLabel(
  scope: DashboardAdminScope,
  role: DashboardRole,
) {
  if (scope === "DEPARTMENT_COMPARISON") {
    return "전체 부서 비교";
  }

  if (scope === "DEPARTMENT_DETAIL") {
    return role === "DEPARTMENT_HEAD" ? "내 부서 팀간 비교" : "부서별 팀 비교";
  }

  return "단일팀 상세";
}

function getScopeLabel(
  value: DashboardScopeSelection,
  departments: DepartmentSummary[],
  adminTeams: DashboardTeamOption[],
  teams: AuthUserTeam[],
) {
  if (value.view === "ME") {
    const teamName =
      teams.find((team) => team.teamId === value.teamId)?.teamName ?? "소속 팀";
    return `내 업무 / ${teamName}`;
  }

  if (value.adminScope === "TEAM_DETAIL") {
    const teamName =
      adminTeams.find((team) => team.teamId === value.teamId)?.teamName ??
      "선택 팀";
    return `관리자 / ${teamName} 상세`;
  }

  if (value.adminScope === "DEPARTMENT_DETAIL") {
    const departmentName =
      departments.find(
        (department) => department.departmentId === value.departmentId,
      )?.departmentName ?? "선택 부서";
    return `관리자 / ${departmentName} 팀간 비교`;
  }

  return "관리자 / 전체 부서 비교";
}
