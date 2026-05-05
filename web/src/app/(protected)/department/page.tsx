"use client";

import Link from "next/link";
import { Building2, Pencil, Trash2 } from "lucide-react";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { useDeleteDepartment, useDepartmentList } from "./_hooks";
import type { DepartmentSummary } from "./_types/department.types";

export default function DepartmentPage() {
  const { data, isLoading, error } = useDepartmentList();
  const deleteDepartment = useDeleteDepartment();

  const departments = data?.departments ?? [];

  async function handleDelete(departmentId: number) {
    if (!confirm("부서를 비활성화하시겠습니까?")) return;

    try {
      await deleteDepartment.mutateAsync(departmentId);
    } catch (deleteError) {
      alert(getApiErrorMessage(deleteError, "부서 삭제에 실패했습니다."));
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="부서 관리"
        description="권한 범위 안의 부서 목록과 현재 부서 운영 규모를 한 화면에서 확인합니다."
        actions={
          <Button
            asChild
            type="button"
            variant="default"
            className="h-10 min-w-32 px-6 text-sm font-semibold !text-primary-foreground hover:!text-primary-foreground"
          >
            <Link href="/department/create">부서 등록</Link>
          </Button>
        }
      />

      <div className="grid gap-4 md:grid-cols-3">
        <SummaryCard
          title="부서"
          value={`${data?.activeDepartmentCount ?? 0}개`}
        />
        <SummaryCard
          title="팀"
          value={`${data?.activeTeamCount ?? 0}개`}
        />
        <SummaryCard
          title="구성원"
          value={`${data?.activeUserCount ?? 0}명`}
        />
      </div>

      <section className="space-y-4">
        <div className="border-t-2 border-foreground/70 pt-5">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
                부서 목록
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                부서 삭제는 비활성화로 처리되며, 활성 팀이 남아 있으면 제한됩니다.
              </p>
            </div>
            <p className="text-sm font-medium text-muted-foreground">
              총 {departments.length}개
            </p>
          </div>
        </div>

        <div className="rounded-2xl border border-border/70 bg-muted/25 px-5 py-4 text-sm leading-6 text-muted-foreground">
          부서 생성과 삭제 정책은 본부장 전용입니다. 삭제 제약은 실제 API
          응답을 기준으로 안내됩니다.
        </div>

        {isLoading ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            부서 목록을 불러오는 중입니다.
          </div>
        ) : null}

        {error ? (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
            부서 목록을 불러오지 못했습니다.
          </div>
        ) : null}

        {!isLoading && !error && departments.length === 0 ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            등록된 부서가 없습니다.
          </div>
        ) : null}

        <div className="grid gap-4 xl:grid-cols-3">
          {departments.map((department) => (
            <DepartmentCard
              key={department.departmentId}
              department={department}
              isDeleting={deleteDepartment.isPending}
              onDelete={() => handleDelete(department.departmentId)}
            />
          ))}
        </div>
      </section>
    </section>
  );
}

function SummaryCard({
  title,
  value,
}: {
  title: string;
  value: string;
}) {
  return (
    <CardSpotlight className="rounded-[24px] p-4 transition-all duration-300 hover:-translate-y-1">
      <p className="text-xs font-medium text-muted-foreground">{title}</p>
      <p className="mt-2 text-2xl font-semibold text-foreground transition-colors group-hover/card-spotlight:text-primary">
        {value}
      </p>
    </CardSpotlight>
  );
}

function DepartmentCard({
  department,
  isDeleting,
  onDelete,
}: {
  department: DepartmentSummary;
  isDeleting: boolean;
  onDelete: () => void;
}) {
  return (
    <CardSpotlight className="h-full rounded-[24px] transition-all duration-300 hover:-translate-y-1">
      <CardContent className="flex h-full min-h-[21rem] flex-col gap-5 p-6">
        <div className="grid min-h-[6.75rem] grid-cols-[2.75rem_minmax(0,1fr)] items-start gap-3">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl border border-border/70 bg-muted/40 text-muted-foreground transition-colors group-hover/card-spotlight:border-primary/30 group-hover/card-spotlight:bg-primary/8 group-hover/card-spotlight:text-primary">
            <Building2 className="size-5" />
          </div>
          <div className="min-w-0">
            <h3 className="truncate text-base font-semibold text-foreground transition-colors group-hover/card-spotlight:text-primary">
              {department.departmentName}
            </h3>
            <p className="mt-2 line-clamp-2 h-12 text-sm leading-6 text-muted-foreground">
              {department.description ?? "부서 설명이 없습니다."}
            </p>
          </div>
        </div>

        <div className="grid gap-3">
          <Info
            label="사업부장"
            value={department.departmentHeadUserName ?? "-"}
          />
          <Info label="생성일" value={formatDate(department.createdAt)} />
        </div>

        <div className="mt-auto flex justify-end gap-3 pt-1">
          <Button
            asChild
            type="button"
            variant="default"
            className="h-11 min-w-28 px-5 text-sm font-semibold !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
          >
            <Link href={`/department/edit/${department.departmentId}`}>
              <Pencil className="size-4" />
              수정
            </Link>
          </Button>
          <Button
            type="button"
            variant="destructive"
            className="h-11 min-w-28 px-5 text-sm font-semibold !text-destructive hover:!text-destructive [&_svg]:!text-destructive"
            onClick={onDelete}
            disabled={isDeleting}
          >
            <Trash2 className="size-4" />
            삭제
          </Button>
        </div>
      </CardContent>
    </CardSpotlight>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3 transition-colors group-hover/card-spotlight:border-primary/20 group-hover/card-spotlight:bg-muted/45">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-semibold text-foreground">{value}</p>
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
