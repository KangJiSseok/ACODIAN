"use client"

import Link from "next/link"
import { useState } from "react"
import { worklogs } from "../_mock/worklog.mock"
import type {
  WorklogDependencyItem,
  WorklogStatus,
} from "../_types/worklog.types"
import { StatusBadge } from "./statusBadge"
import { WorklogPreviewDialog } from "./worklogPreviewDialog"

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
  dependencyIds,
  dependencies: apiDependencies,
}: {
  dependencyIds: number[]
  dependencies?: WorklogDependencyItem[]
}) {
  const hasApiDependencies = Array.isArray(apiDependencies)
  const dependencies = hasApiDependencies
    ? apiDependencies.map((dependency) => ({
      id: dependency.worklogId,
      title: dependency.title,
      aiSummary: "",
      dueDate: "",
      status: toWorklogStatus(dependency.statusCode),
    }))
    : worklogs.filter((worklog) => dependencyIds.includes(worklog.id))
  const [previewWorklogId, setPreviewWorklogId] = useState<number | null>(null)

  if (dependencies.length === 0) {
    return <p className="text-sm text-muted-foreground">선행 업무가 없습니다.</p>
  }

  return (
    <>
      <div className="space-y-3">
        {dependencies.map((dependency) => {
          const content = (
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
          )

          return hasApiDependencies ? (
            <Link
              key={dependency.id}
              href={`/worklog/detail/${dependency.id}`}
              className="block w-full rounded-xl border border-border bg-card p-4 text-left transition-colors hover:border-primary/35"
            >
              {content}
            </Link>
          ) : (
            <button
              key={dependency.id}
              type="button"
              className="w-full rounded-xl border border-border bg-card p-4 text-left transition-colors hover:border-primary/35"
              onClick={() => setPreviewWorklogId(dependency.id)}
            >
              {content}
            </button>
          )
        })}
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
