"use client";

import Link from "next/link";
import { Building2 } from "lucide-react";
import { ResultCount } from "@/app/_common/components/data-display/resultCount";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { useDepartmentList } from "./_hooks";
import type { DepartmentSummary } from "./_types/department.types";

export default function DepartmentPage() {
  const { data, isLoading, error } = useDepartmentList();

  const departments = data?.departments ?? [];

  return (
    <section className="space-y-6">
      <PageHeader title="부서 관리" />

      <section className="space-y-2">
        <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
          부서 관리
        </h2>
        <p className="max-w-3xl text-sm leading-6 text-muted-foreground">
          관리 가능한 부서와 운영 현황을 확인합니다.
        </p>
      </section>

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
          <div className="space-y-2">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
                부서 목록
              </h2>
              <Button
                asChild
                type="button"
                variant="default"
                className="h-10 min-w-32 px-6 text-sm font-semibold !text-primary-foreground hover:!text-primary-foreground"
              >
                <Link href="/department/create">부서 등록</Link>
              </Button>
            </div>
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                활성 팀이 남아 있는 부서는 삭제할 수 없습니다.
              </p>
              <ResultCount
                label="조회된 부서"
                count={departments.length}
                unit="개"
              />
            </div>
          </div>
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
            <DepartmentCard key={department.departmentId} department={department} />
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
    <CardSpotlight className="rounded-[24px] p-5 transition-all duration-300 hover:-translate-y-1">
      <p className="text-xs font-medium uppercase tracking-[0.18em] text-muted-foreground">
        {title}
      </p>
      <p className="mt-2 text-2xl font-semibold text-foreground transition-colors group-hover/card-spotlight:text-primary">
        {value}
      </p>
    </CardSpotlight>
  );
}

function DepartmentCard({
  department,
}: {
  department: DepartmentSummary;
}) {
  return (
    <Link
      href={`/department/detail/${department.departmentId}`}
      className="block h-full rounded-[24px] outline-none transition-transform focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
    >
      <CardSpotlight className="h-full cursor-pointer rounded-[24px] transition-all duration-300 hover:-translate-y-1">
        <CardContent className="flex h-full min-h-[17rem] flex-col gap-5 p-6">
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
        </CardContent>
      </CardSpotlight>
    </Link>
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
