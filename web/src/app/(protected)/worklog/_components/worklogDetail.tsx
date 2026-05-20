"use client"

import { type ReactNode, useState } from "react"
import type { Worklog, WorklogStatus } from "../_types/worklog.types"
import { AiSummaryCard } from "./aiSummaryCard"
import { DependencyGraph } from "./dependencyGraph"
import { FileAttachment } from "./fileAttachment"
import { ImportanceBadge } from "./importanceBadge"
import { StatusBadge } from "./statusBadge"
import { StatusHistory } from "./statusHistory"
import { StatusTransition } from "./statusTransition"
import { TagList } from "./tagList"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"
import { formatDate, formatDateTime, formatHours } from "../_utils/worklogFormat"
import { ChevronDown } from "lucide-react"

export function WorklogDetail({
  worklog,
  canTransition = false,
  onTransition = async () => {},
  isTransitionPending = false,
  transitionNotice,
  transitionErrorMessage,
  transitionDisabledMessage,
  canRetryAiSummary = false,
  isRetryingAiSummary = false,
  aiSummaryRetryErrorMessage,
  onRetryAiSummary,
}: {
  worklog: Worklog | null
  canTransition?: boolean
  onTransition?: (nextStatus: WorklogStatus, reason: string) => Promise<void>
  isTransitionPending?: boolean
  transitionNotice?: string
  transitionErrorMessage?: string
  transitionDisabledMessage?: string
  canRetryAiSummary?: boolean
  isRetryingAiSummary?: boolean
  aiSummaryRetryErrorMessage?: string
  onRetryAiSummary?: () => void
}) {
  const [isStatusTransitionOpen, setIsStatusTransitionOpen] = useState(false)
  const [isStatusHistoryOpen, setIsStatusHistoryOpen] = useState(false)

  if (!worklog) {
    return (
      <Card className="rounded-[28px]">
        <CardContent className="p-8 text-sm text-muted-foreground">
          선택된 업무가 없습니다.
        </CardContent>
      </Card>
    )
  }

  const authorName = worklog.authorName ?? "-"
  const createdAt = worklog.createdAt ? formatDateTime(worklog.createdAt) : "-"
  const updatedAt = worklog.updatedAt ? formatDateTime(worklog.updatedAt) : "-"

  return (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_minmax(320px,0.65fr)]">
      <div className="space-y-6">
        <Card>
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between gap-3">
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <StatusBadge status={worklog.status} />
                  <ImportanceBadge importance={worklog.importance} />
                </div>
                <div className="space-y-2">
                  <CardTitle className="text-2xl tracking-[-0.04em]">
                    {worklog.title}
                  </CardTitle>
                </div>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4 border-t border-border/70 pt-4">
            <section>
              <p className="text-sm font-medium">요청 내용</p>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                {worklog.requestContent || "상위 요청/지시 내용이 아직 입력되지 않았습니다."}
              </p>
            </section>
            <section>
              <p className="text-sm font-medium">업무 내용</p>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                {worklog.workContent}
              </p>
            </section>
            <section>
              <p className="text-sm font-medium">메타 태그</p>
              <div className="mt-2">
                <TagList tagNames={worklog.tagNames} />
              </div>
            </section>
          </CardContent>
        </Card>

        <AiSummaryCard
          worklog={worklog}
          canRetry={canRetryAiSummary}
          isRetrying={isRetryingAiSummary}
          retryErrorMessage={aiSummaryRetryErrorMessage}
          onRetry={onRetryAiSummary}
        />

        <Card>
          <CardHeader>
            <CardTitle>선행 업무</CardTitle>
          </CardHeader>
          <CardContent>
            <DependencyGraph
              dependencies={worklog.dependOnWorklogs}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>첨부 파일</CardTitle>
          </CardHeader>
          <CardContent>
            <FileAttachment files={worklog.fileItems} />
          </CardContent>
        </Card>

      </div>

      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>작성자 및 일정</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3 rounded-xl bg-muted/40 p-4">
              <Avatar className="size-11">
                <AvatarFallback>{authorName.slice(0, 1)}</AvatarFallback>
              </Avatar>
              <div>
                <p className="font-medium">{authorName}</p>
                <p className="text-sm text-muted-foreground">
                  {worklog.teamName ?? "-"}
                </p>
              </div>
            </div>
            <div className="grid gap-3">
              <InfoRow label="지시일" value={formatDate(worklog.instructionDate)} />
              <InfoRow label="마감일" value={formatDate(worklog.dueDate)} />
              <InfoRow label="업무 소요 예상 시간" value={formatHours(worklog.actualHours)} />
              <InfoRow
                label="완료일"
                value={worklog.completionDate ? formatDate(worklog.completionDate) : "-"}
              />
              <InfoRow
                label="AI 수동 편집"
                value={worklog.aiSummaryEdited ? "사용자 수정 완료" : "자동 생성 유지"}
              />
              <InfoRow label="생성일" value={createdAt} />
              <InfoRow label="수정일" value={updatedAt} />
            </div>
          </CardContent>
        </Card>

        <CollapsibleCard
          title="상태 변경"
          open={isStatusTransitionOpen}
          onOpenChange={setIsStatusTransitionOpen}
        >
          <StatusTransition
            worklog={worklog}
            canTransition={canTransition}
            onTransition={onTransition}
            isPending={isTransitionPending}
            disabledMessage={transitionDisabledMessage}
          />
          {transitionErrorMessage ? (
            <div className="rounded-xl border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {transitionErrorMessage}
            </div>
          ) : null}
          {transitionNotice ? (
            <div className="rounded-xl border border-warning/40 bg-warning/10 px-4 py-3 text-sm text-[color:var(--warning)]">
              {transitionNotice}
            </div>
          ) : null}
        </CollapsibleCard>

        <CollapsibleCard
          title="상태 이력"
          open={isStatusHistoryOpen}
          onOpenChange={setIsStatusHistoryOpen}
        >
          <StatusHistory worklog={worklog} />
        </CollapsibleCard>
      </div>
    </div>
  )
}

export default WorklogDetail

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-muted/40 px-4 py-3 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium">{value}</span>
    </div>
  )
}

function CollapsibleCard({
  title,
  open,
  onOpenChange,
  children,
}: {
  title: string
  open: boolean
  onOpenChange: (open: boolean) => void
  children: ReactNode
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-3">
        <CardTitle>{title}</CardTitle>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-expanded={open}
          aria-label={`${title} ${open ? "접기" : "펼치기"}`}
          onClick={() => onOpenChange(!open)}
        >
          <ChevronDown
            className={`size-4 transition-transform ${open ? "rotate-180" : ""}`}
          />
        </Button>
      </CardHeader>
      <CardContent className={cn("space-y-3", !open && "hidden")}>
        {children}
      </CardContent>
    </Card>
  )
}
