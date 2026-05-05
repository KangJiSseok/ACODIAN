"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  ArrowRight,
  CalendarDays,
  Crown,
  Pencil,
  ShieldCheck,
  Users,
} from "lucide-react";
import { Pagination } from "@/app/_common/components/data-display/pagination";
import { usePagination } from "@/app/_common/hooks/usePagination";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { cn } from "@/lib/utils";
import { useTeamList, useTeamSummary } from "./_hooks";
import type { TeamStatusCode, TeamSummary } from "./_types/team.types";

type TeamFilter = "all" | "ACTIVE" | "INACTIVE";

const teamFilters: Array<{
  key: TeamFilter;
  label: string;
  getCount: (summary: {
    total: number;
    active: number;
    inactive: number;
  }) => number;
}> = [
  { key: "all", label: "전체 팀", getCount: (summary) => summary.total },
  {
    key: "ACTIVE",
    label: "활성화된 팀",
    getCount: (summary) => summary.active,
  },
  {
    key: "INACTIVE",
    label: "비활성화된 팀",
    getCount: (summary) => summary.inactive,
  },
];

export default function TeamPage() {
  const [filter, setFilter] = useState<TeamFilter>("all");
  const { data: teamPage, isLoading, error } = useTeamList({ pageSize: 100 });
  const { data: summary } = useTeamSummary();

  const teams = useMemo(() => teamPage?.items ?? [], [teamPage?.items]);
  const counts = {
    total: summary?.totalTeamCount ?? teams.length,
    active:
      summary?.activeTeamCount ??
      teams.filter((team) => team.statusCode === "ACTIVE").length,
    inactive:
      summary?.inactiveTeamCount ??
      teams.filter((team) => team.statusCode === "INACTIVE").length,
  };

  const filteredTeams = useMemo(() => {
    if (filter === "all") {
      return teams;
    }

    return teams.filter((team) => team.statusCode === filter);
  }, [filter, teams]);

  const pagination = usePagination(filteredTeams, 4);

  return (
    <section className="space-y-6">
      <PageHeader
        title="팀 관리"
        description="역할에 따라 팀 목록과 소속 구성원을 올바른 범위로 확인합니다."
        actions={
          <Button
            asChild
            type="button"
            variant="default"
            className="h-10 min-w-32 px-6 text-sm font-semibold !text-primary-foreground hover:!text-primary-foreground"
          >
            <Link href="/team/create">팀 등록</Link>
          </Button>
        }
      />

      <div className="grid gap-4 md:grid-cols-3">
        {teamFilters.map((item) => {
          const isSelected = filter === item.key;

          return (
            <button
              key={item.key}
              type="button"
              className="text-left"
              aria-pressed={isSelected}
              onClick={() => setFilter(item.key)}
            >
              <CardSpotlight
                className={cn(
                  "rounded-[24px] p-5 transition-all duration-300 hover:-translate-y-1",
                  isSelected &&
                    "border-primary/50 bg-primary/10 shadow-[0_18px_42px_-28px_rgba(59,130,246,0.9)]",
                )}
              >
                <p className="text-xs font-medium uppercase tracking-[0.18em] text-muted-foreground">
                  {item.label}
                </p>
                <p className="mt-2 text-2xl font-semibold text-foreground transition-colors group-hover/card-spotlight:text-primary">
                  {item.getCount(counts)}개
                </p>
              </CardSpotlight>
            </button>
          );
        })}
      </div>

      <section className="space-y-4">
        <div className="border-t-2 border-foreground/70 pt-5">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
                {filter === "all" ? "팀 목록" : `${getStatusLabel(filter)} 팀`}
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                팀 상태, 책임자, 구성원 규모와 내 역할을 한 화면에서 확인합니다.
              </p>
            </div>
            <p className="text-sm font-medium text-muted-foreground">
              표시 {filteredTeams.length}개
            </p>
          </div>
        </div>

        {isLoading ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            팀 목록을 불러오는 중입니다.
          </div>
        ) : null}

        {error ? (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
            팀 목록을 불러오지 못했습니다.
          </div>
        ) : null}

        {!isLoading && !error && filteredTeams.length === 0 ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            조건에 맞는 팀이 없습니다.
          </div>
        ) : null}

        <div className="grid gap-4 xl:grid-cols-2">
          {pagination.items.map((team) => (
            <TeamCard key={team.teamId} team={team} />
          ))}
        </div>

        <Pagination
          page={pagination.page}
          totalPages={pagination.totalPages}
          onPageChange={pagination.setPage}
        />
      </section>
    </section>
  );
}

