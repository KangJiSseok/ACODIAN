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
      <DialogContent className="worklog-root max-w-3xl rounded-[28px] p-0">
        {!previewWorklog ? (
          <div className="p-6 text-sm text-muted-foreground">
            미리보기할 업무를 찾을 수 없습니다.
          </div>
        ) : (
          <div className="space-y-6 p-7">
            <DialogHeader className="mb-0 gap-3">
              <div className="flex flex-wrap items-center gap-2">
                <StatusBadge status={previewWorklog.status} />
                <ImportanceBadge importance={previewWorklog.importance} />
              </div>
              <DialogTitle className="text-[26px] tracking-[-0.05em]">
                {previewWorklog.title}
              </DialogTitle>
            </DialogHeader>

            <div className="grid gap-6 lg:grid-cols-[1.25fr_0.75fr]">
              <div className="space-y-4">
                <section className="rounded-2xl border border-border/70 bg-muted/25 p-5">
                  <p className="text-sm font-semibold text-foreground">업무 내용</p>
                  <p className="mt-3 text-[15px] leading-7 text-muted-foreground">
                    {previewWorklog.workContent}
                  </p>
                </section>

                <section className="rounded-2xl border border-border/70 bg-muted/25 p-5">
                  <p className="text-sm font-semibold text-foreground">AI 요약</p>
                  <p className="mt-3 text-[15px] leading-7 text-muted-foreground">
                    {previewWorklog.aiSummary}
                  </p>
                </section>
              </div>

              <div className="space-y-3">
                <InfoRow label="팀" value={teamName} />
                <InfoRow label="작성자" value={authorName} />
                <InfoRow label="마감일" value={formatDate(previewWorklog.dueDate)} />
                <InfoRow label="업무시간" value={actualHours} />
                <InfoRow label="AI 상태" value={getAiStatusLabel(previewWorklog.aiStatus)} />
                <InfoRow label="선행 업무" value={`${predecessorCount}건`} />
              </div>
            </div>

            <div className="flex flex-wrap justify-end gap-3 border-t border-border/70 pt-6">
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
        )}
      </DialogContent>
    </Dialog>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
      <p className="text-[12px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      <p className="mt-1 text-[15px] font-semibold text-foreground">{value}</p>
    </div>
  )
}
