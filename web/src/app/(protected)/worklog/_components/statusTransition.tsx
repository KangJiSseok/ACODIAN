"use client"

import { useMemo, useState } from "react"
import type { WorklogStatus, Worklog } from "../_types/worklog.types"
import { Button } from "@/components/ui/button"
import { Select } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { getWorklogStatusLabel } from "../_utils/worklogFormat"
import { StatusBadge } from "./statusBadge"

const nextMap: Record<WorklogStatus, WorklogStatus[]> = {
  PENDING: ["IN_PROGRESS"],
  IN_PROGRESS: ["DONE", "ON_HOLD", "FAILED", "CANCELLED"],
  ON_HOLD: ["IN_PROGRESS", "FAILED"],
  DONE: ["IN_PROGRESS"],
  FAILED: ["IN_PROGRESS"],
  CANCELLED: [],
}

export function StatusTransition({
  worklog,
  canTransition,
  onTransition,
  disabledMessage = "현재 역할에서는 이 업무 상태를 변경할 수 없습니다.",
}: {
  worklog: Worklog
  canTransition: boolean
  onTransition: (nextStatus: WorklogStatus, reason: string) => Promise<void>
  disabledMessage?: string
}) {
  const [draft, setDraft] = useState<{
    worklogId: number
    currentStatus: WorklogStatus
    nextStatus: WorklogStatus
    reason: string
  }>({
    worklogId: worklog.id,
    currentStatus: worklog.status,
    nextStatus: worklog.status,
    reason: "",
  })
  const isDraftCurrent =
    draft.worklogId === worklog.id && draft.currentStatus === worklog.status
  const nextStatus = isDraftCurrent ? draft.nextStatus : worklog.status
  const reason = isDraftCurrent ? draft.reason : ""
  const availableStatuses = useMemo(
    () => Array.from(new Set([worklog.status, ...nextMap[worklog.status]])),
    [worklog.status]
  )
  const statusOptions = availableStatuses.map((status) => ({
    label:
      status === worklog.status
        ? `${getWorklogStatusLabel(status)} (현재)`
        : getWorklogStatusLabel(status),
    value: status,
  }))
  const dependencyWarning = useMemo(() => {
    if (!canTransition) return false
    return (worklog.dependOnWorklogs ?? []).some(
      (dependency) => dependency.statusCode !== "COMPLETED" && dependency.statusCode !== "DONE"
    )
  }, [canTransition, worklog.dependOnWorklogs])
  const hasNextStatus = nextMap[worklog.status].length > 0
  const isStatusChanged = nextStatus !== worklog.status
  const canSubmit = canTransition && hasNextStatus && isStatusChanged

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3 rounded-lg bg-muted/40 px-4 py-3 text-sm">
        <span className="text-muted-foreground">현재 상태</span>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <StatusBadge status={worklog.status} />
        </div>
      </div>

      {!canTransition ? (
        <p className="text-sm text-muted-foreground">{disabledMessage}</p>
      ) : null}

      {dependencyWarning && nextStatus === "IN_PROGRESS" ? (
        <div className="rounded-xl border border-warning/40 bg-warning/10 px-4 py-3 text-sm text-[color:var(--warning)]">
          선행 업무가 아직 완료되지 않았습니다. 진행 중으로 전환할 경우 경고 메시지만
          표시되고 차단되지는 않습니다.
        </div>
      ) : null}

      <div className="space-y-2">
        <p className="text-sm font-medium">변경할 상태</p>
        <Select
          value={nextStatus}
          onChange={(event) =>
            setDraft({
              worklogId: worklog.id,
              currentStatus: worklog.status,
              nextStatus: event.target.value as WorklogStatus,
              reason,
            })
          }
          options={statusOptions}
          disabled={!canTransition || !hasNextStatus}
        />
        {!hasNextStatus ? (
          <p className="text-sm text-muted-foreground">최종 상태입니다.</p>
        ) : null}
      </div>

      <div className="space-y-2">
        <p className="text-sm font-medium">변경 사유</p>
        <Textarea
          value={reason}
          onChange={(event) =>
            setDraft({
              worklogId: worklog.id,
              currentStatus: worklog.status,
              nextStatus,
              reason: event.target.value,
            })
          }
          placeholder="상태 변경 사유를 기록하세요."
          disabled={!canTransition || !hasNextStatus}
          className="min-h-28"
        />
      </div>

      <div className="flex justify-end">
        <Button
          type="button"
          disabled={!canSubmit}
          onClick={async () => {
            await onTransition(nextStatus, reason)
            setDraft({
              worklogId: worklog.id,
              currentStatus: nextStatus,
              nextStatus,
              reason: "",
            })
          }}
        >
          상태 변경
        </Button>
      </div>
    </div>
  )
}
