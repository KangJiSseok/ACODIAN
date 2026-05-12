"use client"

import Link from "next/link"
import { formatDate, formatHours, getAiStatusLabel } from "../_utils/worklogFormat"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { ImportanceBadge } from "./importanceBadge"
import { StatusBadge } from "./statusBadge"
import { useAuth } from "@/app/_common/hooks/useAuth"
import type { WorklogListItem } from "../_types/worklog.types"

export function WorklogPreviewDialog({
  open,
  onOpenChange,
  worklog,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  worklog?: WorklogListItem | null
  worklogId?: number | null
}) {
  const { user } = useAuth()
  const previewWorklog = worklog ?? null
  const teamName = previewWorklog?.teamName ?? "-"
  const authorName = previewWorklog?.authorName ?? "-"
  const actualHours = previewWorklog
    ? formatHours(previewWorklog.actualHours)
    : "-"
  const predecessorCount = previewWorklog?.predecessorCount ?? 0

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="worklog-root flex max-h-[88vh] max-w-5xl overflow-hidden rounded-[28px] p-0">
        {!previewWorklog ? (
          <div className="p-6 text-sm text-muted-foreground">
            미리보기할 업무를 찾을 수 없습니다.
          </div>
        ) : (
          <div className="flex max-h-[88vh] min-h-0 w-full flex-col">
            <div className="space-y-4 px-7 pb-5 pt-7">
              <DialogHeader className="mb-0 gap-3">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={previewWorklog.status} />
                  <ImportanceBadge importance={previewWorklog.importance} />
                </div>
                <DialogTitle className="max-w-3xl text-[28px] leading-tight tracking-[-0.05em]">
                  {previewWorklog.title}
                </DialogTitle>
              </DialogHeader>
            </div>

            <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto px-7 pb-6">
              <aside className="rounded-[24px] border border-border/70 bg-muted/20 p-4">
                <div className="mb-3 flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold text-foreground">업무 정보</p>
                  <span className="text-xs font-medium text-muted-foreground">
                    {formatDate(previewWorklog.dueDate)}
                  </span>
                </div>
                <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-[minmax(280px,2.2fr)_minmax(96px,0.7fr)_minmax(128px,0.9fr)_minmax(140px,0.9fr)_minmax(104px,0.7fr)]">
                  <InfoRow
                    label="팀"
                    value={teamName}
                    valueClassName="line-clamp-2 break-keep"
                  />
                  <InfoRow label="작성자" value={authorName} />
                  <InfoRow label="마감일" value={formatDate(previewWorklog.dueDate)} />
                  <InfoRow label="예상 소요 시간" value={actualHours} />
                  <InfoRow label="선행 업무" value={`${predecessorCount}건`} />
                </div>
              </aside>

              <section className="flex min-h-[260px] flex-col rounded-[24px] border border-border/70 bg-muted/25 p-5">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold text-foreground">AI 요약</p>
                  <span className="rounded-full border border-border/70 bg-background/70 px-3 py-1 text-xs font-semibold text-muted-foreground">
                    {getAiStatusLabel(previewWorklog.aiStatus)}
                  </span>
                </div>
                <p className="dashboard-scrollbar mt-4 max-h-[320px] overflow-y-auto pr-2 text-[15px] leading-7 text-muted-foreground [scrollbar-gutter:stable]">
                  {previewWorklog.aiSummary}
                </p>
              </section>
            </div>

            <div className="border-t border-border/70 px-7 py-5">
              <div className="flex flex-wrap justify-end gap-3">
                <Button variant="secondary" type="button" onClick={() => onOpenChange(false)}>
                  닫기
                </Button>
                <Button variant="outline" asChild>
                  <Link
                    href={`/worklog/detail/${previewWorklog.id}`}
                    onClick={() => onOpenChange(false)}
                  >
                    상세 페이지
                  </Link>
                </Button>
                {user?.userId === previewWorklog.authorId ? (
                  <Button asChild>
                    <Link
                      href={`/worklog/edit/${previewWorklog.id}`}
                      onClick={() => onOpenChange(false)}
                    >
                      업무 수정
                    </Link>
                  </Button>
                ) : null}
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

function InfoRow({
  label,
  value,
  className,
  valueClassName,
}: {
  label: string
  value: string
  className?: string
  valueClassName?: string
}) {
  return (
    <div className={`min-w-0 rounded-2xl bg-background/70 px-4 py-3 ${className ?? ""}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      <p
        className={`mt-1 text-[15px] font-semibold leading-6 text-foreground ${valueClassName ?? "truncate"}`}
        title={value}
      >
        {value}
      </p>
    </div>
  )
}
