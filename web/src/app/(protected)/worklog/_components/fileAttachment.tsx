import { files } from "../_mock/worklog.mock"
import { FileAiStatusBadge } from "./fileAiStatusBadge"

export function FileAttachment({ fileIds }: { fileIds: number[] }) {
  const attached = files.filter((file) => fileIds.includes(file.id))

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
