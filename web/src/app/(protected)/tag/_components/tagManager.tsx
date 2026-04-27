"use client"

import { useMemo, useState } from "react"
import { ArrowLeftRight, RefreshCw, Search } from "lucide-react"
import { Pagination } from "@/app/_common/components/data-display/pagination"
import { usePagination } from "@/app/_common/hooks/usePagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import { useTagList } from "../_hooks"
import { getTagSourceBadgeClass } from "../_utils/tagBadge"
import { TagStateBadge } from "./tagStateBadge"

const sourceLabelMap = {
  AI: "AI 생성",
  MANUAL: "수동 생성",
} as const

export function TagManager() {
  const { tags } = useTagList()
  const [query, setQuery] = useState("")
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([])
  const normalizedQuery = query.trim().toLowerCase()

  const filteredTags = useMemo(() => {
    return tags.filter((tag) => {
      if (!normalizedQuery) return true

      const searchableText = [
        tag.name,
        tag.category,
        tag.reuseHint,
        sourceLabelMap[tag.source],
        tag.mergeState,
      ]
        .join(" ")
        .toLowerCase()

      return searchableText.includes(normalizedQuery)
    })
  }, [normalizedQuery, tags])

  const pagination = usePagination(filteredTags, 6)
  const pageTagIds = pagination.items.map((tag) => tag.id)
  const activeCount = tags.filter((tag) => tag.mergeState === "ACTIVE").length
  const mergeCandidateCount = tags.filter(
    (tag) => tag.mergeState === "MERGE_CANDIDATE",
  ).length
  const reviewCount = tags.filter((tag) => tag.mergeState === "REVIEW").length
  const isAllCurrentPageSelected =
    pageTagIds.length > 0 && pageTagIds.every((id) => selectedTagIds.includes(id))

  const handleToggleSelectAllCurrentPage = (checked: boolean) => {
    setSelectedTagIds((current) => {
      if (checked) {
        return Array.from(new Set([...current, ...pageTagIds]))
      }

      return current.filter((id) => !pageTagIds.includes(id))
    })
  }

  const handleSelectCard = (tagId: number) => {
    setSelectedTagIds((current) =>
      current.includes(tagId)
        ? current.filter((id) => id !== tagId)
        : Array.from(new Set([...current, tagId])),
    )
  }

  return (
    <div className="space-y-6">
      <div className="grid gap-4 lg:grid-cols-3">
        <SummaryCard label="Total Tags" value={tags.length} />
        <SummaryCard label="Merge Candidates" value={mergeCandidateCount} />
        <SummaryCard label="Review Queue" value={reviewCount} />
      </div>

      <div className="space-y-4">
        <div className="relative">
          <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="h-12 rounded-2xl pl-11 pr-12 text-sm"
            placeholder="태그 이름, 분류, 상태, 힌트로 검색하세요"
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="absolute right-3 top-1/2 size-8 -translate-y-1/2 rounded-xl text-muted-foreground"
            aria-label="검색어 초기화"
            onClick={() => setQuery("")}
          >
            <RefreshCw className="size-4" />
          </Button>
        </div>

        <div className="h-0.5 bg-foreground/80 dark:bg-foreground/70" />

        <div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <span className="font-medium">표시 중인 태그</span>
            <span className="text-lg font-semibold text-foreground">
              {filteredTags.length}개
            </span>
          </div>
          <label className="inline-flex items-center gap-2">
            <Checkbox
              checked={isAllCurrentPageSelected}
              onChange={(event) =>
                handleToggleSelectAllCurrentPage(event.target.checked)
              }
            />
            <span>현재 페이지 전체 선택</span>
          </label>
          <span className="text-xs">운영중 {activeCount}개</span>

          <Button
            type="button"
            className="ml-auto h-11 min-w-36 rounded-xl px-5 text-sm font-semibold"
          >
            <ArrowLeftRight className="size-4" />
            선택 태그 병합
          </Button>
        </div>
      </div>

      {filteredTags.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
          조건에 맞는 태그가 없습니다.
        </div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {pagination.items.map((tag) => (
            <CardSpotlight
              key={tag.id}
              className={cn(
                "rounded-[24px] border-border/75 transition-all duration-300 hover:-translate-y-1",
                selectedTagIds.includes(tag.id) &&
                  "ring-2 ring-primary/50 ring-offset-2 ring-offset-background",
              )}
            >
              <button
                type="button"
                className="block w-full text-left"
                onClick={() => handleSelectCard(tag.id)}
              >
                <div className="space-y-4 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 space-y-1">
                      <p className="truncate font-medium text-foreground">#{tag.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {tag.category} / {tag.usageCount}회 사용
                      </p>
                    </div>
                    <TagStateBadge state={tag.mergeState} />
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <Badge
                      variant="outline"
                      className={cn(
                        "rounded-full border px-2.5 py-0.5 font-medium",
                        getTagSourceBadgeClass(tag.source),
                      )}
                    >
                      {sourceLabelMap[tag.source]}
                    </Badge>
                    {tag.mergeTargetId ? (
                      <Badge
                        variant="outline"
                        className="rounded-full border-border bg-muted text-muted-foreground"
                      >
                        병합 대상 #{tag.mergeTargetId}
                      </Badge>
                    ) : null}
                  </div>

                  <div className="min-h-20 rounded-2xl border border-dashed border-border/60 bg-muted/25 p-3 text-sm leading-6 text-muted-foreground">
                    {tag.reuseHint}
                  </div>
                </div>
              </button>
            </CardSpotlight>
          ))}
        </div>
      )}

      <Pagination
        page={pagination.page}
        totalPages={pagination.totalPages}
        onPageChange={pagination.setPage}
      />
    </div>
  )
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <CardSpotlight className="rounded-[24px] p-4">
      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
        {label}
      </p>
      <p className="mt-2 text-2xl font-semibold text-foreground">{value}</p>
    </CardSpotlight>
  )
}
