"use client"

import { useRef } from "react"
import { Upload, X } from "lucide-react"
import { Button } from "@/components/ui/button"

interface WorklogFileUploadProps {
  attachmentNames: string[]
  onAddAttachmentNames: (names: string[]) => void
  onRemoveAttachmentName: (name: string) => void
}

export function WorklogFileUpload({
  attachmentNames,
  onAddAttachmentNames,
  onRemoveAttachmentName,
}: WorklogFileUploadProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)

  return (
    <section className="rounded-[1.75rem] border border-border/70 bg-muted/35 p-5">
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

      <div className="rounded-2xl border border-border/60 bg-input/95 p-4 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-foreground">파일 업로드</p>
            <p className="mt-1 text-xs text-muted-foreground">
              업무와 관련된 문서, 이미지, 자료 파일을 첨부합니다.
            </p>
          </div>
          <Button
            type="button"
            variant="secondary"
            className="h-10 rounded-2xl px-4 text-sm"
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="size-4" />
            파일 선택
          </Button>
        </div>
      </div>

      <div className="mt-3 space-y-2">
        {attachmentNames.length === 0 ? (
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
                className="flex size-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
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
          const nextNames = Array.from(event.target.files ?? []).map(
            (file) => file.name,
          )
          onAddAttachmentNames(nextNames)
          event.target.value = ""
        }}
      />
    </section>
  )
}

export default WorklogFileUpload
