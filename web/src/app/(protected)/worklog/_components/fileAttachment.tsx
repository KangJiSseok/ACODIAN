import type { WorklogFileItem } from "../_types/worklog.types"
import { FileAiStatusBadge } from "./fileAiStatusBadge"

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${(bytes / 1024 / 1024).toFixed(1)}MB`
}

export function FileAttachment({
  files: apiFiles,
}: {
  files?: WorklogFileItem[]
}) {
  const attached = (apiFiles ?? []).map((file) => ({
      id: file.fileId,
      originalName: file.originalName,
      summaryPreview: file.storedPath,
      type: file.fileExtension.toUpperCase(),
      size: formatFileSize(file.fileSizeBytes),
      aiStatus: "DONE" as const,
    }))

  if (attached.length === 0) {
    return <p className="text-sm text-muted-foreground">첨부 파일이 없습니다.</p>
  }

  return (
    <div className="space-y-3">
      {attached.map((file) => (
        <div key={file.id} className="rounded-xl border border-border bg-card p-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="font-medium">{file.originalName}</p>
              <p className="mt-1 text-sm text-muted-foreground">{file.summaryPreview}</p>
              <p className="mt-2 text-xs text-muted-foreground">
                {file.type} / {file.size}
              </p>
            </div>
            <FileAiStatusBadge status={file.aiStatus} />
          </div>
        </div>
      ))}
    </div>
  )
}
