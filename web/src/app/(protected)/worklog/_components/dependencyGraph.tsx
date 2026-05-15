import Link from "next/link"
import { ChevronRight } from "lucide-react"
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
          className="group block w-full rounded-xl border border-border bg-card p-4 text-left transition-colors hover:border-primary/35 hover:bg-primary/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          aria-label={`${dependency.title} 선행 업무 상세 보기`}
        >
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
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
            <div className="flex shrink-0 items-center gap-3">
              <StatusBadge status={dependency.status} />
              <span className="inline-flex items-center gap-1 text-xs font-semibold text-muted-foreground transition-colors group-hover:text-primary">
                상세 보기
                <ChevronRight className="size-3.5 transition-transform group-hover:translate-x-0.5" />
              </span>
            </div>
          </div>
        </Link>
      ))}
    </div>
  )
}
