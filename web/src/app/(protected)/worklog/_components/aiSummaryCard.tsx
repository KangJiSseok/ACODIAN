import type { Worklog } from "../_types/worklog.types"
import { FileAiStatusBadge } from "./fileAiStatusBadge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export function AiSummaryCard({ worklog }: { worklog: Worklog }) {
  return (
    <Card className={worklog.aiStatus === "FAILED" ? "border-destructive/30 bg-destructive/5" : ""}>
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <CardTitle>AI 요약</CardTitle>
          <FileAiStatusBadge status={worklog.aiStatus} />
        </div>
      </CardHeader>
      <CardContent className="space-y-2">
        <p className="text-sm leading-6">{worklog.aiSummary}</p>
        {worklog.aiSummaryEdited ? (
          <p className="text-xs text-muted-foreground">사용자 수동 편집됨</p>
        ) : null}
      </CardContent>
    </Card>
  )
}
