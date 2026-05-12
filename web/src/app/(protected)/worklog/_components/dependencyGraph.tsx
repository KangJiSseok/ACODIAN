import Link from "next/link"
import type {
  WorklogDependencyItem,
  WorklogStatus,
} from "../_types/worklog.types"
import { StatusBadge } from "./statusBadge"

const worklogStatusCodeMap: Record<string, WorklogStatus> = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "DONE",
  DONE: "DONE",
  ON_HOLD: "ON_HOLD",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
}

function toWorklogStatus(statusCode: string): WorklogStatus {
  return worklogStatusCodeMap[statusCode] ?? "PENDING"
}

export function DependencyGraph({
  dependencies: apiDependencies,
}: {
  dependencies?: WorklogDependencyItem[]
}) {
  const dependencies = (apiDependencies ?? []).map((dependency) => ({
      id: dependency.worklogId,
      title: dependency.title,
      aiSummary: "",
      dueDate: "",
      status: toWorklogStatus(dependency.statusCode),
    }))

  if (dependencies.length === 0) {
    return <p className="text-sm text-muted-foreground">선행 업무가 없습니다.</p>
  }

  return (
    <div className="space-y-3">
      {dependencies.map((dependency) => (
        <Link
          key={dependency.id}
          href={`/worklog/detail/${dependency.id}`}
          className="block w-full rounded-xl border border-border bg-card p-4 text-left transition-colors hover:border-primary/35"
        >
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="font-medium">{dependency.title}</p>
              {dependency.aiSummary ? (
                <p className="mt-1 text-sm text-muted-foreground">
                  {dependency.aiSummary}
                </p>
              ) : null}
              {dependency.dueDate ? (
                <p className="mt-2 text-xs text-muted-foreground">
                  마감일 {dependency.dueDate}
                </p>
              ) : null}
            </div>
            <StatusBadge status={dependency.status} />
          </div>
        </Link>
      ))}
    </div>
  )
}
