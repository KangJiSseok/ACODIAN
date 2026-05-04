// web/src/app/(protected)/department/page.tsx

"use client";

import { useState } from "react";
import { Building2, Pencil, Trash2 } from "lucide-react";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  useCreateDepartment,
  useDeleteDepartment,
  useDepartmentList,
  useUpdateDepartment,
} from "./_hooks";
import type {
  DepartmentFormValues,
  DepartmentSummary,
} from "./_types/department.types";

const emptyForm: DepartmentFormValues = {
  departmentName: "",
  description: "",
  departmentHeadUserId: null,
};

export default function DepartmentPage() {
  const { data, isLoading, error } = useDepartmentList();
  const createDepartment = useCreateDepartment();
  const updateDepartment = useUpdateDepartment();
  const deleteDepartment = useDeleteDepartment();

  const [formValues, setFormValues] = useState(emptyForm);
  const [editingDepartment, setEditingDepartment] =
    useState<DepartmentSummary | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const departments = data?.departments ?? [];

  function openCreateForm() {
    setEditingDepartment(null);
    setFormValues(emptyForm);
    setIsFormOpen(true);
  }

  function openEditForm(department: DepartmentSummary) {
    setEditingDepartment(department);
    setFormValues({
      departmentName: department.departmentName,
      description: department.description ?? "",
      departmentHeadUserId: department.departmentHeadUserId,
    });
    setIsFormOpen(true);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const payload = {
      departmentName: formValues.departmentName.trim(),
      description: formValues.description?.trim() || null,
      departmentHeadUserId: formValues.departmentHeadUserId,
    };

    try {
      if (editingDepartment) {
        await updateDepartment.mutateAsync({
          departmentId: editingDepartment.departmentId,
          payload,
        });
      } else {
        await createDepartment.mutateAsync(payload);
      }

      setIsFormOpen(false);
      setEditingDepartment(null);
      setFormValues(emptyForm);
    } catch (submitError) {
      alert(getApiErrorMessage(submitError, "부서 저장에 실패했습니다."));
    }
  }

  async function handleDelete(departmentId: number) {
    if (!confirm("부서를 비활성화하시겠습니까?")) return;

    try {
      await deleteDepartment.mutateAsync(departmentId);
    } catch (deleteError) {
      alert(getApiErrorMessage(deleteError, "부서 삭제에 실패했습니다."));
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="부서 관리"
        description="권한 범위 안의 부서 목록과 현재 부서 운영 규모를 확인합니다."
        actions={<Button onClick={openCreateForm}>부서 등록</Button>}
      />

      <div className="grid gap-4 md:grid-cols-3">
        <SummaryCard
          label="DEPARTMENTS"
          value={`${data?.activeDepartmentCount ?? 0}개`}
        />
        <SummaryCard label="TEAMS" value={`${data?.activeTeamCount ?? 0}개`} />
        <SummaryCard
          label="MEMBERS"
          value={`${data?.activeUserCount ?? 0}명`}
        />
      </div>

      {isFormOpen ? (
        <Card>
          <CardContent className="space-y-4 pt-6">
            <form className="grid gap-4" onSubmit={handleSubmit}>
              <Input
                value={formValues.departmentName}
                onChange={(event) =>
                  setFormValues((prev) => ({
                    ...prev,
                    departmentName: event.target.value,
                  }))
                }
                placeholder="부서명"
                required
              />

              <Textarea
                value={formValues.description ?? ""}
                onChange={(event) =>
                  setFormValues((prev) => ({
                    ...prev,
                    description: event.target.value,
                  }))
                }
                placeholder="부서 설명"
              />

              <Input
                type="number"
                value={formValues.departmentHeadUserId ?? ""}
                onChange={(event) =>
                  setFormValues((prev) => ({
                    ...prev,
                    departmentHeadUserId: event.target.value
                      ? Number(event.target.value)
                      : null,
                  }))
                }
                placeholder="부서장 사용자 ID"
              />

              <div className="flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsFormOpen(false)}
                >
                  취소
                </Button>
                <Button type="submit">
                  {editingDepartment ? "수정" : "등록"}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      ) : null}

      {isLoading ? (
        <p className="text-sm text-muted-foreground">
          부서 목록을 불러오는 중입니다.
        </p>
      ) : null}
      {error ? (
        <p className="text-sm text-destructive">
          부서 목록을 불러오지 못했습니다.
        </p>
      ) : null}

      <div className="grid gap-5 xl:grid-cols-3">
        {departments.map((department) => (
          <Card key={department.departmentId}>
            <CardContent className="space-y-5 pt-6">
              <div className="flex items-start gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl border bg-muted">
                  <Building2 className="size-5 text-muted-foreground" />
                </div>
                <div>
                  <h2 className="font-semibold">{department.departmentName}</h2>
                  <p className="mt-2 text-sm text-muted-foreground">
                    {department.description ?? "부서 설명이 없습니다."}
                  </p>
                </div>
              </div>

              <Info
                label="사업부장"
                value={department.departmentHeadUserName ?? "-"}
              />
              <Info label="생성일" value={formatDate(department.createdAt)} />
              <Info label="수정일" value={formatDate(department.updatedAt)} />

              <div className="flex justify-end gap-2">
                <Button
                  variant="outline"
                  onClick={() => openEditForm(department)}
                >
                  <Pencil className="size-4" />
                  수정
                </Button>
                <Button
                  variant="destructive"
                  onClick={() => handleDelete(department.departmentId)}
                >
                  <Trash2 className="size-4" />
                  삭제
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <Card>
      <CardContent className="pt-6">
        <p className="text-xs font-semibold tracking-[0.2em] text-muted-foreground">
          {label}
        </p>
        <p className="mt-3 text-2xl font-bold">{value}</p>
      </CardContent>
    </Card>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-semibold">{value}</p>
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
