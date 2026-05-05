"use client";

import { FormEvent, type ReactNode, useMemo, useState } from "react";
import { Search, ShieldCheck, UserPlus, X } from "lucide-react";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { useTeamUserCandidates } from "../_hooks";
import type {
  CreateTeamRequest,
  TeamDetail,
  TeamStatusCode,
  TeamUserSummary,
  UpdateTeamRequest,
} from "../_types/team.types";

interface TeamFormProps {
  initialTeam?: TeamDetail;
  initialUsers?: TeamUserSummary[];
  onSubmit: (payload: CreateTeamRequest | UpdateTeamRequest) => Promise<void>;
  submitLabel: string;
  isSubmitting: boolean;
}

interface SelectedMember {
  userId: number;
  teamRole: string;
  isLeader: boolean;
}

export default function TeamForm({
  initialTeam,
  initialUsers = [],
  onSubmit,
  submitLabel,
  isSubmitting,
}: TeamFormProps) {
  const { data: candidates = [], isLoading } = useTeamUserCandidates();
  const [teamName, setTeamName] = useState(initialTeam?.teamName ?? "");
  const [description, setDescription] = useState(
    initialTeam?.description ?? "",
  );
  const [statusCode, setStatusCode] = useState<TeamStatusCode>(
    initialTeam?.statusCode ?? "ACTIVE",
  );
  const [startDate, setStartDate] = useState(initialTeam?.startDate ?? "");
  const [expectedEndDate, setExpectedEndDate] = useState(
    initialTeam?.expectedEndDate ?? "",
  );
  const [adminId, setAdminId] = useState(
    initialTeam?.deptHeadAdminUserId
      ? String(initialTeam.deptHeadAdminUserId)
      : "",
  );
  const [query, setQuery] = useState("");
  const [members, setMembers] = useState<SelectedMember[]>(
    initialUsers.map((user) => ({
      userId: user.userId,
      teamRole: user.teamRole ?? "",
      isLeader: user.isLeader,
    })),
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const candidateById = useMemo(
    () => new Map(candidates.map((candidate) => [candidate.userId, candidate])),
    [candidates],
  );
  const selectedIds = useMemo(
    () => new Set(members.map((member) => member.userId)),
    [members],
  );
  const adminCandidates = useMemo(
    () =>
      candidates.filter((candidate) =>
        ["본부장", "부서장", "이사"].some((keyword) =>
          [candidate.titleName, candidate.positionName].some((value) =>
            value?.includes(keyword),
          ),
        ),
      ),
    [candidates],
  );
  const filteredCandidates = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return candidates.filter((candidate) => {
      if (!normalizedQuery) return true;

      return [
        candidate.userName,
        candidate.email,
        candidate.departmentName,
        candidate.positionName,
        candidate.titleName,
      ]
        .join(" ")
        .toLowerCase()
        .includes(normalizedQuery);
    });
  }, [candidates, query]);

  function addMember(userId: number) {
    if (selectedIds.has(userId)) return;

    setMembers((current) => [
      ...current,
      { userId, teamRole: "", isLeader: current.length === 0 },
    ]);
  }

  function removeMember(userId: number) {
    setMembers((current) => {
      const next = current.filter((member) => member.userId !== userId);

      if (next.some((member) => member.isLeader) || next.length === 0) {
        return next;
      }

      return next.map((member, index) => ({
        ...member,
        isLeader: index === 0,
      }));
    });
  }

  function updateMemberRole(userId: number, teamRole: string) {
    setMembers((current) =>
      current.map((member) =>
        member.userId === userId ? { ...member, teamRole } : member,
      ),
    );
  }

  function selectLeader(userId: number) {
    setMembers((current) =>
      current.map((member) => ({
        ...member,
        isLeader: member.userId === userId,
      })),
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedName = teamName.trim();
    const normalizedMembers = members.map((member) => ({
      ...member,
      teamRole: member.teamRole.trim(),
    }));

    if (!normalizedName) {
      setErrorMessage("팀명을 입력해주세요.");
      return;
    }

    if (!adminId) {
      setErrorMessage("팀 관리자를 선택해주세요.");
      return;
    }

    if (normalizedMembers.length === 0) {
      setErrorMessage("팀 구성원을 한 명 이상 추가해주세요.");
      return;
    }

    if (!normalizedMembers.some((member) => member.isLeader)) {
      setErrorMessage("팀장을 지정해주세요.");
      return;
    }

    if (normalizedMembers.some((member) => !member.teamRole)) {
      setErrorMessage("모든 팀원의 팀 내 역할을 입력해주세요.");
      return;
    }

    setErrorMessage(null);

    try {
      if (!initialTeam) {
        await onSubmit({
          teamName: normalizedName,
          description: description.trim(),
          addAdmin: Number(adminId),
          addUsers: normalizedMembers,
          statusCode,
          startDate: startDate || null,
          expectedEndDate: expectedEndDate || null,
        });
        return;
      }

      const removeUsers = initialUsers
        .filter((user) => !selectedIds.has(user.userId))
        .map((user) => user.userId);
      const previousAdminId = initialTeam.deptHeadAdminUserId;
      const nextAdminId = Number(adminId);

      await onSubmit({
        teamName: normalizedName,
        description: description.trim(),
        addAdmin: previousAdminId === nextAdminId ? null : nextAdminId,
        removeAdmin: previousAdminId === nextAdminId ? null : previousAdminId,
        addUsers: normalizedMembers.map((member) => ({
          userId: member.userId,
          isLeader: member.isLeader,
          teamRole: member.teamRole,
        })),
        removeUsers,
        editUsers: [],
        statusCode,
        startDate: startDate || null,
        expectedEndDate: expectedEndDate || null,
      });
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(error, "팀 정보를 저장하지 못했습니다."),
      );
    }
  }

  return (
    <form
      className="registration-surface w-full max-w-[1480px] pb-10"
      onSubmit={handleSubmit}
    >
      <Card className="rounded-[28px]">
        <div className="space-y-7 p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">
                TEAM WORKFLOW
              </p>
              <h2 className="mt-2 text-[22px] font-semibold tracking-[-0.05em] text-foreground">
                팀 정보 및 운영 설정
              </h2>
            </div>
            <div className="flex size-11 items-center justify-center rounded-2xl border border-primary/20 bg-primary/10 text-primary">
              <ShieldCheck className="size-4" />
            </div>
          </div>

          <div className="grid gap-8 xl:grid-cols-[minmax(0,1.15fr)_minmax(420px,0.85fr)]">
            <div className="space-y-5">
              <Field label="팀명">
                <input
                  value={teamName}
                  onChange={(event) => setTeamName(event.target.value)}
                  className={inputClassName}
                  placeholder="팀명을 입력하세요"
                />
              </Field>

              <Field label="팀 설명">
                <textarea
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                  className={cn(inputClassName, "min-h-[160px] py-3")}
                  placeholder="팀의 목적과 운영 범위를 작성하세요"
                />
              </Field>

              <div className="grid gap-4 md:grid-cols-3">
                <Field label="상태">
                  <select
                    value={statusCode}
                    onChange={(event) => setStatusCode(event.target.value)}
                    className={inputClassName}
                  >
                    <option value="ACTIVE">운영중</option>
                    <option value="INACTIVE">비활성</option>
                  </select>
                </Field>
                <Field label="시작일">
                  <input
                    type="date"
                    value={startDate}
                    onChange={(event) => setStartDate(event.target.value)}
                    className={inputClassName}
                  />
                </Field>
                <Field label="종료 예정일">
                  <input
                    type="date"
                    value={expectedEndDate}
                    onChange={(event) =>
                      setExpectedEndDate(event.target.value)
                    }
                    className={inputClassName}
                  />
                </Field>
              </div>

              <Field label="팀 관리자">
                <select
                  value={adminId}
                  onChange={(event) => setAdminId(event.target.value)}
                  className={inputClassName}
                >
                  <option value="">팀 관리자를 선택하세요</option>
                  {(adminCandidates.length > 0
                    ? adminCandidates
                    : candidates
                  ).map((candidate) => (
                    <option key={candidate.userId} value={candidate.userId}>
                      {candidate.userName} /{" "}
                      {candidate.titleName ??
                        candidate.positionName ??
                        candidate.departmentName ??
                        "-"}
                    </option>
                  ))}
                </select>
              </Field>
            </div>

            <aside className="space-y-5 xl:border-l xl:border-border/70 xl:pl-8">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
                  MEMBERS
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  구성원을 추가하고 팀장을 한 명 지정합니다.
                </p>
              </div>

              <div className="relative">
                <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  className={cn(inputClassName, "pl-11")}
                  placeholder="이름, 이메일, 부서로 검색"
                />
              </div>

              <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
                {isLoading ? (
                  <div className="workspace-empty rounded-2xl px-4 py-6 text-center text-sm">
                    사용자 목록을 불러오는 중입니다.
                  </div>
                ) : null}
                {filteredCandidates.map((candidate) => (
                  <button
                    key={candidate.userId}
                    type="button"
                    onClick={() => addMember(candidate.userId)}
                    disabled={selectedIds.has(candidate.userId)}
                    className="flex w-full items-center justify-between rounded-2xl border border-border/70 bg-muted/20 px-4 py-3 text-left transition hover:bg-muted/40 disabled:cursor-not-allowed disabled:opacity-45"
                  >
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-semibold text-foreground">
                        {candidate.userName}
                      </span>
                      <span className="mt-1 block truncate text-xs text-muted-foreground">
                        {candidate.departmentName ?? "-"} ·{" "}
                        {candidate.positionName ?? "-"}
                      </span>
                    </span>
                    <UserPlus className="size-4 shrink-0 text-muted-foreground" />
                  </button>
                ))}
              </div>

              <div className="space-y-3">
                {members.length === 0 ? (
                  <div className="workspace-empty rounded-2xl px-4 py-6 text-center text-sm">
                    추가된 팀 구성원이 없습니다.
                  </div>
                ) : null}

                {members.map((member) => {
                  const candidate = candidateById.get(member.userId);
                  const name =
                    candidate?.userName ??
                    initialUsers.find((user) => user.userId === member.userId)
                      ?.userName ??
                    "사용자";

                  return (
                    <div
                      key={member.userId}
                      className="rounded-2xl border border-border/70 bg-background/55 p-4"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="font-semibold text-foreground">
                              {name}
                            </p>
                            {member.isLeader ? (
                              <Badge variant="default">팀장</Badge>
                            ) : null}
                          </div>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {candidate?.positionName ?? "-"}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeMember(member.userId)}
                          className="rounded-lg p-1 text-muted-foreground transition hover:bg-muted hover:text-foreground"
                          aria-label={`${name} 제거`}
                        >
                          <X className="size-4" />
                        </button>
                      </div>
                      <div className="mt-3 grid gap-2 md:grid-cols-[auto_minmax(0,1fr)] md:items-center">
                        <label className="inline-flex items-center gap-2 text-sm text-muted-foreground">
                          <input
                            type="radio"
                            checked={member.isLeader}
                            onChange={() => selectLeader(member.userId)}
                          />
                          팀장 지정
                        </label>
                        <input
                          value={member.teamRole}
                          onChange={(event) =>
                            updateMemberRole(member.userId, event.target.value)
                          }
                          className="h-10 rounded-xl border border-input bg-background/70 px-3 text-sm outline-none transition focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/20"
                          placeholder="팀 내 역할"
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </aside>
          </div>

          {errorMessage ? (
            <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-5 py-4 text-sm text-destructive">
              {errorMessage}
            </div>
          ) : null}

          <div className="flex justify-end">
            <Button
              type="submit"
              disabled={isSubmitting}
              className="h-12 min-w-[168px] rounded-2xl px-7 font-semibold !text-primary-foreground hover:!text-primary-foreground"
            >
              {isSubmitting ? "저장 중..." : submitLabel}
            </Button>
          </div>
        </div>
      </Card>
    </form>
  );
}

const inputClassName =
  "h-14 w-full rounded-2xl border border-input bg-background/70 px-4 text-base text-foreground shadow-sm outline-none transition placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/20";

function Field({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <label className="block space-y-3">
      <span className="inline-flex items-center gap-2 text-[15px] font-semibold text-foreground">
        {label}
      </span>
      {children}
    </label>
  );
}
