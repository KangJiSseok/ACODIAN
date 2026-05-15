"use client"

import type { FocusEvent } from "react"
import { Search, X } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import type { WorklogFormTagOption } from "../_types/worklog.types"
import { getTagSourceBadgeClass } from "../_utils/tagBadge"

interface WorklogTagModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  searchControlClassName: string
  tagKeywordInput: string
  onTagKeywordInputChange: (value: string) => void
  tagSearchOpen: boolean
  onTagSearchOpenChange: (open: boolean) => void
  filteredTagCandidates: WorklogFormTagOption[]
  selectedTags: WorklogFormTagOption[]
  hasMoreTagCandidates?: boolean
  isFetchingMoreTagCandidates?: boolean
  onLoadMoreTagCandidates?: () => void
  onAddTag: (tagId: number) => void
  onRemoveTag: (tagId: number) => void
}

export function WorklogTagModal({
  open,
  onOpenChange,
  searchControlClassName,
  tagKeywordInput,
  onTagKeywordInputChange,
  tagSearchOpen,
  onTagSearchOpenChange,
  filteredTagCandidates,
  selectedTags,
  hasMoreTagCandidates,
  isFetchingMoreTagCandidates,
  onLoadMoreTagCandidates,
  onAddTag,
  onRemoveTag,
}: WorklogTagModalProps) {
  const handleTagSearchBlur = (event: FocusEvent<HTMLDivElement>) => {
    const nextTarget = event.relatedTarget

    if (!(nextTarget instanceof Node) || !event.currentTarget.contains(nextTarget)) {
      onTagSearchOpenChange(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="relative max-h-[86vh] max-w-3xl overflow-y-auto rounded-[28px] p-7">
        <DialogHeader>
          <DialogTitle>태그 등록</DialogTitle>
          <DialogDescription>
            업무에 직접 연결할 태그를 검색해서 선택합니다.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-5">
          <div className="space-y-2" onBlur={handleTagSearchBlur}>
            <div className="relative">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                className={searchControlClassName}
                value={tagKeywordInput}
                onFocus={() => onTagSearchOpenChange(true)}
                onChange={(event) => {
                  onTagKeywordInputChange(event.target.value)
                  onTagSearchOpenChange(true)
                }}
                placeholder="태그명으로 검색"
              />
            </div>

            {tagSearchOpen && tagKeywordInput.trim() ? (
              <div className="overflow-hidden rounded-2xl border border-border bg-popover p-2 shadow-[0_18px_48px_-28px_rgba(15,23,42,0.65)]">
                <div className="dashboard-scrollbar max-h-[260px] overflow-y-auto [scrollbar-gutter:stable]">
                  {filteredTagCandidates.length === 0 ? (
                    <p className="px-3 py-3 text-sm text-muted-foreground">
                      조건에 맞는 태그가 없습니다.
                    </p>
                  ) : (
                    filteredTagCandidates.map((tag) => (
                      <button
                        key={tag.id}
                        type="button"
                        className="block w-full rounded-xl px-3 py-2.5 text-left transition-colors hover:bg-muted"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => onAddTag(tag.id)}
                      >
                        <span className="block text-sm font-semibold text-popover-foreground">
                          #{tag.name}
                        </span>
                        <span className="mt-1 block text-xs text-muted-foreground">
                          {tag.category} / {tag.usageCount}회 사용
                        </span>
                      </button>
                    ))
                  )}
                </div>
                {hasMoreTagCandidates ? (
                  <div className="border-t border-border/70 px-2 pt-2">
                    <Button
                      type="button"
                      variant="ghost"
                      className="h-9 w-full rounded-xl text-sm"
                      disabled={isFetchingMoreTagCandidates}
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={onLoadMoreTagCandidates}
                    >
                      {isFetchingMoreTagCandidates ? "불러오는 중..." : "더보기"}
                    </Button>
                  </div>
                ) : null}
              </div>
            ) : null}
          </div>

          <div className="space-y-2">
            {selectedTags.length === 0 ? (
              <p className="rounded-2xl border border-dashed border-border/70 px-4 py-3 text-sm text-muted-foreground">
                선택한 태그가 없습니다. 저장 시 AI가 관련 태그를 자동으로 추가합니다.
              </p>
            ) : (
              selectedTags.map((tag) => (
                <div
                  key={tag.id}
                  className="flex items-start justify-between gap-3 rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm"
                >
                  <div className="min-w-0">
                    <Badge
                      variant="outline"
                      className={cn(
                        "rounded-full px-3 py-1",
                        getTagSourceBadgeClass(tag.source),
                      )}
                    >
                      #{tag.name}
                    </Badge>
                    <p className="mt-2 text-xs text-muted-foreground">
                      {tag.category} / {tag.usageCount}회 사용
                    </p>
                  </div>
                  <button
                    type="button"
                    className="flex size-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                    aria-label={`${tag.name} 태그 제거`}
                    onClick={() => onRemoveTag(tag.id)}
                  >
                    <X className="size-4" />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
        <DialogFooter className="border-t border-border/70 pt-4">
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            닫기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
