"use client";

import { FormEvent, type ReactNode, useMemo, useRef, useState } from "react";
import { CalendarDays, ShieldCheck, UserPlus, X } from "lucide-react";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { useTeamUserCandidates } from "../_hooks";
import type {
  CreateTeamRequest,
  TeamDetail,
  TeamStatusCode,
  TeamUserSummary,
  UpdateTeamRequest,
} from "../_types/team.types";
import { getTeamStatusLabel } from "../_utils/teamStatus.utils";
import TeamMemberSearchDialog from "./teamMemberSearchDialog";
import type { TeamMemberSelection } from "./teamForm.utils";

interface TeamFormProps {
  initialTeam?: TeamDetail;
  initialUsers?: TeamUserSummary[];
  onSubmit: (payload: CreateTeamRequest | UpdateTeamRequest) => Promise<void>;
  submitLabel: string;
  isSubmitting: boolean;
}

const TEAM_ADMIN_TITLE_KEYWORDS = ["본부장", "사업부장"] as const;

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
  const [isMemberSearchOpen, setIsMemberSearchOpen] = useState(false);
  const [members, setMembers] = useState<TeamMemberSelection[]>(
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
        TEAM_ADMIN_TITLE_KEYWORDS.some((keyword) =>
          [candidate.titleName, candidate.positionName].some((value) =>
            value?.includes(keyword),
          ),
        ),
      ),
    [candidates],
  );
  function removeMember(userId: number) {
    setMembers((current) => current.filter((member) => member.userId !== userId));
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
                  <Select
                    value={statusCode}
                    onChange={(event) => setStatusCode(event.target.value)}
                    className={selectClassName}
                    options={[
                      { value: "ACTIVE", label: getTeamStatusLabel("ACTIVE") },
                      {
                        value: "INACTIVE",
                        label: getTeamStatusLabel("INACTIVE"),
                      },
                    ]}
                  />
                </Field>
                <DatePickerField
                  id="team-start-date"
                  label="시작일"
                  value={startDate}
                  onChange={setStartDate}
                  pickerLabel="시작일 선택 달력 열기"
                />
                <DatePickerField
                  id="team-expected-end-date"
                  label="종료 예정일"
                  value={expectedEndDate}
                  onChange={setExpectedEndDate}
                  pickerLabel="종료 예정일 선택 달력 열기"
                />
              </div>

              <Field label="팀 관리자">
                <Select
                  value={adminId}
                  onChange={(event) => setAdminId(event.target.value)}
                  className={selectClassName}
                  options={[
                    { value: "", label: "팀 관리자를 선택하세요" },
                    ...(adminCandidates.length > 0
                      ? adminCandidates
                      : candidates
                    ).map((candidate) => ({
                      value: String(candidate.userId),
                      label: `${candidate.userName} / ${
                        candidate.titleName ??
                        candidate.positionName ??
                        candidate.departmentName ??
                        "-"
                      }`,
                    })),
                  ]}
                />
              </Field>
            </div>

            <aside className="space-y-5 xl:border-l xl:border-border/70 xl:pl-8">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between xl:flex-col 2xl:flex-row">
                <div>
                  <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
                    MEMBERS
                  </p>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    사용자 검색/추가 모달에서 구성원을 선택한 뒤 팀장과 역할을
                    지정합니다.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsMemberSearchOpen(true)}
                  className="h-11 shrink-0 rounded-2xl px-4 font-semibold"
                >
                  <UserPlus className="size-4" />
                  사용자 검색/추가
                </Button>
              </div>

              <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold text-foreground">
                    추가된 구성원
                  </p>
                  <Badge variant="outline">{members.length}명</Badge>
                </div>
              </div>

              <div className="space-y-3">
                {members.length === 0 ? (
                  <div className="workspace-empty rounded-2xl px-4 py-6 text-center text-sm">
                    사용자 검색/추가 버튼을 눌러 구성원을 추가하세요.
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
                            {candidate?.positionName ??
                              initialUsers.find(
                                (user) => user.userId === member.userId,
                              )?.positionName ??
                              "-"}
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

      {isMemberSearchOpen ? (
        <TeamMemberSearchDialog
          open={isMemberSearchOpen}
          onOpenChange={setIsMemberSearchOpen}
          candidates={candidates}
          initialUsers={initialUsers}
          isLoading={isLoading}
          members={members}
          onSave={setMembers}
        />
      ) : null}
    </form>
  );
}

const inputClassName =
  "h-14 w-full rounded-2xl border border-input bg-background/70 px-4 text-base text-foreground shadow-sm outline-none transition placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/20";
const selectClassName = "h-11 rounded-2xl px-4 text-sm";

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

function DatePickerField({
  id,
  label,
  value,
  onChange,
  pickerLabel,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  pickerLabel: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  function openPicker() {
    const input = inputRef.current;

    if (!input) {
      return;
    }

    if (typeof input.showPicker === "function") {
      input.showPicker();
      return;
    }

    input.focus();
  }

  return (
    <div className="block space-y-3">
      <label
        htmlFor={id}
        className="inline-flex items-center gap-2 text-[15px] font-semibold text-foreground"
      >
        {label}
      </label>
      <div className="relative">
        <input
          ref={inputRef}
          id={id}
          type="date"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          className={cn(inputClassName, "pr-12")}
        />
        <button
          type="button"
          onClick={openPicker}
          className="absolute right-3 top-1/2 flex size-9 -translate-y-1/2 items-center justify-center rounded-xl text-muted-foreground transition hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
          aria-label={pickerLabel}
        >
          <CalendarDays className="size-4" />
        </button>
      </div>
    </div>
  );
}
