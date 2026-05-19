"use client";

import { useMemo, useState } from "react";
import type { AiProcessingStatus } from "@/app/(protected)/worklog/_types/worklog.types";
import { Pagination } from "@/app/_common/components/data-display/pagination";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import {
  CircleHelp,
  ChevronDown,
  Download,
  RefreshCw,
  Search,
  SlidersHorizontal,
} from "lucide-react";
import { FileCard } from "./_components/fileCard";
import { useFileList, useFileTypes } from "./_hooks";
import type { FileFiltersValue, FileItem, GetFilesParams } from "./_types/file.types";

const ALL_FILTER_VALUE = "ALL";
const FILE_PAGE_SIZE = 10;

type DownloadResultMessage = {
  tone: "success" | "error";
  text: string;
};

export default function FilePage() {
  const [selectedFileIds, setSelectedFileIds] = useState<Set<number>>(new Set());
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadResultMessage, setDownloadResultMessage] =
    useState<DownloadResultMessage | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [query, setQuery] = useState("");
  const [fileType, setFileType] = useState(ALL_FILTER_VALUE);
  const [period, setPeriod] = useState<FileFiltersValue["period"]>("ALL");
  const [aiStatus, setAiStatus] = useState<FileFiltersValue["aiStatus"]>("ALL");
  const [page, setPage] = useState(1);

  const fileListParams = useMemo<GetFilesParams>(
    () => ({
      page,
      pageSize: FILE_PAGE_SIZE,
      fileType: fileType === ALL_FILTER_VALUE ? undefined : fileType,
      period: toPeriodDays(period),
    }),
    [fileType, page, period],
  );

  const {
    data: filePage,
    isLoading,
    error,
    refetch,
  } = useFileList(fileListParams);
  const { data: fileTypes = [] } = useFileTypes();
  const fileTypeOptions = useMemo(
    () => [
      { label: "전체 형식", value: ALL_FILTER_VALUE },
      ...fileTypes.map((type) => ({
        label: `${type.fileType} (.${type.extension})`,
        value: type.fileType,
      })),
    ],
    [fileTypes],
  );

  const visibleFiles = useMemo(
    () =>
      filterFiles(filePage?.items ?? [], {
        query,
        fileType,
        period,
        aiStatus,
      }),
    [aiStatus, filePage?.items, fileType, period, query],
  );
  const allVisibleFilesSelected =
    visibleFiles.length > 0 && visibleFiles.every((file) => selectedFileIds.has(file.id));
  const selectedFiles = useMemo(
    () => visibleFiles.filter((file) => selectedFileIds.has(file.id)),
    [selectedFileIds, visibleFiles],
  );
  const selectedCount = selectedFiles.length;
  const activeFilterCount = [fileType, period, aiStatus].filter(
    (value) => value !== ALL_FILTER_VALUE,
  ).length;

  function resetFilters() {
    setQuery("");
    setFileType(ALL_FILTER_VALUE);
    setPeriod("ALL");
    setAiStatus("ALL");
    setPage(1);
  }

  function updateQuery(value: string) {
    setQuery(value);
    setPage(1);
  }

  function updateFileType(value: string) {
    setFileType(value);
    setPage(1);
  }

  function updatePeriod(value: string) {
    setPeriod(value as FileFiltersValue["period"]);
    setPage(1);
  }

  function updateAiStatus(value: string) {
    setAiStatus(value as FileFiltersValue["aiStatus"]);
    setPage(1);
  }

  function toggleFileSelection(fileId: number, checked: boolean) {
    setSelectedFileIds((current) => {
      const next = new Set(current);

      if (checked) {
        next.add(fileId);
      } else {
        next.delete(fileId);
      }

      return next;
    });
  }

  function toggleVisibleFiles(checked: boolean) {
    setSelectedFileIds((current) => {
      const next = new Set(current);

      visibleFiles.forEach((file) => {
        if (checked) {
          next.add(file.id);
        } else {
          next.delete(file.id);
        }
      });

      return next;
    });
  }

  async function handleDownloadSelectedFiles() {
    if (selectedFiles.length === 0 || isDownloading) {
      return;
    }

    const filesToDownload = selectedFiles;
    const failedFiles: FileItem[] = [];
    let successCount = 0;

    setDownloadResultMessage(null);
    setIsDownloading(true);

    for (const file of filesToDownload) {
      try {
        await downloadFile(file);
        successCount += 1;
      } catch {
        failedFiles.push(file);
      }
    }

    setDownloadResultMessage(createDownloadResultMessage(successCount, failedFiles));
    setIsDownloading(false);
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="파일 관리" />

      <div className="space-y-4">
        <div>
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            파일 탐색
          </h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            업무일지에 연결된 파일을 확인합니다.
          </p>
        </div>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={query}
                onChange={(event) => updateQuery(event.target.value)}
                className="h-12 rounded-2xl pl-11 pr-4 transition-all duration-500"
                placeholder="파일명, AI 요약, 업무명으로 검색하세요"
                aria-label="파일 검색"
              />
            </div>
            <div className="flex w-full items-center gap-2 sm:w-auto">
              <Button
                variant="secondary"
                className="h-12 justify-center"
                type="button"
                onClick={() => setShowFilters((prev) => !prev)}
              >
                <SlidersHorizontal className="size-4" />
                필터
                {activeFilterCount > 0 ? (
                  <span className="ml-1 rounded-full bg-primary px-2 py-0.5 text-[11px] font-semibold text-primary-foreground">
                    {activeFilterCount}
                  </span>
                ) : null}
                <ChevronDown
                  className={cn(
                    "ml-1 size-4 transition-transform duration-300 ease-out",
                    showFilters && "rotate-180",
                  )}
                />
              </Button>
            </div>
          </div>

          <div
            className={cn(
              "grid overflow-hidden transition-[grid-template-rows,opacity,margin] duration-300 ease-out",
              showFilters
                ? "mt-0 grid-rows-[1fr] opacity-100"
                : "mt-[-4px] grid-rows-[0fr] opacity-0",
            )}
          >
            <div className="overflow-hidden">
              <div className="space-y-4 pb-4 pt-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                    File Filters
                  </p>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-9 w-9"
                    type="button"
                    aria-label="필터 초기화"
                    onClick={resetFilters}
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                </div>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      형식
                    </p>
                    <Select
                      aria-label="파일 형식 필터"
                      value={fileType}
                      onChange={(event) => updateFileType(event.target.value)}
                      options={fileTypeOptions}
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      기간
                    </p>
                    <Select
                      aria-label="업로드 기간 필터"
                      value={period}
                      onChange={(event) => updatePeriod(event.target.value)}
                      options={[
                        { label: "전체 기간", value: "ALL" },
                        { label: "최근 7일", value: "7D" },
                        { label: "최근 30일", value: "30D" },
                        { label: "최근 90일", value: "90D" },
                      ]}
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      AI 상태
                    </p>
                    <Select
                      aria-label="AI 상태 필터"
                      value={aiStatus}
                      onChange={(event) => updateAiStatus(event.target.value)}
                      options={[
                        { label: "전체 AI 상태", value: "ALL" },
                        { label: "대기", value: "PENDING" },
                        { label: "처리 중", value: "PROCESSING" },
                        { label: "완료", value: "DONE" },
                        { label: "실패", value: "FAILED" },
                      ]}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-2 space-y-4">
        <div className="border-t-2 border-foreground/70 pt-5">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
            <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
              <span className="font-medium">표시 중인 파일</span>
              <span className="text-lg font-semibold text-foreground">
                {visibleFiles.length}건 / 전체 {filePage?.totalCount ?? 0}건
              </span>
              <label className="ml-0 inline-flex items-center gap-2 text-sm text-muted-foreground lg:ml-3">
                <Checkbox
                  checked={allVisibleFilesSelected}
                  onChange={(event) => toggleVisibleFiles(event.target.checked)}
                  disabled={visibleFiles.length === 0}
                />
                <span>현재 페이지 전체 선택</span>
              </label>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                type="button"
                aria-label="AI 상태 안내"
              >
                <CircleHelp className="size-4" />
              </Button>
            </div>

            <div className="flex items-center gap-2 lg:ml-auto">
              <Button
                variant="secondary"
                className="h-11 min-w-28 px-5 text-sm font-semibold"
                type="button"
                onClick={() => void handleDownloadSelectedFiles()}
                disabled={selectedCount === 0 || isDownloading}
              >
                <Download className="size-4" />
                {isDownloading ? "다운로드 중" : "다운로드"}
              </Button>
            </div>
          </div>

          {downloadResultMessage ? (
            <p
              className={cn(
                "mt-3 text-right text-sm font-medium",
                downloadResultMessage.tone === "error"
                  ? "text-destructive"
                  : "text-muted-foreground",
              )}
            >
              {downloadResultMessage.text}
            </p>
          ) : null}
        </div>

        {isLoading ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            파일 목록을 불러오는 중입니다.
          </div>
        ) : null}

        {error ? (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
            파일 목록을 불러오지 못했습니다.
            <button
              type="button"
              className="ml-3 font-semibold underline underline-offset-4"
              onClick={() => void refetch()}
            >
              다시 시도
            </button>
          </div>
        ) : null}

        {!isLoading && !error && visibleFiles.length === 0 ? (
          <p className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            표시할 파일이 없습니다.
          </p>
        ) : null}

        <div className="grid gap-4">
          {visibleFiles.map((file) => (
            <FileCard
              key={file.id}
              file={file}
              selected={selectedFileIds.has(file.id)}
              onToggleSelect={toggleFileSelection}
            />
          ))}
        </div>

        <Pagination
          page={filePage?.page ?? page}
          totalPages={filePage?.totalPages ?? 1}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}

