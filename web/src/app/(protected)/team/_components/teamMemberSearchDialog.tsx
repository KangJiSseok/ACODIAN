"use client";

import { useMemo, useState } from "react";
import { Check, Search, SlidersHorizontal, UserPlus, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";
import type { TeamUserCandidate, TeamUserSummary } from "../_types/team.types";
import {
  ALL_MEMBER_FILTER_VALUE,
  addTeamMemberSelection,
  filterTeamUserCandidates,
  getCandidateRankName,
  type TeamMemberSelection,
} from "./teamForm.utils";

interface TeamMemberSearchDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  candidates: TeamUserCandidate[];
  initialUsers: TeamUserSummary[];
  isLoading: boolean;
  members: TeamMemberSelection[];
  onSave: (members: TeamMemberSelection[]) => void;
}

export default function TeamMemberSearchDialog({
  open,
  onOpenChange,
  candidates,
  initialUsers,
  isLoading,
  members,
  onSave,
}: TeamMemberSearchDialogProps) {
  const [query, setQuery] = useState("");
  const [departmentName, setDepartmentName] = useState(
    ALL_MEMBER_FILTER_VALUE,
  );
  const [rankName, setRankName] = useState(ALL_MEMBER_FILTER_VALUE);
  const [draftMembers, setDraftMembers] =
    useState<TeamMemberSelection[]>(members);

  const candidateById = useMemo(
    () => new Map(candidates.map((candidate) => [candidate.userId, candidate])),
    [candidates],
  );
  const initialUserById = useMemo(
    () => new Map(initialUsers.map((user) => [user.userId, user])),
    [initialUsers],
  );
  const selectedIds = useMemo(
    () => new Set(draftMembers.map((member) => member.userId)),
    [draftMembers],
  );
  const departmentOptions = useMemo(
    () =>
      getUniqueSortedValues(
        candidates.map((candidate) => candidate.departmentName),
      ),
    [candidates],
  );
  const rankOptions = useMemo(
    () =>
      getUniqueSortedValues(
        candidates.flatMap((candidate) => [
          candidate.titleName,
          candidate.positionName,
        ]),
      ),
    [candidates],
  );
  const filteredCandidates = useMemo(
    () =>
      filterTeamUserCandidates(candidates, {
        query,
        departmentName,
        rankName,
      }),
    [candidates, departmentName, query, rankName],
  );

  function addDraftMember(userId: number) {
    setDraftMembers((current) => addTeamMemberSelection(current, userId));
  }

  function removeDraftMember(userId: number) {
    setDraftMembers((current) =>
      current.filter((member) => member.userId !== userId),
    );
  }

  function saveDraftMembers() {
    onSave(draftMembers);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="workspace-panel max-h-[calc(100vh-3rem)] w-full max-w-[1160px] overflow-hidden rounded-[28px] border border-border/80 p-0"
        role="dialog"
        aria-modal="true"
      >
        <div className="flex max-h-[calc(100vh-3rem)] flex-col">
          <div className="border-b border-border/70 px-6 py-5 md:px-8">
            <DialogHeader className="mb-0 gap-2">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <DialogTitle className="text-xl font-semibold">
                    사용자 검색/추가
                  </DialogTitle>
                  <DialogDescription className="mt-2 text-base leading-7">
                    부서와 직급 기준으로 사용자를 좁힌 뒤, 이름으로 필요한
                    구성원을 검색하세요.
                  </DialogDescription>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => onOpenChange(false)}
                  aria-label="사용자 검색 모달 닫기"
                  className="rounded-xl"
                >
                  <X className="size-4" />
                </Button>
              </div>
            </DialogHeader>
          </div>

          <div className="grid min-h-0 flex-1 gap-5 overflow-y-auto p-6 md:p-8 xl:grid-cols-[minmax(0,1fr)_minmax(320px,0.52fr)]">
            <section className="min-w-0 space-y-5">
              <div className="grid gap-4 lg:grid-cols-[minmax(260px,1.3fr)_minmax(180px,0.7fr)_minmax(180px,0.7fr)]">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <input
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    className={cn(dialogInputClassName, "pl-11")}
                    placeholder="이름으로 검색"
                  />
                </div>
                <select
                  value={departmentName}
                  onChange={(event) => setDepartmentName(event.target.value)}
                  className={dialogInputClassName}
                  aria-label="부서 필터"
                >
                  <option value={ALL_MEMBER_FILTER_VALUE}>전체 부서</option>
                  {departmentOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
                <select
                  value={rankName}
                  onChange={(event) => setRankName(event.target.value)}
                  className={dialogInputClassName}
                  aria-label="직급 필터"
                >
                  <option value={ALL_MEMBER_FILTER_VALUE}>전체 직급</option>
                  {rankOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>

              <div className="rounded-2xl border border-border/70 bg-muted/15 p-4">
                <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-muted-foreground">
                  <SlidersHorizontal className="size-4" />
                  <span>검색 결과 {filteredCandidates.length}명</span>
                </div>

                <div className="max-h-[25rem] space-y-3 overflow-y-auto pr-1">
                  {isLoading ? (
                    <div className="workspace-empty rounded-2xl px-4 py-8 text-center text-sm">
                      사용자 목록을 불러오는 중입니다.
                    </div>
                  ) : null}

                  {!isLoading && filteredCandidates.length === 0 ? (
                    <div className="workspace-empty rounded-2xl px-4 py-8 text-center text-sm">
                      조건에 맞는 사용자가 없습니다.
                    </div>
                  ) : null}

                  {!isLoading
                    ? filteredCandidates.map((candidate) => {
                        const isSelected = selectedIds.has(candidate.userId);

                        return (
                          <button
                            key={candidate.userId}
                            type="button"
                            onClick={() => addDraftMember(candidate.userId)}
                            disabled={isSelected}
                            className="flex min-h-[88px] w-full items-center justify-between gap-4 rounded-2xl border border-border/70 bg-background/70 px-4 py-3 text-left shadow-sm transition hover:border-primary/30 hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            <span className="min-w-0">
                              <span className="flex flex-wrap items-center gap-2">
                                <span className="truncate text-base font-semibold text-foreground">
                                  {candidate.userName}
                                </span>
                                {candidate.positionName ? (
                                  <Badge variant="outline">
                                    {candidate.positionName}
                                  </Badge>
                                ) : null}
                                {getCandidateRankName(candidate) ? (
                                  <Badge variant="default">
                                    {getCandidateRankName(candidate)}
                                  </Badge>
                                ) : null}
                              </span>
                              <span className="mt-2 block truncate text-sm text-muted-foreground">
                                {candidate.departmentName ?? "-"} ·{" "}
                                {candidate.email}
                              </span>
                            </span>
                            <span
                              className={cn(
                                "flex size-10 shrink-0 items-center justify-center rounded-xl border",
                                isSelected
                                  ? "border-primary/25 bg-primary/10 text-primary"
                                  : "border-primary/20 bg-primary text-primary-foreground",
                              )}
                            >
                              {isSelected ? (
                                <Check className="size-4" />
                              ) : (
                                <UserPlus className="size-4" />
                              )}
                            </span>
                          </button>
                        );
                      })
                    : null}
                </div>
              </div>
            </section>

            <aside className="min-w-0 rounded-2xl border border-border/70 bg-background/45 p-5">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-base font-semibold text-foreground">
                  추가된 사용자
                </h3>
                <Badge variant="outline">{draftMembers.length}명</Badge>
              </div>

              <div className="mt-4 min-h-[16rem] space-y-3">
                {draftMembers.length === 0 ? (
                  <div className="workspace-empty rounded-2xl border border-dashed border-border/80 px-4 py-9 text-center text-sm">
                    추가 버튼을 누르면 여기에 이름이 쌓입니다.
                  </div>
                ) : null}

                {draftMembers.map((member) => {
                  const candidate = candidateById.get(member.userId);
                  const initialUser = initialUserById.get(member.userId);
                  const name =
                    candidate?.userName ?? initialUser?.userName ?? "사용자";
                  const positionName =
                    candidate?.positionName ?? initialUser?.positionName ?? "-";

                  return (
                    <div
                      key={member.userId}
                      className="flex items-center justify-between gap-3 rounded-2xl border border-border/70 bg-muted/20 px-4 py-3"
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-foreground">
                          {name}
                        </p>
                        <p className="mt-1 truncate text-xs text-muted-foreground">
                          {positionName}
                        </p>
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => removeDraftMember(member.userId)}
                        aria-label={`${name} 제거`}
                        className="rounded-xl"
                      >
                        <X className="size-4" />
                      </Button>
                    </div>
                  );
                })}
              </div>
            </aside>
          </div>

          <DialogFooter className="border-t border-border/70 px-6 py-5 md:px-8">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              className="h-11 min-w-28 rounded-2xl px-5"
            >
              취소
            </Button>
            <Button
              type="button"
              onClick={saveDraftMembers}
              className="h-11 min-w-36 rounded-2xl px-6 font-semibold !text-primary-foreground hover:!text-primary-foreground"
            >
              저장하기
            </Button>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>
  );
}

const dialogInputClassName =
  "h-14 w-full rounded-2xl border border-input bg-background/80 px-4 text-base text-foreground shadow-sm outline-none transition placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/20";

function getUniqueSortedValues(values: Array<string | null>) {
  return Array.from(new Set(values.filter((value): value is string => !!value)))
    .sort((first, second) => first.localeCompare(second, "ko"));
}
