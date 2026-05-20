"use client"

import type { FocusEvent } from "react"
import { CalendarDays, Search, X } from "lucide-react"
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
import { cn } from "@/lib/utils"
import { getImportanceLabel, getWorklogStatusLabel } from "../_utils/worklogFormat"
import type {
  WorklogFormDependencyOption,
  WorklogFormValues,
} from "../_types/worklog.types"

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
  disableTeamChange?: boolean
  teamOptions: SelectOption[]
  statusOptions: SelectOption[]
  validationErrors?: WorklogSettingsValidationErrors
  actualHoursInput: string
  onActualHoursInputChange: (value: string) => void
  searchControlClassName: string
  dependencyKeywordInput: string
  onDependencyKeywordInputChange: (value: string) => void
  dependencySearchOpen: boolean
  onDependencySearchOpenChange: (open: boolean) => void
  filteredDependencyCandidates: WorklogFormDependencyOption[]
  selectedDependencies: WorklogFormDependencyOption[]
  onAddDependency: (dependencyId: number) => void
  onRemoveDependency: (dependencyId: number) => void
  incompleteDependencies: WorklogFormDependencyOption[]
  circularDependencyDetected: boolean
  statusChangeReasonVisible: boolean
}

export function WorklogSettingsModal({
  open,
  onOpenChange,
  values,
  onValuesChange,
  controlClassName,
  disableTeamChange,
  teamOptions,
  statusOptions,
  validationErrors,
  actualHoursInput,
  onActualHoursInputChange,
  searchControlClassName,
  dependencyKeywordInput,
  onDependencyKeywordInputChange,
  dependencySearchOpen,
  onDependencySearchOpenChange,
  filteredDependencyCandidates,
  selectedDependencies,
  onAddDependency,
  onRemoveDependency,
  incompleteDependencies,
  circularDependencyDetected,
  statusChangeReasonVisible,
}: WorklogSettingsModalProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="relative flex max-h-[86vh] max-w-4xl flex-col overflow-hidden rounded-2xl p-0">
        <DialogHeader className="shrink-0 px-7 pb-4 pt-7">
          <DialogTitle>작업 설정</DialogTitle>
          <DialogDescription>
            업무 상태, 담당자, 업무 소요 예상 시간과 진행 일정을 설정합니다.
          </DialogDescription>
        </DialogHeader>
        <div className="dashboard-scrollbar min-h-0 flex-1 overflow-y-auto px-7 pb-5 [scrollbar-gutter:stable]">
          <div className="grid gap-5">
            <ModalField label="상태 및 중요도">
              <div className="space-y-3">
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
                        importance:
                          event.target.value as WorklogFormValues["importance"],
                      })
                    }
                  />
                </div>
                <div
                  className={cn(
                    "space-y-3 overflow-hidden rounded-xl border border-border/70 bg-muted/20 p-4",
                    !statusChangeReasonVisible && "hidden"
                  )}
                >
                  <label className="text-sm font-semibold text-foreground">
                    변경 사유
                  </label>
                  <div className="rounded-xl border border-input bg-background/75 px-4 py-3 shadow-sm transition-all focus-within:border-primary/45 focus-within:ring-2 focus-within:ring-primary/15">
                    <textarea
                      className="h-[112px] w-full resize-none bg-transparent pr-2 text-sm leading-6 text-foreground outline-none placeholder:text-muted-foreground [scrollbar-color:theme(colors.slate.400)_transparent] [scrollbar-gutter:stable] [scrollbar-width:thin] [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-muted-foreground/35 [&::-webkit-scrollbar-track]:bg-transparent"
                      value={values.statusChangeReason ?? ""}
                      onChange={(event) =>
                        onValuesChange({
                          ...values,
                          statusChangeReason: event.target.value,
                        })
                      }
                      placeholder="상태를 변경하는 이유를 간단히 남겨주세요."
                      spellCheck={false}
                    />
                  </div>
                </div>
              </div>
            </ModalField>

              <ModalField label="담당 팀">
                <div className="grid gap-3">
                  <Select
                    className={cn(
                      controlClassName,
                      disableTeamChange &&
                        "border-border/60 bg-muted/70 text-muted-foreground shadow-none blur-[0.2px]"
                    )}
                    disabled={disableTeamChange}
                    value={String(values.teamId)}
                    options={teamOptions}
                    onChange={(event) =>
                      onValuesChange({ ...values, teamId: Number(event.target.value) })
                    }
                  />
                  {disableTeamChange ? (
                    <p className="text-xs leading-5 text-muted-foreground">
                      수정 화면에서는 담당 팀을 변경할 수 없습니다.
                    </p>
                  ) : null}
                </div>
              </ModalField>

              <ModalField label="선행 업무">
                <DependencyDropdown
                  status={values.status}
                  searchControlClassName={searchControlClassName}
                  dependencyKeywordInput={dependencyKeywordInput}
                  onDependencyKeywordInputChange={onDependencyKeywordInputChange}
                  dependencySearchOpen={dependencySearchOpen}
                  onDependencySearchOpenChange={onDependencySearchOpenChange}
                  filteredDependencyCandidates={filteredDependencyCandidates}
                  selectedDependencies={selectedDependencies}
                  onAddDependency={onAddDependency}
                  onRemoveDependency={onRemoveDependency}
                  incompleteDependencies={incompleteDependencies}
                  circularDependencyDetected={circularDependencyDetected}
                />
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
          </div>
        <DialogFooter className="shrink-0 items-center justify-between border-t border-border/70 bg-muted/20 px-7 py-4">
          <p className="mr-auto text-xs text-muted-foreground">
            설정을 확인한 뒤 반영 버튼으로 창을 닫습니다.
          </p>
          <Button
            type="button"
            className="h-10 min-w-28 rounded-lg px-5 font-semibold"
            onClick={() => onOpenChange(false)}
          >
            설정 반영
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

