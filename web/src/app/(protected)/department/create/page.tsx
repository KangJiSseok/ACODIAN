"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Button } from "@/components/ui/button";
import { DepartmentForm } from "../_components/departmentForm";
import { useCreateDepartment } from "../_hooks";
import type { DepartmentFormValues } from "../_types/department.types";

export default function DepartmentCreatePage() {
  const router = useRouter();
  const createDepartment = useCreateDepartment();

  async function handleSubmit(values: DepartmentFormValues) {
    try {
      await createDepartment.mutateAsync(values);
      router.push("/department");
    } catch (error) {
      alert(getApiErrorMessage(error, "부서 등록에 실패했습니다."));
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="부서 등록"
        description="새 부서를 등록하고 부서 운영 정보를 입력합니다."
        actions={
          <Button asChild variant="outline" className="h-10 px-4">
            <Link href="/department">
              <ArrowLeft className="size-4" />
              목록으로
            </Link>
          </Button>
        }
      />

      <DepartmentForm
        submitLabel="부서 등록"
        isSubmitting={createDepartment.isPending}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
