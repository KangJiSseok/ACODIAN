"use client"

import Link from "next/link"
import { teams, users, worklogs } from "../_mock/worklog.mock"
import { formatDate, formatHours } from "../_utils/worklogFormat"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Separator } from "@/components/ui/separator"
import { ImportanceBadge } from "./importanceBadge"
import { StatusBadge } from "./statusBadge"
import { TagList } from "./tagList"
import type { WorklogListItem } from "../_types/worklog.types"

export function WorklogPreviewDialog({
  open,
  onOpenChange,
  worklog,
  worklogId,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  worklog?: WorklogListItem | null
  worklogId?: number | null
}) {
  const mockWorklog = worklogs.find(
    (item) => item.id === worklogId && !item.isDeleted
  )
  const previewWorklog = worklog ?? mockWorklog
  const author = previewWorklog
    ? users.find((user) => user.id === previewWorklog.authorId)
    : undefined
  const team = previewWorklog
    ? teams.find((item) => item.id === previewWorklog.teamId)
    : undefined
  const teamName = worklog?.teamName ?? team?.name ?? "-"
  const authorName = worklog?.authorName ?? author?.name ?? "-"
  const actualHours = previewWorklog
    ? formatHours(previewWorklog.actualHours)
    : "-"
  const tagIds = mockWorklog?.tagIds ?? []

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="worklog-root max-w-2xl rounded-[28px] p-0">
        {!previewWorklog ? (
          <div className="p-6 text-sm text-muted-foreground">
            미리보기할 업무를 찾을 수 없습니다.
          </div>
        ) : (
          <div className="space-y-5 p-6">
            <DialogHeader>
              <div className="mb-2 flex flex-wrap gap-2">
                <StatusBadge status={previewWorklog.status} />
                <ImportanceBadge importance={previewWorklog.importance} />
              </div>
              <DialogTitle className="text-2xl tracking-[-0.04em]">
                {previewWorklog.title}
              </DialogTitle>
              <DialogDescription>{previewWorklog.aiSummary}</DialogDescription>
            </DialogHeader>

            <Separator />

            <div className="grid gap-3 text-sm sm:grid-cols-2">
              <PreviewMeta label="팀" value={teamName} />
              <PreviewMeta label="작성자" value={authorName} />
              <PreviewMeta label="업무 시간" value={actualHours} />
              <PreviewMeta
                label="지시일"
                value={formatDate(previewWorklog.instructionDate)}
              />
              <PreviewMeta label="마감일" value={formatDate(previewWorklog.dueDate)} />
            </div>

            {tagIds.length > 0 ? (
              <div className="space-y-2">
                <p className="text-sm font-semibold text-foreground">태그</p>
                <TagList tagIds={tagIds} />
              </div>
            ) : null}

            <DialogFooter>
              <Button variant="outline" type="button" onClick={() => onOpenChange(false)}>
                닫기
              </Button>
              <Button asChild>
                <Link
                  href={`/worklog/detail/${previewWorklog.id}`}
                  className="text-primary-foreground"
                >
                  상세 보기
                </Link>
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

function PreviewMeta({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      <p className="mt-1 font-medium text-foreground">{value}</p>
    </div>
  )
}
