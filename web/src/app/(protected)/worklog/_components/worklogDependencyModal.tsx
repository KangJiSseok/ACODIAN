"use client"

import type { FocusEvent } from "react"
import { Search, X } from "lucide-react"
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
import type { worklogs } from "../_mock/worklog.mock"
import { getWorklogStatusLabel } from "../_utils/worklogFormat"
import type { WorklogFormValues } from "../_types/worklog.types"

type WorklogItem = (typeof worklogs)[number]

interface WorklogDependencyModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  status: WorklogFormValues["status"]
  searchControlClassName: string
  dependencyKeywordInput: string
  onDependencyKeywordInputChange: (value: string) => void
  dependencySearchOpen: boolean
  onDependencySearchOpenChange: (open: boolean) => void
  filteredDependencyCandidates: WorklogItem[]
  selectedDependencies: WorklogItem[]
  onAddDependency: (dependencyId: number) => void
  onRemoveDependency: (dependencyId: number) => void
  incompleteDependencies: WorklogItem[]
  circularDependencyDetected: boolean
}

export function WorklogDependencyModal({
  open,
  onOpenChange,
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
}: WorklogDependencyModalProps) {
  const handleDependencySearchBlur = (event: FocusEvent<HTMLDivElement>) => {
    const nextTarget = event.relatedTarget

    if (!(nextTarget instanceof Node) || !event.currentTarget.contains(nextTarget)) {
      onDependencySearchOpenChange(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="relative max-h-[86vh] max-w-3xl overflow-y-auto rounded-[28px] p-7">
        <DialogHeader>
          <DialogTitle>선행 업무</DialogTitle>
          <DialogDescription>
            완료 여부가 현재 업무 진행에 영향을 주는 업무를 검색해서 연결합니다.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-5">
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
              <div className="overflow-hidden rounded-2xl border border-border bg-popover p-2 shadow-[0_18px_48px_-28px_rgba(15,23,42,0.65)]">
                <div className="dashboard-scrollbar max-h-[260px] overflow-y-auto [scrollbar-gutter:stable]">
                  {filteredDependencyCandidates.length === 0 ? (
                    <p className="px-3 py-3 text-sm text-muted-foreground">
                      조건에 맞는 선행 업무가 없습니다.
                    </p>
                  ) : (
                    filteredDependencyCandidates.map((dependency) => (
                      <button
                        key={dependency.id}
                        type="button"
                        className="block w-full rounded-xl px-3 py-2.5 text-left transition-colors hover:bg-muted"
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
        <DialogFooter className="border-t border-border/70 pt-4">
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            닫기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function DependencySelectionList({
  dependencies,
  onRemoveDependency,
}: {
  dependencies: WorklogItem[]
  onRemoveDependency: (dependencyId: number) => void
}) {
  if (dependencies.length === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-border/70 px-4 py-3 text-sm text-muted-foreground">
        선택한 선행 업무가 없습니다.
      </p>
    )
  }

  return (
    <div className="space-y-2">
      {dependencies.map((dependency) => (
        <div
          key={dependency.id}
          className="flex items-start justify-between gap-3 rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm"
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
            className="flex size-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
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
