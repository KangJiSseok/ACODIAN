"use client";

import { useState } from "react";
import {
  CircleHelp,
  ChevronDown,
  Download,
  RefreshCw,
  Search,
  SlidersHorizontal,
} from "lucide-react";
import { files } from "@/app/(protected)/worklog/_mock/worklog.mock";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { FileCard } from "./_components/fileCard";

export default function FilePage() {
  const [selected, setSelected] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const sampleFile = files.find((file) => !file.isDeleted);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="파일 관리" description="업무일지에 연결된 파일을 확인합니다." />

      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            파일 탐색
          </h2>
        </div>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="h-12 pl-11"
                placeholder="파일명, AI 요약 키워드로 검색하세요"
                aria-label="파일 검색"
              />
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                className="h-10"
                type="button"
                onClick={() => setShowFilters((prev) => !prev)}
              >
                <SlidersHorizontal className="size-4" />
                필터
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
              <div className="space-y-4 pt-3">
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
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                </div>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      형식
                    </p>
                    <Select
                      aria-label="파일 형식 필터"
                      defaultValue="ALL"
                      options={[
                        { label: "전체 형식", value: "ALL" },
                        { label: "PDF", value: "PDF" },
                        { label: "HWP", value: "HWP" },
                        { label: "DOCX", value: "DOCX" },
                      ]}
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      기간
                    </p>
                    <Select
                      aria-label="업로드 기간 필터"
                      defaultValue="ALL"
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
                      defaultValue="ALL"
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
                {sampleFile ? "1건" : "0건"}
              </span>
              <label className="ml-0 inline-flex items-center gap-2 text-sm text-muted-foreground lg:ml-3">
                <Checkbox
                  checked={selected}
                  onChange={(event) => setSelected(event.target.checked)}
                />
                <span>현재 페이지 전체 선택</span>
              </label>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                type="button"
                aria-label="AI 상태 도움말"
              >
                <CircleHelp className="size-4" />
              </Button>
            </div>

            <div className="flex items-center gap-2 lg:ml-auto">
              <Button
                variant="secondary"
                className="h-11 min-w-28 px-5 text-sm font-semibold"
                type="button"
                disabled={!selected}
              >
                <Download className="size-4" />
                다운로드
              </Button>
            </div>
          </div>
        </div>

        {sampleFile ? (
          <FileCard
            file={sampleFile}
            selected={selected}
            onToggleSelect={(_, checked) => setSelected(checked)}
          />
        ) : (
          <p className="text-sm leading-6 text-muted-foreground">
            표시할 샘플 파일이 없습니다.
          </p>
        )}
      </div>
    </div>
  );
}