function TeamCard({ team }: { team: TeamSummary }) {
  return (
    <CardSpotlight className="h-full rounded-[26px] transition-all duration-300 hover:-translate-y-1">
      <CardContent className="flex h-full min-h-[23rem] flex-col gap-5 p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0 space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="max-w-full truncate text-lg font-semibold tracking-[-0.04em] text-foreground transition-colors group-hover/card-spotlight:text-primary">
                {team.teamName}
              </h3>
              {team.isPrimary ? (
                <Badge variant="default">대표 소속</Badge>
              ) : null}
              {team.myIsLeader ? <Badge variant="secondary">팀장</Badge> : null}
            </div>
            <p className="line-clamp-2 min-h-12 text-sm leading-6 text-muted-foreground">
              {team.description ?? "팀 설명이 없습니다."}
            </p>
          </div>
          <StatusBadge status={team.statusCode} />
        </div>

        <div className="grid gap-3 sm:grid-cols-3">
          <InfoBox
            icon={Crown}
            label="팀장"
            value={team.teamLeaderName ?? "미지정"}
          />
          <InfoBox
            icon={Users}
            label="구성원"
            value={`${team.memberCount}명`}
          />
          <InfoBox
            icon={ShieldCheck}
            label="내 역할"
            value={team.teamRole ?? "-"}
          />
        </div>

        <div className="rounded-2xl border border-border/60 bg-muted/25 p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            운영 범위
          </p>
          <div className="mt-3 grid gap-3 text-sm text-muted-foreground sm:grid-cols-2">
            <DateLine label="시작일" value={team.startDate} />
            <DateLine label="종료 예정일" value={team.expectedEndDate} />
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Badge variant="outline">
              {team.allocation ?? "배치 정보 없음"}
            </Badge>
            <Badge variant="outline">
              {team.isPrimary ? "주 소속" : "겸임/참여"}
            </Badge>
          </div>
        </div>

        <div className="mt-auto flex justify-end gap-3 pt-1">
          <Button
            asChild
            type="button"
            variant="secondary"
            className="h-11 min-w-28 px-5 text-sm font-semibold"
          >
            <Link href={`/team/detail/${team.teamId}`}>
              상세
              <ArrowRight className="size-4" />
            </Link>
          </Button>
          <Button
            asChild
            type="button"
            variant="default"
            className="h-11 min-w-28 px-5 text-sm font-semibold !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
          >
            <Link href={`/team/edit/${team.teamId}`}>
              <Pencil className="size-4" />
              수정
            </Link>
          </Button>
        </div>
      </CardContent>
    </CardSpotlight>
  );
}

function StatusBadge({ status }: { status: TeamStatusCode }) {
  const isActive = status === "ACTIVE";

  return (
    <Badge variant={isActive ? "success" : "outline"} className="shrink-0">
      {getStatusLabel(status)}
    </Badge>
  );
}

function InfoBox({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Users;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl border border-border/60 bg-muted/25 p-4 text-sm transition-colors group-hover/card-spotlight:border-primary/20 group-hover/card-spotlight:bg-muted/40">
      <div className="flex items-center gap-2 text-muted-foreground">
        <Icon className="size-4" />
        <p className="text-xs font-semibold uppercase tracking-[0.14em]">
          {label}
        </p>
      </div>
      <p className="mt-2 truncate font-semibold text-foreground">{value}</p>
    </div>
  );
}

function DateLine({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="flex min-w-0 items-center gap-2">
      <CalendarDays className="size-4 shrink-0" />
      <span className="shrink-0 font-medium text-foreground">{label}</span>
      <span className="truncate">{value ? formatDate(value) : "-"}</span>
    </div>
  );
}

function getStatusLabel(status: TeamStatusCode) {
  if (status === "ACTIVE") {
    return "운영중";
  }

  if (status === "INACTIVE") {
    return "비활성";
  }

  return status;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}
