import type { Worklog } from "../_types/worklog.types"
import { FileAiStatusBadge } from "./fileAiStatusBadge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { RefreshCw } from "lucide-react"

export function AiSummaryCard({
  worklog,
  canRetry = false,
  isRetrying = false,
  retryErrorMessage,
  onRetry,
}: {
  worklog: Worklog
  canRetry?: boolean
  isRetrying?: boolean
  retryErrorMessage?: string
  onRetry?: () => void
}) {
  const showRetryButton = canRetry && worklog.aiStatus === "FAILED"

  return (
    <Card className={worklog.aiStatus === "FAILED" ? "border-destructive/30 bg-destructive/5" : ""}>
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <CardTitle>AI 요약</CardTitle>
          <div className="flex items-center gap-2">
            {showRetryButton ? (
              <Button
                type="button"
                variant="secondary"
                size="sm"
                className="font-semibold"
                disabled={isRetrying}
                onClick={onRetry}
              >
                <RefreshCw className={isRetrying ? "size-4 animate-spin" : "size-4"} />
                AI 요약 다시 생성
              </Button>
            ) : null}
            <FileAiStatusBadge status={worklog.aiStatus} />
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-2">
        <p className="text-sm leading-6">{worklog.aiSummary}</p>
        {worklog.aiSummaryEdited ? (
          <p className="text-xs text-muted-foreground">사용자 수동 편집됨</p>
        ) : null}
        {retryErrorMessage ? (
          <p className="text-xs text-destructive">{retryErrorMessage}</p>
        ) : null}
      </CardContent>
    </Card>
  )
}
