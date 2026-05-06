"use client";

import Link from "next/link";
import { useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  Building2,
  CalendarDays,
  Pencil,
  Trash2,
  UserCog,
} from "lucide-react";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useDeleteDepartment, useDepartmentList } from "../../_hooks";
import type { DepartmentSummary } from "../../_types/department.types";

export default function DepartmentDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const departmentId = Number(params.id);
  const { data, isLoading, error } = useDepartmentList();
  const deleteDepartment = useDeleteDepartment();

  const department = useMemo(
    () =>
      data?.departments.find((item) => item.departmentId === departmentId) ??
      null,
    [data?.departments, departmentId],
  );

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
        description="부서의 책임자와 생성 정보를 확인합니다."
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
        <DepartmentDetail
          department={department}
          isDeleting={deleteDepartment.isPending}
          onDelete={handleDelete}
        />
      ) : null}
    </section>
  );
}

function DepartmentDetail({
  department,
  isDeleting,
  onDelete,
}: {
  department: DepartmentSummary;
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
              <h2 className="text-[28px] font-bold tracking-[-0.06em] text-foreground">
                {department.departmentName}
              </h2>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-muted-foreground">
                {department.description ?? "부서 설명이 없습니다."}
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
              icon={CalendarDays}
              label="생성일"
              value={formatDate(department.createdAt)}
            />
            <InfoCard
              icon={CalendarDays}
              label="수정일"
              value={formatDate(department.updatedAt)}
            />
          </div>
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

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}
