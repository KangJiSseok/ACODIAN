"use client";

import { type FormEvent, useMemo, useState } from "react";
import { Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { Input } from "@/components/ui/input";
import { Select, type SelectOption } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAdminCandidates } from "../_hooks";
import type { AdminCandidate } from "../_types/admin-candidate.types";
import type { DepartmentFormValues } from "../_types/department.types";

const selectClassName = "h-11 rounded-2xl px-4 text-sm";

const emptyValues: DepartmentFormValues = {
  departmentName: "",
  description: "",
  departmentHeadUserId: null,
};

export function DepartmentForm({
  initialValues,
  submitLabel,
  isSubmitting = false,
  onSubmit,
}: {
  initialValues?: DepartmentFormValues;
  submitLabel: string;
  isSubmitting?: boolean;
  onSubmit: (values: DepartmentFormValues) => Promise<void>;
}) {
  const [values, setValues] = useState<DepartmentFormValues>(
    initialValues ?? emptyValues,
  );

  const {
    data: candidates,
    isLoading: isCandidatesLoading,
    error: candidatesError,
  } = useAdminCandidates();

  // 부서장 후보 응답을 셀렉트 옵션으로 변환합니다.
  // 폼 초기값에 들어 있는 부서장이 후보 응답에 없을 수 있어 별도 옵션으로 보존합니다.
  const headOptions = useMemo<SelectOption[]>(() => {
    const options: SelectOption[] = [{ value: "", label: "선택 안 함" }];
    const fetched = candidates ?? [];
    const fetchedIds = new Set(fetched.map((candidate) => candidate.userId));

    fetched.forEach((candidate) => {
      options.push({
        value: String(candidate.userId),
        label: formatCandidateLabel(candidate),
      });
    });

    if (
      values.departmentHeadUserId !== null &&
      !fetchedIds.has(values.departmentHeadUserId)
    ) {
      options.push({
        value: String(values.departmentHeadUserId),
        label: `현재 부서장 (ID: ${values.departmentHeadUserId})`,
      });
    }

    return options;
  }, [candidates, values.departmentHeadUserId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    await onSubmit({
      departmentName: values.departmentName.trim(),
      description: values.description?.trim() || null,
      departmentHeadUserId: values.departmentHeadUserId,
    });
  }

  return (
    <CardSpotlight className="rounded-[24px]">
      <CardContent className="p-6">
        <form className="grid gap-5" onSubmit={handleSubmit}>
          <div className="flex items-start gap-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl border border-border/70 bg-muted/40 text-muted-foreground">
              <Building2 className="size-5" />
            </div>
            <div>
              <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
                부서 기본 정보
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                부서명은 필수이며, 설명과 부서장 지정은 선택 입력입니다.
              </p>
            </div>
          </div>

          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(18rem,0.42fr)]">
            <div className="grid gap-2">
              <label
                htmlFor="departmentName"
                className="text-sm font-medium text-foreground"
              >
                부서명
              </label>
              <Input
                id="departmentName"
                className="h-12"
                value={values.departmentName}
                onChange={(event) =>
                  setValues((prev) => ({
                    ...prev,
                    departmentName: event.target.value,
                  }))
                }
                placeholder="예: 솔루션개발사업부"
                required
              />
            </div>

            <div className="grid gap-2">
              <label
                htmlFor="departmentHeadUserId"
                className="text-sm font-medium text-foreground"
              >
                부서장
              </label>
              <Select
                id="departmentHeadUserId"
                className={selectClassName}
                value={
                  values.departmentHeadUserId === null
                    ? ""
                    : String(values.departmentHeadUserId)
                }
                onChange={(event) =>
                  setValues((prev) => ({
                    ...prev,
                    departmentHeadUserId: event.target.value
                      ? Number(event.target.value)
                      : null,
                  }))
                }
                options={headOptions}
                disabled={isCandidatesLoading}
              />
              {candidatesError ? (
                <p className="text-xs text-destructive">
                  부서장 후보를 불러오지 못했습니다.
                </p>
              ) : null}
            </div>
          </div>

          <div className="grid gap-2">
            <label
              htmlFor="description"
              className="text-sm font-medium text-foreground"
            >
              설명
            </label>
            <Textarea
              id="description"
              className="min-h-36 rounded-2xl bg-input/90 px-4 py-3"
              value={values.description ?? ""}
              onChange={(event) =>
                setValues((prev) => ({
                  ...prev,
                  description: event.target.value,
                }))
              }
              placeholder="부서의 주요 역할이나 운영 범위를 입력하세요."
            />
          </div>

          <div className="rounded-2xl border border-border/70 bg-muted/25 px-5 py-4 text-sm leading-6 text-muted-foreground">
            부서장은 현재 정책상 DIRECTOR 또는 DEPT_HEAD 역할의 사용자만 후보로
            노출됩니다. 후보가 없거나 변경하지 않으려면 &quot;선택 안 함&quot;을
            그대로 두세요.
          </div>

          <div className="flex justify-end">
            <Button
              type="submit"
              className="h-11 min-w-32 px-6 font-semibold"
              disabled={isSubmitting}
            >
              {isSubmitting ? "저장 중" : submitLabel}
            </Button>
          </div>
        </form>
      </CardContent>
    </CardSpotlight>
  );
}

function formatCandidateLabel(candidate: AdminCandidate) {
  const role = candidate.titleName ?? candidate.positionName;
  return role ? `${candidate.userName} (${role})` : candidate.userName;
}
