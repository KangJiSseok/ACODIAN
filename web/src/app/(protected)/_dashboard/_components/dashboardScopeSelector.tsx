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
import type { DashboardScopeSelection } from "../_types/dashboard.types";

interface DashboardScopeSelectorProps {
  value: DashboardScopeSelection;
  departments: DepartmentSummary[];
  teams: AuthUserTeam[];
  onChange: (value: DashboardScopeSelection) => void;
}

type DraftView = "ADMIN" | "ME";
type DraftAdminScope = "DEPARTMENT_COMPARISON" | "DEPARTMENT_DETAIL";

export function DashboardScopeSelector({
  value,
  departments,
  teams,
  onChange,
}: DashboardScopeSelectorProps) {
  const [open, setOpen] = useState(false);
  const [draftView, setDraftView] = useState<DraftView>(value.view);
  const [draftAdminScope, setDraftAdminScope] = useState<DraftAdminScope>(
    value.view === "ADMIN" ? value.scope : "DEPARTMENT_COMPARISON",
  );
  const [draftDepartmentId, setDraftDepartmentId] = useState<number | null>(
    value.view === "ADMIN" && value.scope === "DEPARTMENT_DETAIL"
      ? value.departmentId
      : departments[0]?.departmentId ?? null,
  );
  const [draftTeamId, setDraftTeamId] = useState<number | null>(
    value.view === "ME" ? value.teamId : getDefaultTeamId(teams),
  );

  // 모달 안의 draft 값은 저장 전까지 실제 대시보드 조회 조건에 반영하지 않습니다.
  const handleOpen = () => {
    setDraftView(value.view);
    setDraftAdminScope(
      value.view === "ADMIN" ? value.scope : "DEPARTMENT_COMPARISON",
    );
    setDraftDepartmentId(
      value.view === "ADMIN" && value.scope === "DEPARTMENT_DETAIL"
        ? value.departmentId
        : departments[0]?.departmentId ?? null,
    );
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

  const teamOptions = useMemo(
    () =>
      teams.map((team) => ({
        label: `${team.teamName}${team.isPrimary ? " · 기본" : ""}`,
        value: String(team.teamId),
      })),
    [teams],
  );

  const selectedLabel = getScopeLabel(value, departments, teams);
  const canApply =
    draftView === "ADMIN"
      ? draftAdminScope === "DEPARTMENT_COMPARISON" ||
        draftDepartmentId !== null
      : draftTeamId !== null;

  const handleApply = () => {
    if (!canApply) {
      return;
    }

    // 백엔드가 요구하는 scope별 필수 파라미터만 선택 결과에 포함합니다.
    if (draftView === "ME") {
      onChange({
        view: "ME",
        scope: "ME",
        teamId: draftTeamId as number,
      });
      setOpen(false);
      return;
    }

    if (draftAdminScope === "DEPARTMENT_DETAIL") {
      onChange({
        view: "ADMIN",
        scope: "DEPARTMENT_DETAIL",
        departmentId: draftDepartmentId as number,
      });
      setOpen(false);
      return;
    }

    onChange({
      view: "ADMIN",
      scope: "DEPARTMENT_COMPARISON",
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
                  description="본부장 관점으로 조직 간 성과와 리스크를 비교합니다."
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
                      options={[
                        {
                          label: "전체 부서 비교",
                          value: "DEPARTMENT_COMPARISON",
                        },
                        {
                          label: "부서 내 팀 비교",
                          value: "DEPARTMENT_DETAIL",
                        },
                      ]}
                      onChange={(event) =>
                        setDraftAdminScope(
                          event.target.value as DraftAdminScope,
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

function getDefaultTeamId(teams: AuthUserTeam[]) {
  return teams.find((team) => team.isPrimary)?.teamId ?? teams[0]?.teamId ?? null;
}

function getScopeLabel(
  value: DashboardScopeSelection,
  departments: DepartmentSummary[],
  teams: AuthUserTeam[],
) {
  if (value.view === "ME") {
    const teamName =
      teams.find((team) => team.teamId === value.teamId)?.teamName ?? "소속 팀";
    return `내 업무 / ${teamName}`;
  }

  if (value.scope === "DEPARTMENT_DETAIL") {
    const departmentName =
      departments.find(
        (department) => department.departmentId === value.departmentId,
      )?.departmentName ?? "선택 부서";
    return `관리자 / ${departmentName} 팀 비교`;
  }

  return "관리자 / 전체 부서 비교";
}