async function downloadFile(file: FileItem) {
  if (!file.storedPath) {
    throw new Error("파일 다운로드 URL이 없습니다.");
  }

  const response = await fetch(file.storedPath);

  if (!response.ok) {
    throw new Error("파일 다운로드 요청에 실패했습니다.");
  }

  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = objectUrl;
  link.download = file.originalName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
}

function createDownloadResultMessage(
  successCount: number,
  failedFiles: FileItem[],
): DownloadResultMessage {
  const failedNames = failedFiles.map((file) => file.originalName);

  if (failedFiles.length === 0) {
    return {
      tone: "success",
      text: `${successCount}개 파일 다운로드를 시작했습니다.`,
    };
  }

  if (successCount === 0) {
    return {
      tone: "error",
      text: `다운로드에 실패했습니다: ${failedNames.join(", ")}`,
    };
  }

  return {
    tone: "error",
    text: `${successCount}개 파일 다운로드를 시작했고, ${failedFiles.length}개 파일은 실패했습니다: ${failedNames.join(", ")}`,
  };
}

function filterFiles(
  files: FileItem[],
  filters: {
    query: string;
    fileType: string;
    period: FileFiltersValue["period"];
    aiStatus: FileFiltersValue["aiStatus"];
  },
) {
  const keyword = filters.query.trim().toLowerCase();

  return files.filter((file) => {
    if (keyword && !matchesKeyword(file, keyword)) {
      return false;
    }

    if (
      filters.fileType !== ALL_FILTER_VALUE &&
      file.fileExtension.toUpperCase() !== filters.fileType
    ) {
      return false;
    }

    if (
      filters.aiStatus !== "ALL" &&
      toAiProcessingStatus(file.aiProcessingStatus) !== filters.aiStatus
    ) {
      return false;
    }

    return matchesPeriod(file.createdAt, filters.period);
  });
}

function matchesKeyword(file: FileItem, keyword: string) {
  return [
    file.originalName,
    file.aiSummary,
    file.worklog?.title,
    file.worklog?.teamName,
    file.worklog?.authorName,
  ]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword));
}

function matchesPeriod(value: string, period: FileFiltersValue["period"]) {
  if (period === "ALL") {
    return true;
  }

  const days = Number(period.replace("D", ""));
  const uploadedAt = new Date(value).getTime();

  if (!Number.isFinite(uploadedAt)) {
    return false;
  }

  return Date.now() - uploadedAt <= days * 24 * 60 * 60 * 1000;
}

function toPeriodDays(period: FileFiltersValue["period"]) {
  if (period === "ALL") {
    return undefined;
  }

  const days = Number(period.replace("D", ""));

  return Number.isFinite(days) ? days : undefined;
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
