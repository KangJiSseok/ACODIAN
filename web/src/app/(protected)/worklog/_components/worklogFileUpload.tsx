"use client"

import { useRef, useState } from "react"
import { Upload, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface WorklogFileUploadProps {
  attachmentNames: string[]
  onAddAttachmentFiles: (files: File[]) => void
  onRemoveAttachmentName: (name: string) => void
}

export function WorklogFileUpload({
  attachmentNames,
  onAddAttachmentFiles,
  onRemoveAttachmentName,
}: WorklogFileUploadProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const dragDepthRef = useRef(0)
  const [isDragging, setIsDragging] = useState(false)
  const hasFiles = attachmentNames.length > 0

  const openFilePicker = () => {
    fileInputRef.current?.click()
  }

  const addDroppedFiles = (files: FileList | null) => {
    const nextFiles = Array.from(files ?? [])
    if (nextFiles.length > 0) {
      onAddAttachmentFiles(nextFiles)
    }
  }

  return (
    <section
      className={cn(
        "rounded-[1.75rem] border border-border/70 bg-muted/35 p-5 transition-colors",
        isDragging && "border-primary/45 bg-primary/5"
      )}
      onDragEnter={(event) => {
        event.preventDefault()
        dragDepthRef.current += 1
        if (event.dataTransfer.types.includes("Files")) {
          setIsDragging(true)
        }
      }}
      onDragOver={(event) => {
        event.preventDefault()
        event.dataTransfer.dropEffect = "copy"
      }}
      onDragLeave={(event) => {
        event.preventDefault()
        dragDepthRef.current = Math.max(0, dragDepthRef.current - 1)
        if (dragDepthRef.current === 0) {
          setIsDragging(false)
        }
      }}
      onDrop={(event) => {
        event.preventDefault()
        dragDepthRef.current = 0
        setIsDragging(false)
        addDroppedFiles(event.dataTransfer.files)
      }}
    >
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
            FILES
          </p>
          <h3 className="mt-2 text-lg font-semibold text-foreground">
            첨부 파일
          </h3>
        </div>
        <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl border border-border/60 bg-input/90 text-primary shadow-sm">
          <Upload className="size-4" />
        </span>
      </div>

      <div
        className={cn(
          "rounded-2xl border border-dashed border-border/70 bg-input/95 p-5 shadow-sm transition-colors",
          isDragging && "border-primary/60 bg-primary/10"
        )}
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-foreground">파일 업로드</p>
            <p className="mt-1 text-xs text-muted-foreground">
              파일을 이 영역에 끌어다 놓거나 파일 선택 버튼으로 첨부합니다.
            </p>
            {hasFiles ? (
              <p className="mt-1 text-xs font-medium text-primary">
                현재 {attachmentNames.length}개 파일이 선택되었습니다.
              </p>
            ) : null}
          </div>
          <Button
            type="button"
            variant="secondary"
            className="h-10 rounded-lg px-4 text-sm"
            onClick={openFilePicker}
          >
            <Upload className="size-4" />
            파일 선택
          </Button>
        </div>
      </div>

      <div className="mt-3 space-y-2">
        {!hasFiles ? (
          <p className="rounded-2xl border border-dashed border-border/70 bg-card/45 px-4 py-3 text-sm text-muted-foreground">
            아직 업로드된 파일이 없습니다.
          </p>
        ) : (
          attachmentNames.map((name) => (
            <div
              key={name}
              className="flex items-center justify-between gap-3 rounded-2xl border border-border/70 bg-card/55 px-4 py-3 text-sm"
            >
              <span className="min-w-0 truncate font-medium text-foreground">
                {name}
              </span>
              <button
                type="button"
                className="flex size-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground"
                aria-label={`${name} 제거`}
                onClick={() => onRemoveAttachmentName(name)}
              >
                <X className="size-4" />
              </button>
            </div>
          ))
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        multiple
        className="hidden"
        onChange={(event) => {
          onAddAttachmentFiles(Array.from(event.target.files ?? []))
          event.target.value = ""
        }}
      />
    </section>
  )
}

export default WorklogFileUpload
