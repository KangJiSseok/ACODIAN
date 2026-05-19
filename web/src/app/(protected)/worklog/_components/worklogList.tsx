"use client"

import Link from "next/link"
import { useState } from "react"
import type { WorklogListItem } from "../_types/worklog.types"
import { ImportanceBadge } from "./importanceBadge"
import { StatusBadge } from "./statusBadge"
import { useAuth } from "@/app/_common/hooks/useAuth"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { getAiStatusLabel } from "../_utils/worklogFormat"
import { WorklogPreviewDialog } from "./worklogPreviewDialog"

export function WorklogList({
  worklogs,
  selectedWorklogId,
  onSelect,
}: {
  worklogs: WorklogListItem[]
  selectedWorklogId?: number | null
  onSelect?: (worklogId: number) => void
}) {
  const { user } = useAuth()
  const [previewWorklog, setPreviewWorklog] = useState<WorklogListItem | null>(
    null
  )

  return (
    <>
      <div className="grid gap-3">
        {worklogs.length === 0 ? (
          <div className="workspace-empty rounded-xl px-6 py-10 text-center text-sm">
            조건에 맞는 업무가 없습니다.
          </div>
        ) : (
          worklogs.map((worklog) => (
            <Card
              key={worklog.id}
              role="button"
              tabIndex={0}
              onClick={() => {
                if (onSelect) {
                  onSelect(worklog.id)
                  return
                }
                setPreviewWorklog(worklog)
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault()
                  if (onSelect) {
                    onSelect(worklog.id)
                    return
                  }
                  setPreviewWorklog(worklog)
                }
              }}
              className={`cursor-pointer rounded-[24px] transition-all duration-300 hover:-translate-y-1 ${
                selectedWorklogId === worklog.id ? "ring-2 ring-primary/40" : ""
              }`}
            >
              <CardContent className="flex flex-col gap-5 p-5 lg:flex-row lg:items-stretch lg:justify-between">
                <div className="min-w-0 flex-1 space-y-4">
                  <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-[18px] font-semibold tracking-[-0.03em] text-foreground">
                        {worklog.title}
                      </p>
                      <StatusBadge status={worklog.status} />
                      <ImportanceBadge importance={worklog.importance} />
                    </div>
                    <p className="line-clamp-2 max-w-4xl text-sm leading-6 text-muted-foreground">
                      {worklog.aiSummary}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      AI 상태 {getAiStatusLabel(worklog.aiStatus)} · 선행 업무{" "}
                      {worklog.predecessorCount}건
                    </p>
                  </div>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
                    <div className="flex min-w-0 items-center gap-2">
                      <span className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                        팀
                      </span>
                      <span className="truncate font-medium text-foreground">
                        {worklog.teamName}
                      </span>
                    </div>
                    <span className="hidden h-4 w-px bg-border/80 md:block" />
                    <div className="flex items-center gap-2">
                      <span className="text-[13px] font-medium text-foreground">
                        {worklog.authorName}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex shrink-0 flex-col items-start justify-between gap-4 text-sm lg:w-[250px] lg:border-l lg:border-border/70 lg:pl-5">
                  <div className="grid w-full grid-cols-2 gap-5 text-left lg:text-right">
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                        지시일
                      </p>
                      <p className="mt-1 font-semibold text-foreground">
                        {worklog.instructionDate}
                      </p>
                    </div>
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                        마감일
                      </p>
                      <p className="mt-1 font-semibold text-foreground">
                        {worklog.dueDate}
                      </p>
                    </div>
                  </div>
                  <div className="grid w-full gap-3">
                    <Button
                      variant="secondary"
                      className="h-11 w-full px-5 text-sm font-semibold"
                      asChild
                    >
                      <Link
                        href={`/worklog/detail/${worklog.id}`}
                        onClick={(event) => event.stopPropagation()}
                      >
                        상세
                      </Link>
                    </Button>
                    {user?.userId === worklog.authorId ? (
                      <Button
                        variant="default"
                        className="h-11 w-full px-5 text-sm font-semibold"
                        asChild
                      >
                        <Link
                          href={`/worklog/edit/${worklog.id}`}
                          onClick={(event) => event.stopPropagation()}
                        >
                          수정
                        </Link>
                      </Button>
                    ) : null}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>
      <WorklogPreviewDialog
        open={previewWorklog !== null}
        onOpenChange={(open) => {
          if (!open) setPreviewWorklog(null)
        }}
        worklog={previewWorklog}
      />
    </>
  )
}

export default WorklogList
