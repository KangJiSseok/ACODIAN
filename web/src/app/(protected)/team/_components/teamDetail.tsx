"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  CalendarDays,
  Crown,
  Pencil,
  ShieldCheck,
  Trash2,
  UserRound,
  Users,
} from "lucide-react";
import { useAuth } from "@/app/_common/hooks/useAuth";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useDeleteTeam } from "../_hooks";
import type {
  TeamDetail as TeamDetailType,
  TeamStatusCode,
  TeamUserSummary,
} from "../_types/team.types";
import { canManageTeam } from "../_utils/teamAccess.utils";
import { getTeamStatusLabel } from "../_utils/teamStatus.utils";

export default function TeamDetail({
  team,
  users,
}: {
  team: TeamDetailType;
  users: TeamUserSummary[];
}) {
  const router = useRouter();
  const { user } = useAuth();
  const canEdit = canManageTeam(user, team);
  const deleteTeam = useDeleteTeam();

  async function handleDelete() {
    if (!confirm(`${team.teamName} 팀을 삭제하시겠습니까?`)) {
      return;
    }

    try {
      await deleteTeam.mutateAsync(team.teamId);
      router.push("/team");
    } catch (deleteError) {
      alert(getApiErrorMessage(deleteError, "팀 삭제에 실패했습니다."));
    }
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button asChild variant="outline" className="h-10">
          <Link href="/team">
            <ArrowLeft className="size-4" />
            팀 목록
          </Link>
        </Button>
        <div className="flex flex-wrap justify-end gap-3">
          {canEdit ? (
            <Button
              asChild
              variant="default"
              className="h-10 min-w-28 !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
            >
              <Link href={`/team/edit/${team.teamId}`}>
                <Pencil className="size-4" />
                수정
              </Link>
            </Button>
          ) : (
            <Button
              type="button"
              variant="default"
              disabled
              title="팀 관리자만 수정할 수 있습니다."
              className="h-10 min-w-28 !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
            >
              <Pencil className="size-4" />
              수정
            </Button>
          )}
          <Button
            type="button"
            variant="destructive"
            className="h-10 min-w-28 !text-destructive hover:!text-destructive [&_svg]:!text-destructive"
            disabled={!canEdit || deleteTeam.isPending}
            title={canEdit ? "팀 삭제" : "팀 관리자만 삭제할 수 있습니다."}
            onClick={handleDelete}
          >
            <Trash2 className="size-4" />
            삭제
          </Button>
        </div>
      </div>

      <Card className="rounded-[28px]">
        <CardContent className="space-y-7 p-6">
          <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-[28px] font-bold tracking-[-0.06em] text-foreground">
                  {team.teamName}
                </h2>
                <StatusBadge status={team.statusCode} />
              </div>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-muted-foreground">
                {team.description ?? "팀 설명이 없습니다."}
              </p>
            </div>
            <div className="flex size-12 items-center justify-center rounded-2xl border border-primary/20 bg-primary/10 text-primary">
              <ShieldCheck className="size-5" />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <InfoCard
              icon={CalendarDays}
              label="시작일"
              value={team.startDate ? formatDate(team.startDate) : "-"}
            />
            <InfoCard
              icon={CalendarDays}
              label="종료 예정일"
              value={
                team.expectedEndDate ? formatDate(team.expectedEndDate) : "-"
              }
            />
            <InfoCard
              icon={Crown}
              label="팀장"
              value={team.teamLeaderName ?? "미지정"}
            />
            <InfoCard
              icon={ShieldCheck}
              label="관리자"
              value={team.deptHeadAdminUsername ?? "미지정"}
            />
          </div>

          <section className="space-y-4 border-t border-border/70 pt-6">
            <div>
              <h3 className="text-lg font-semibold tracking-[-0.04em] text-foreground">
                팀원 목록
              </h3>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                팀 내 역할과 책임자를 확인합니다.
              </p>
            </div>

            {users.length === 0 ? (
              <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
                등록된 팀원이 없습니다.
              </div>
            ) : null}

            <div className="grid gap-3">
              {users.map((user) => (
                <div
                  key={user.userId}
                  className="flex flex-col gap-4 rounded-2xl border border-border/70 bg-muted/20 px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-primary/10 font-bold text-primary">
                      {user.userName.slice(0, 1)}
                    </div>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="font-semibold text-foreground">
                          {user.userName}
                        </p>
                        {user.isLeader ? (
                          <Badge variant="default">팀장</Badge>
                        ) : null}
                        <Badge variant="outline">
                          {user.positionName ?? "-"}
                        </Badge>
                      </div>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {user.teamRole ?? "-"}
                      </p>
                    </div>
                  </div>
                  <UserRound className="hidden size-5 text-muted-foreground sm:block" />
                </div>
              ))}
            </div>
          </section>
        </CardContent>
      </Card>
    </section>
  );
}

function InfoCard({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Users;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl border border-border/70 bg-muted/25 p-4">
      <div className="flex items-center gap-2 text-muted-foreground">
        <Icon className="size-4" />
        <p className="text-xs font-semibold uppercase tracking-[0.14em]">
          {label}
        </p>
      </div>
      <p className="mt-3 truncate text-sm font-semibold text-foreground">
        {value}
      </p>
    </div>
  );
}

function StatusBadge({ status }: { status: TeamStatusCode }) {
  return (
    <Badge variant={status === "ACTIVE" ? "success" : "outline"}>
      {getTeamStatusLabel(status)}
    </Badge>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}
