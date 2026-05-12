"use client";

import Link from "next/link";
import { useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Button } from "@/components/ui/button";
import { DepartmentForm } from "../../_components/departmentForm";
import { useDepartmentList, useUpdateDepartment } from "../../_hooks";
import type { DepartmentFormValues } from "../../_types/department.types";

export default function DepartmentEditPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const departmentId = Number(params.id);
  const { data, isLoading, error } = useDepartmentList();
  const updateDepartment = useUpdateDepartment();

  const department = useMemo(
    () =>
      data?.departments.find((item) => item.departmentId === departmentId) ??
      null,
    [data?.departments, departmentId],
  );

  async function handleSubmit(values: DepartmentFormValues) {
    try {
      await updateDepartment.mutateAsync({
        departmentId,
        payload: values,
      });
      router.push("/department");
    } catch (submitError) {
      alert(getApiErrorMessage(submitError, "부서 수정에 실패했습니다."));
    }
  }

  const initialValues: DepartmentFormValues | undefined = department
    ? {
        departmentName: department.departmentName,
        description: department.description ?? "",
        departmentHeadUserId: department.departmentHeadUserId,
      }
    : undefined;

  return (
    <section className="space-y-6">
      <PageHeader
        title="부서 수정"
        description="기존 부서의 이름, 설명, 부서장 정보를 수정합니다."
        actions={
          <Button asChild variant="outline" className="h-10 px-4">
            <Link href="/department">
              <ArrowLeft className="size-4" />
              목록으로
            </Link>
          </Button>
        }
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
          수정할 부서를 찾을 수 없습니다.
        </div>
      ) : null}

      {department && initialValues ? (
        <DepartmentForm
          key={department.departmentId}
          initialValues={initialValues}
          submitLabel="부서 수정"
          isSubmitting={updateDepartment.isPending}
          onSubmit={handleSubmit}
        />
      ) : null}
    </section>
  );
}
