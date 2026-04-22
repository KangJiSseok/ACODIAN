"use client"

import { useState } from "react"
import { worklogs } from "../_mock/worklog.mock"
import { StatusBadge } from "./statusBadge"
import { WorklogPreviewDialog } from "./worklogPreviewDialog"

export function DependencyGraph({ dependencyIds }: { dependencyIds: number[] }) {
  const dependencies = worklogs.filter((worklog) => dependencyIds.includes(worklog.id))
  const [previewWorklogId, setPreviewWorklogId] = useState<number | null>(null)

  if (dependencies.length === 0) {
    return <p className="text-sm text-muted-foreground">선행 업무가 없습니다.</p>
  }

  return (
    <>
      <div className="space-y-3">
        {dependencies.map((dependency) => (
          <button
            key={dependency.id}
            type="button"
            className="w-full rounded-xl border border-border bg-card p-4 text-left transition-colors hover:border-primary/35"
            onClick={() => setPreviewWorklogId(dependency.id)}
          >
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="font-medium">{dependency.title}</p>
                <p className="mt-1 text-sm text-muted-foreground">{dependency.aiSummary}</p>
                <p className="mt-2 text-xs text-muted-foreground">마감일 {dependency.dueDate}</p>
              </div>
              <StatusBadge status={dependency.status} />
            </div>
          </button>
        ))}
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
