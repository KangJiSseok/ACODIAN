import type { Worklog } from "../_types/worklog.types"
import { formatDateTime, getWorklogStatusLabel } from "../_utils/worklogFormat"

export function StatusHistory({ worklog }: { worklog: Worklog }) {
  return (
    <div className="space-y-3">
      {worklog.statusHistory.length === 0 ? (
        <div className="rounded-lg bg-muted/40 px-4 py-3 text-sm text-muted-foreground">
          상태 변경 이력이 없습니다.
        </div>
      ) : (
        worklog.statusHistory.map((history) => (
          <div
            key={history.id}
            className="rounded-lg border border-dashed border-border bg-muted/20 px-4 py-3 text-sm"
          >
            <div className="flex items-center justify-between gap-3">
              <div className="font-medium">
                {history.previousStatus
                  ? `${getWorklogStatusLabel(history.previousStatus)} → ${getWorklogStatusLabel(history.newStatus)}`
                  : `${getWorklogStatusLabel(history.newStatus)} 등록`}
              </div>
              <span className="text-xs text-muted-foreground">
                {formatDateTime(history.changedAt)}
              </span>
            </div>
            <p className="mt-2 text-muted-foreground">
              변경자: {history.changedByName ?? "-"}
            </p>
            <p className="mt-1 text-muted-foreground">사유: {history.reason}</p>
          </div>
        ))
      )}
    </div>
  )
}
