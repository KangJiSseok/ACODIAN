"use client"

import { useMemo, useState } from "react"
import type { WorklogStatus, Worklog } from "../_types/worklog.types"
import { worklogs } from "../_mock/worklog.mock"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { getWorklogStatusLabel } from "../_utils/worklogFormat"

const nextMap: Record<WorklogStatus, WorklogStatus[]> = {
  PENDING: ["IN_PROGRESS"],
  IN_PROGRESS: ["DONE", "ON_HOLD", "FAILED", "CANCELLED"],
  ON_HOLD: ["IN_PROGRESS", "FAILED"],
  DONE: [],
  FAILED: ["IN_PROGRESS"],
  CANCELLED: [],
}

export function StatusTransition({
  worklog,
  canTransition,
  onTransition,
}: {
  worklog: Worklog
  canTransition: boolean
  onTransition: (nextStatus: WorklogStatus, reason: string) => Promise<void>
}) {
  const [reason, setReason] = useState("")
  const dependencyWarning = useMemo(() => {
    // 선행 업무 미완료 여부는 차단이 아니라 전환 전 경고 표시 용도로만 사용합니다.
    return worklog.dependencyIds.some((dependencyId) => {
      const dependency = worklogs.find((item) => item.id === dependencyId)
      return dependency && dependency.status !== "DONE"
    })
  }, [worklog.dependencyIds])

  if (!canTransition) {
    return (
      <p className="text-sm text-muted-foreground">
        현재 역할에서는 이 업무 상태를 변경할 수 없습니다.
      </p>
    )
  }

  return (
    <div className="space-y-3">
      {nextMap[worklog.status].length === 0 ? (
        <p className="text-sm text-muted-foreground">최종 상태입니다.</p>
      ) : (
        <>
          {dependencyWarning && worklog.status !== "IN_PROGRESS" ? (
            <div className="rounded-xl border border-warning/40 bg-warning/10 px-4 py-3 text-sm text-[color:var(--warning)]">
              선행 업무가 아직 완료되지 않았습니다. 진행중으로 전환할 경우 경고 메시지만
              표시되고 차단되지는 않습니다.
            </div>
          ) : null}
          <Textarea
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="상태 변경 사유를 기록하세요."
          />
          <div className="flex flex-wrap gap-2">
            {nextMap[worklog.status].map((nextStatus) => (
              <Button
                key={nextStatus}
                size="sm"
                variant="outline"
                type="button"
                onClick={async () => {
                  await onTransition(nextStatus, reason)
                  setReason("")
                }}
              >
                {getWorklogStatusLabel(nextStatus)}로 변경
              </Button>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
