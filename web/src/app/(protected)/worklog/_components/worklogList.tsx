"use client"

import Link from "next/link"
import { useState } from "react"
import { canEditWorklog } from "../_utils/accessControl"
import { useAuth } from "../_hooks/useAuth"
import { teams, users } from "../_mock/worklog.mock"
import type { Worklog } from "../_types/worklog.types"
import { ImportanceBadge } from "./importanceBadge"
import { StatusBadge } from "./statusBadge"
import { WorklogPreviewDialog } from "./worklogPreviewDialog"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { CardContent } from "@/components/ui/card"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { getAiStatusLabel } from "../_utils/worklogFormat"

export function WorklogList({
  worklogs,
  selectedWorklogId,
  onSelect,
}: {
  worklogs: Worklog[]
  selectedWorklogId?: number | null
  onSelect?: (worklogId: number) => void
}) {
  const { user } = useAuth()
  const [previewWorklogId, setPreviewWorklogId] = useState<number | null>(null)

  return (
    <>
      <div className="grid gap-3">
        {worklogs.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border/70 bg-muted/30 px-6 py-10 text-center text-sm text-muted-foreground">
            조건에 맞는 업무가 없습니다.
          </div>
        ) : (
          worklogs.map((worklog) => {
            const author = users.find((user) => user.id === worklog.authorId)
            const team = teams.find((item) => item.id === worklog.teamId)

            return (
              <CardSpotlight
                key={worklog.id}
                role="button"
                tabIndex={0}
                onClick={() => {
                  if (onSelect) {
                    onSelect(worklog.id)
                    return
                  }
                  setPreviewWorklogId(worklog.id)
                }}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault()
                    // 카드 전체를 버튼처럼 쓰기 때문에 키보드 접근도 동일한 선택 흐름으로 맞춥니다.
                    if (onSelect) {
                      onSelect(worklog.id)
                      return
                    }
                    setPreviewWorklogId(worklog.id)
                  }
                }}
                className={`group cursor-pointer rounded-[24px] transition-all duration-300 hover:-translate-y-1 ${
                  selectedWorklogId === worklog.id ? "ring-2 ring-primary/40" : ""
                }`}
              >
                <CardContent className="flex flex-col gap-5 p-5 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0 flex-1 space-y-4">
                    <div className="space-y-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-[18px] font-semibold tracking-[-0.03em] text-foreground transition-colors group-hover/card-spotlight:text-primary">
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
                        {worklog.dependencyIds.length}건
                      </p>
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
                      <div className="flex min-w-0 items-center gap-2">
                        <span className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                          팀
                        </span>
                        <span className="truncate font-medium text-foreground">
                          {team?.name}
                        </span>
                      </div>
                      {author ? (
                        <>
                          <span className="hidden h-4 w-px bg-border/80 md:block" />
                          <div className="flex items-center gap-2">
                            <Avatar className="size-6">
                              <AvatarImage src={author.profileImage} alt={author.name} />
                              <AvatarFallback className="bg-primary/20 text-[10px] font-bold text-primary">
                                {author.name.slice(0, 1)}
                              </AvatarFallback>
                            </Avatar>
                            <span className="text-[13px] font-medium text-foreground">
                              {author.name}
                            </span>
                          </div>
                        </>
                      ) : null}
                    </div>
                  </div>

                  <div className="flex shrink-0 flex-col items-start gap-4 text-sm lg:items-end">
                    <div className="text-left lg:text-right">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                        마감일
                      </p>
                      <p className="mt-1 font-semibold text-foreground">
                        {worklog.dueDate}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <Button
                        variant="secondary"
                        className="h-11 min-w-28 px-5 text-sm font-semibold"
                        asChild
                      >
                        <Link
                          href={`/worklog/detail/${worklog.id}`}
                          onClick={(event) => event.stopPropagation()}
                        >
                          상세
                        </Link>
                      </Button>
                      {canEditWorklog(user, worklog) ? (
                        <Button
                          variant="default"
                          className="h-11 min-w-28 px-5 text-sm font-semibold"
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
              </CardSpotlight>
            )
          })
        )}
      </div>
      <WorklogPreviewDialog
        open={previewWorklogId !== null}
        onOpenChange={(open) => {
          if (!open) setPreviewWorklogId(null)
        }}
        worklogId={previewWorklogId}
      />
    </>
  )
}

export default WorklogList
