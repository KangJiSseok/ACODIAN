"use client";

import Link from "next/link";
import type { AiProcessingStatus } from "@/app/(protected)/worklog/_types/worklog.types";
import { FileAiStatusBadge } from "@/app/(protected)/worklog/_components/fileAiStatusBadge";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { cn } from "@/lib/utils";
import type { FileItem } from "../_types/file.types";

export function FileCard({
  file,
  selected,
  onToggleSelect,
}: {
  file: FileItem;
  selected: boolean;
  onToggleSelect: (fileId: number, checked: boolean) => void;
}) {
  const worklog = file.worklog;

  return (
    <CardSpotlight
      role="button"
      tabIndex={0}
      onClick={() => onToggleSelect(file.id, !selected)}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          onToggleSelect(file.id, !selected);
        }
      }}
      className={cn(
        "group cursor-pointer rounded-[24px] transition-all duration-300 hover:-translate-y-1",
        selected && "ring-2 ring-primary/60 ring-offset-2 ring-offset-background",
      )}
    >
      <CardContent className="flex flex-col gap-5 p-5 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 flex-1 space-y-4">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <p className="break-words text-[18px] font-semibold tracking-[-0.03em] text-foreground transition-colors group-hover/card-spotlight:text-primary">
                {file.originalName}
              </p>
              <FileAiStatusBadge status={toAiProcessingStatus(file.aiProcessingStatus)} />
            </div>
          </div>

          <div className="rounded-xl border border-border/70 bg-muted/35 px-4 py-3 text-sm leading-7 text-muted-foreground">
            {file.aiSummary?.trim() || "AI 요약이 아직 생성되지 않았습니다."}
          </div>

          <div className="flex flex-wrap items-center gap-3 text-sm">
            <span className="text-muted-foreground">소속 업무일지</span>
            {worklog ? (
              <Link
                href={`/worklog/detail/${worklog.worklogId}`}
                className="font-medium text-primary underline-offset-4 hover:underline"
                onClick={(event) => event.stopPropagation()}
              >
                {worklog.title}
              </Link>
            ) : (
              <span className="text-muted-foreground">연결 없음</span>
            )}
          </div>
        </div>

        <div className="grid shrink-0 gap-3 text-sm text-muted-foreground sm:grid-cols-3 lg:w-64 lg:grid-cols-1 lg:text-right">
          <FileMeta label="용량" value={formatFileSize(file.fileSizeBytes)} />
          <FileMeta label="업로드일" value={formatDateTime(file.createdAt)} />
          <FileMeta label="업무 작성자" value={worklog?.authorName ?? "-"} />
        </div>
      </CardContent>
    </CardSpotlight>
  );
}

function FileMeta({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      <p className="mt-1 truncate font-medium text-foreground">{value}</p>
    </div>
  );
}

function toAiProcessingStatus(status: string | null | undefined): AiProcessingStatus {
  if (status === "COMPLETED" || status === "DONE") {
    return "DONE";
  }

  if (status === "PROCESSING" || status === "FAILED") {
    return status;
  }

  return "PENDING";
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
  }).format(new Date(value));
}

function formatFileSize(bytes: number | null | undefined) {
  if (!bytes || bytes <= 0) {
    return "0 B";
  }

  const units = ["B", "KB", "MB", "GB"];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** index;

  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

export default FileCard;
