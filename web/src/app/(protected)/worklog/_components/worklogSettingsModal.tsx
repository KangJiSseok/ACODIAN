"use client"

import { CalendarDays } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Select } from "@/components/ui/select"
import { getImportanceLabel } from "../_utils/worklogFormat"
import type { WorklogFormValues } from "../_types/worklog.types"

type SelectOption = {
  label: string
  value: string
}

type WorklogSettingsValidationErrors = {
  actualHours?: string
}

interface WorklogSettingsModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  values: WorklogFormValues
  onValuesChange: (values: WorklogFormValues) => void
  controlClassName: string
  teamOptions: SelectOption[]
  statusOptions: SelectOption[]
  validationErrors?: WorklogSettingsValidationErrors
  actualHoursInput: string
  onActualHoursInputChange: (value: string) => void
}

export function WorklogSettingsModal({
  open,
  onOpenChange,
  values,
  onValuesChange,
  controlClassName,
  teamOptions,
  statusOptions,
  validationErrors,
  actualHoursInput,
  onActualHoursInputChange,
}: WorklogSettingsModalProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="relative max-h-[86vh] max-w-4xl overflow-y-auto rounded-[28px] p-7">
        <DialogHeader>
          <DialogTitle>작업 설정</DialogTitle>
          <DialogDescription>
            업무 상태, 담당자, 업무 소요 예상 시간과 진행 일정을 설정합니다.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-5">
          <ModalField label="상태 및 중요도">
            <div className="grid gap-3 sm:grid-cols-2">
              <Select
                className={controlClassName}
                value={values.status}
                options={statusOptions}
                onChange={(event) =>
                  onValuesChange({
                    ...values,
                    status: event.target.value as WorklogFormValues["status"],
                  })
                }
              />
              <Select
                className={controlClassName}
                value={values.importance}
                options={[
                  { label: getImportanceLabel("URGENT"), value: "URGENT" },
                  { label: getImportanceLabel("HIGH"), value: "HIGH" },
                  { label: getImportanceLabel("NORMAL"), value: "NORMAL" },
                  { label: getImportanceLabel("LOW"), value: "LOW" },
                ]}
                onChange={(event) =>
                  onValuesChange({
                    ...values,
                    importance: event.target.value as WorklogFormValues["importance"],
                  })
                }
              />
            </div>
          </ModalField>

          <ModalField label="담당 팀">
            <div className="grid gap-3">
              <Select
                className={controlClassName}
                value={String(values.teamId)}
                options={teamOptions}
                onChange={(event) =>
                  onValuesChange({ ...values, teamId: Number(event.target.value) })
                }
              />
            </div>
          </ModalField>

          <ModalField label="업무 소요 예상 시간">
            <div className="space-y-2">
              <Input
                className={controlClassName}
                type="number"
                step="0.5"
                value={actualHoursInput}
                aria-invalid={Boolean(validationErrors?.actualHours)}
                onFocus={(event) => event.currentTarget.select()}
                onChange={(event) => onActualHoursInputChange(event.target.value)}
                placeholder="예: 1.5"
              />
              {validationErrors?.actualHours ? (
                <p className="text-xs font-medium text-destructive">
                  {validationErrors.actualHours}
                </p>
              ) : null}
              <p className="text-xs leading-5 text-muted-foreground">
                소수 입력이 가능합니다. 예: 1.5 = 1시간 30분
              </p>
            </div>
          </ModalField>

          <ModalField label="진행 일정">
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground">
                숫자로 직접 수정하거나 입력칸의 달력 아이콘으로 선택할 수 있습니다.
              </p>
              <div className="grid gap-3 sm:grid-cols-[1fr_auto_1fr] sm:items-center">
                <ScheduleDateControl
                  label="지시일"
                  value={values.instructionDate}
                  onChange={(nextValue) =>
                    onValuesChange({ ...values, instructionDate: nextValue })
                  }
                />
                <span className="hidden text-center text-sm text-muted-foreground sm:block">
                  ~
                </span>
                <ScheduleDateControl
                  label="마감일"
                  value={values.dueDate}
                  onChange={(nextValue) =>
                    onValuesChange({ ...values, dueDate: nextValue })
                  }
                />
              </div>
            </div>
          </ModalField>
        </div>
        <DialogFooter className="items-center justify-between border-t border-border/70 pt-4">
          <p className="mr-auto text-xs text-muted-foreground">
            변경 내용은 업무 등록 폼에 즉시 반영됩니다.
          </p>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            닫기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function ModalField({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-3">
      <label className="inline-flex items-center gap-2 text-[15px] font-[600] text-foreground">
        {label}
      </label>
      <div>{children}</div>
    </div>
  )
}

function ScheduleDateControl({
  label,
  value,
  onChange,
}: {
  label: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label className="group flex min-h-[76px] items-center gap-3 rounded-2xl border border-border/80 bg-background/75 px-4 py-3 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/35 hover:bg-primary/5 hover:shadow-[0_16px_38px_-28px_rgba(30,58,138,0.8)] focus-within:border-primary/45 focus-within:ring-2 focus-within:ring-primary/15">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-full border border-primary/20 bg-primary/10 text-primary">
        <CalendarDays className="size-5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
          {label}
        </span>
        <input
          type="date"
          className="schedule-date-input mt-1 h-10 w-full rounded-full border border-border/70 bg-background px-3 text-sm font-semibold text-foreground outline-none transition-colors [color-scheme:light] hover:border-primary/35 focus:border-primary dark:[color-scheme:dark]"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          aria-label={`${label} 날짜 입력`}
        />
      </span>
    </label>
  )
}
