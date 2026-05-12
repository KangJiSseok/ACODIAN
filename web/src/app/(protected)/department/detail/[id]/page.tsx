"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  Building2,
  Crown,
  Pencil,
  Trash2,
  UserCog,
  UsersRound,
} from "lucide-react";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useDeleteDepartment, useDepartmentDetail } from "../../_hooks";
import type {
  DepartmentDetail,
  DepartmentDetailTeamSummary,
} from "../../_types/department.types";

export default function DepartmentDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const departmentId = Number(params.id);
  const { data: department, isLoading, error } = useDepartmentDetail(departmentId);
  const deleteDepartment = useDeleteDepartment();

  async function handleDelete() {
    if (!department) {
      return;
    }

    if (!confirm(`${department.departmentName} 부서를 삭제하시겠습니까?`)) {
      return;
    }

    try {
      await deleteDepartment.mutateAsync(department.departmentId);
      router.push("/department");
    } catch (deleteError) {
      alert(getApiErrorMessage(deleteError, "부서 삭제에 실패했습니다."));
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title={department?.departmentName ?? "부서 상세"}
        description="부서의 책임자와 소속 팀을 확인합니다."
      />

      {isLoading ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          부서 정보를 불러오는 중입니다.
        </div>
      ) : null}

      {error ? (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          부서 정보를 불러오지 못했습니다.
        </div>
      ) : null}

      {!isLoading && !error && !department ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          부서를 찾을 수 없습니다.
        </div>
      ) : null}

      {department ? (
        <DepartmentDetailView
          department={department}
          isDeleting={deleteDepartment.isPending}
          onDelete={handleDelete}
        />
      ) : null}
    </section>
  );
}

function DepartmentDetailView({
  department,
  isDeleting,
  onDelete,
}: {
  department: DepartmentDetail;
  isDeleting: boolean;
  onDelete: () => void;
}) {
  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button asChild variant="outline" className="h-10">
          <Link href="/department">
            <ArrowLeft className="size-4" />
            부서 목록
          </Link>
        </Button>
        <div className="flex flex-wrap justify-end gap-3">
          <Button
            asChild
            variant="default"
            className="h-10 min-w-28 !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
          >
            <Link href={`/department/edit/${department.departmentId}`}>
              <Pencil className="size-4" />
              수정
            </Link>
          </Button>
          <Button
            type="button"
            variant="destructive"
            className="h-10 min-w-28 !text-destructive hover:!text-destructive [&_svg]:!text-destructive"
            onClick={onDelete}
            disabled={isDeleting}
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
              <h2 className="text-[28px] font-bold tracking-[-0.04em] text-foreground">
                {department.departmentName}
              </h2>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-muted-foreground">
                이 부서가 직접 소유한 팀과 운영 책임자를 확인합니다.
              </p>
            </div>
            <div className="flex size-12 items-center justify-center rounded-2xl border border-primary/20 bg-primary/10 text-primary">
              <Building2 className="size-5" />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <InfoCard
              icon={UserCog}
              label="사업부장"
              value={department.departmentHeadUserName ?? "미지정"}
            />
            <InfoCard
              icon={UsersRound}
              label="소속 팀"
              value={`${department.teams.length}개`}
            />
          </div>
        </CardContent>
      </Card>

      <section className="space-y-4">
        <div className="border-t-2 border-foreground/70 pt-5">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h3 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
                소속 팀
              </h3>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                부서에 연결된 팀의 리더, 기간, 구성원 수를 확인합니다.
              </p>
            </div>
            <p className="text-sm font-medium text-muted-foreground">
              총 {department.teams.length}개
            </p>
          </div>
        </div>

        {department.teams.length === 0 ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            연결된 팀이 없습니다.
          </div>
        ) : null}

        <div className="grid gap-3 xl:grid-cols-2">
          {department.teams.map((team) => (
            <TeamSummaryCard key={team.teamId} team={team} />
          ))}
        </div>
      </section>
    </section>
  );
}

function TeamSummaryCard({ team }: { team: DepartmentDetailTeamSummary }) {
  return (
    <Link
      href={`/team/detail/${team.teamId}`}
      className="block rounded-2xl border border-border/70 bg-muted/20 px-4 py-4 outline-none transition-colors hover:border-primary/30 hover:bg-primary/5 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <Building2 className="size-4 shrink-0 text-primary" />
            <h4 className="truncate text-base font-semibold text-foreground">
              {team.teamName}
            </h4>
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            {formatTeamPeriod(team)}
          </p>
        </div>
        <div className="grid shrink-0 grid-cols-2 gap-2 text-sm sm:min-w-56">
          <MiniInfo
            icon={Crown}
            label="리더"
            value={team.leaderName ?? "미지정"}
          />
          <MiniInfo
            icon={UsersRound}
            label="구성원"
            value={`${team.memberCount}명`}
          />
        </div>
      </div>
    </Link>
  );
}

function InfoCard({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Building2;
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

function MiniInfo({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Building2;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl border border-border/60 bg-background/60 px-3 py-2">
      <div className="flex items-center gap-1.5 text-muted-foreground">
        <Icon className="size-3.5" />
        <span className="text-[11px] font-medium">{label}</span>
      </div>
      <p className="mt-1 truncate text-xs font-semibold text-foreground">
        {value}
      </p>
    </div>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}

function formatTeamPeriod(team: DepartmentDetailTeamSummary) {
  const startDate = team.startDate ? formatDate(team.startDate) : "시작일 미정";
  const expectedEndDate = team.expectedEndDate
    ? formatDate(team.expectedEndDate)
    : "종료일 미정";

  return `${startDate} - ${expectedEndDate}`;
}