function DependencyDropdown({
  status,
  searchControlClassName,
  dependencyKeywordInput,
  onDependencyKeywordInputChange,
  dependencySearchOpen,
  onDependencySearchOpenChange,
  filteredDependencyCandidates,
  selectedDependencies,
  onAddDependency,
  onRemoveDependency,
  incompleteDependencies,
  circularDependencyDetected,
}: {
  status: WorklogFormValues["status"]
  searchControlClassName: string
  dependencyKeywordInput: string
  onDependencyKeywordInputChange: (value: string) => void
  dependencySearchOpen: boolean
  onDependencySearchOpenChange: (open: boolean) => void
  filteredDependencyCandidates: WorklogFormDependencyOption[]
  selectedDependencies: WorklogFormDependencyOption[]
  onAddDependency: (dependencyId: number) => void
  onRemoveDependency: (dependencyId: number) => void
  incompleteDependencies: WorklogFormDependencyOption[]
  circularDependencyDetected: boolean
}) {
  const handleDependencySearchBlur = (event: FocusEvent<HTMLDivElement>) => {
    const nextTarget = event.relatedTarget

    if (!(nextTarget instanceof Node) || !event.currentTarget.contains(nextTarget)) {
      onDependencySearchOpenChange(false)
    }
  }

  return (
    <div className="space-y-3">
      <div className="space-y-2" onBlur={handleDependencySearchBlur}>
        <div className="relative">
          <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            className={searchControlClassName}
            value={dependencyKeywordInput}
            onFocus={() => onDependencySearchOpenChange(true)}
            onChange={(event) => {
              onDependencyKeywordInputChange(event.target.value)
              onDependencySearchOpenChange(true)
            }}
            placeholder="제목, 요약, 담당자, 팀으로 검색"
          />
        </div>

        {dependencySearchOpen && dependencyKeywordInput.trim() ? (
          <div className="overflow-hidden rounded-xl border border-border bg-popover p-2 shadow-[0_18px_48px_-28px_rgba(15,23,42,0.65)]">
            <div className="dashboard-scrollbar max-h-[220px] overflow-y-auto [scrollbar-gutter:stable]">
              {filteredDependencyCandidates.length === 0 ? (
                <p className="px-3 py-3 text-sm text-muted-foreground">
                  조건에 맞는 선행 업무가 없습니다.
                </p>
              ) : (
                filteredDependencyCandidates.map((dependency) => (
                  <button
                    key={dependency.id}
                    type="button"
                    className="block w-full rounded-xl px-3 py-2.5 text-left"
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => onAddDependency(dependency.id)}
                  >
                    <span className="block text-sm font-semibold text-popover-foreground">
                      {dependency.title}
                    </span>
                    <span className="mt-1 block text-xs text-muted-foreground">
                      현재 상태: {getWorklogStatusLabel(dependency.status)}
                    </span>
                  </button>
                ))
              )}
            </div>
          </div>
        ) : null}
      </div>

      <DependencySelectionList
        dependencies={selectedDependencies}
        onRemoveDependency={onRemoveDependency}
      />

      {incompleteDependencies.length > 0 && status === "IN_PROGRESS" ? (
        <p className="text-xs text-[color:var(--warning)]">
          선행 업무가 아직 완료되지 않았습니다. 현재 와이어프레임에서는 경고만
          하고 저장은 허용합니다.
        </p>
      ) : null}
      {circularDependencyDetected ? (
        <p className="text-xs text-destructive">
          순환 의존성이 감지되었습니다. A → B → C → A 형태의 연결은 저장되지
          않습니다.
        </p>
      ) : null}
    </div>
  )
}

function DependencySelectionList({
  dependencies,
  onRemoveDependency,
}: {
  dependencies: WorklogFormDependencyOption[]
  onRemoveDependency: (dependencyId: number) => void
}) {
  if (dependencies.length === 0) {
    return (
        <p className="rounded-xl border border-dashed border-border/70 px-4 py-3 text-sm text-muted-foreground">
        선택한 선행 업무가 없습니다.
      </p>
    )
  }

  return (
    <div className="space-y-2">
      {dependencies.map((dependency) => (
        <div
          key={dependency.id}
          className="flex items-start justify-between gap-3 rounded-xl border border-border/70 bg-muted/25 px-4 py-3 text-sm"
        >
          <div className="min-w-0">
            <p className="truncate font-medium text-foreground">
              {dependency.title}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              현재 상태: {getWorklogStatusLabel(dependency.status)}
            </p>
          </div>
          <button
            type="button"
            className="flex size-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground"
            aria-label={`${dependency.title} 선행 업무 제거`}
            onClick={() => onRemoveDependency(dependency.id)}
          >
            <X className="size-4" />
          </button>
        </div>
      ))}
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
    <label className="group flex min-h-[76px] items-center gap-3 rounded-2xl border border-border/80 bg-background/75 px-4 py-3 shadow-sm focus-within:border-primary/45 focus-within:ring-2 focus-within:ring-primary/15">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-full border border-primary/20 bg-primary/10 text-primary">
        <CalendarDays className="size-5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
          {label}
        </span>
        <input
          type="date"
          className="schedule-date-input mt-1 h-10 w-full rounded-full border border-border/70 bg-background px-3 text-sm font-semibold text-foreground outline-none transition-colors [color-scheme:light] focus:border-primary dark:[color-scheme:dark]"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          aria-label={`${label} 날짜 입력`}
        />
      </span>
    </label>
  )
}
