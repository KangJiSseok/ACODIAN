"use client"

import {
  useEffect,
  useRef,
  useState,
  type FocusEvent,
  type KeyboardEvent,
} from "react"
import { Search, X } from "lucide-react"
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

const tagScrollbarClassName =
  "[scrollbar-color:theme(colors.slate.300)_transparent] [scrollbar-width:thin] [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-slate-300/80 [&::-webkit-scrollbar-track]:bg-transparent dark:[scrollbar-color:theme(colors.slate.600)_transparent] dark:[&::-webkit-scrollbar-thumb]:bg-slate-600/80"

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
  const [highlightedCandidateIndex, setHighlightedCandidateIndex] = useState(0)
  const candidateRefs = useRef<Record<number, HTMLButtonElement | null>>({})
  const activeCandidateIndex =
    filteredTagCandidates.length > 0
      ? Math.min(highlightedCandidateIndex, filteredTagCandidates.length - 1)
      : -1

  useEffect(() => {
    const highlightedCandidate = filteredTagCandidates[activeCandidateIndex]

    if (!highlightedCandidate) {
      return
    }

    candidateRefs.current[highlightedCandidate.id]?.scrollIntoView({
      block: "nearest",
    })
  }, [activeCandidateIndex, filteredTagCandidates])

  const handleTagSearchBlur = (event: FocusEvent<HTMLDivElement>) => {
    const nextTarget = event.relatedTarget

    if (!(nextTarget instanceof Node) || !event.currentTarget.contains(nextTarget)) {
      onTagSearchOpenChange(false)
    }
  }

  const handleTagSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (!tagSearchOpen || !tagKeywordInput.trim() || filteredTagCandidates.length === 0) {
      return
    }

    if (event.key === "ArrowDown") {
      event.preventDefault()
      setHighlightedCandidateIndex((currentIndex) =>
        currentIndex >= filteredTagCandidates.length - 1 ? 0 : currentIndex + 1,
      )
      return
    }

    if (event.key === "ArrowUp") {
      event.preventDefault()
      setHighlightedCandidateIndex((currentIndex) =>
        currentIndex <= 0 ? filteredTagCandidates.length - 1 : currentIndex - 1,
      )
      return
    }

    if (event.key === "Enter") {
      event.preventDefault()
      const highlightedCandidate = filteredTagCandidates[activeCandidateIndex]

      if (highlightedCandidate) {
        onAddTag(highlightedCandidate.id)
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="relative grid max-h-[86vh] min-h-[520px] w-[min(560px,calc(100vw-2rem))] max-w-none grid-rows-[auto_minmax(0,1fr)_auto] overflow-hidden rounded-[28px] p-7">
        <DialogHeader>
          <DialogTitle>태그 등록</DialogTitle>
          <DialogDescription>
            업무에 직접 연결할 태그를 검색해서 선택합니다.
          </DialogDescription>
        </DialogHeader>
        <div
          className={cn(
            "dashboard-scrollbar mt-3 min-h-0 space-y-5 overflow-y-auto px-1 py-1 [scrollbar-gutter:stable]",
            tagScrollbarClassName,
          )}
        >
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
                  setHighlightedCandidateIndex(0)
                }}
                onKeyDown={handleTagSearchKeyDown}
                placeholder="태그명으로 검색"
                role="combobox"
                aria-expanded={tagSearchOpen && Boolean(tagKeywordInput.trim())}
                aria-controls="worklog-tag-candidates"
                aria-activedescendant={
                  tagSearchOpen && filteredTagCandidates[activeCandidateIndex]
                    ? `worklog-tag-candidate-${filteredTagCandidates[activeCandidateIndex].id}`
                    : undefined
                }
              />
            </div>

            {tagSearchOpen && tagKeywordInput.trim() ? (
              <div className="overflow-hidden rounded-2xl border border-border bg-popover p-2">
                <div
                  id="worklog-tag-candidates"
                  role="listbox"
                  className={cn(
                    "dashboard-scrollbar max-h-[260px] overflow-y-auto [scrollbar-gutter:stable]",
                    tagScrollbarClassName,
                  )}
                >
                  {filteredTagCandidates.length === 0 ? (
                    <p className="px-3 py-3 text-sm text-muted-foreground">
                      조건에 맞는 태그가 없습니다.
                    </p>
                  ) : (
                    filteredTagCandidates.map((tag, index) => (
                      <button
                        key={tag.id}
                        id={`worklog-tag-candidate-${tag.id}`}
                        ref={(node) => {
                          candidateRefs.current[tag.id] = node
                        }}
                        type="button"
                        role="option"
                        aria-selected={index === activeCandidateIndex}
                        className={cn(
                          "block w-full rounded-xl px-3 py-2.5 text-left",
                          index === activeCandidateIndex && "bg-muted",
                        )}
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

          <div className="min-h-[126px]">
            <div className="w-full overflow-hidden rounded-2xl border border-border/70 bg-muted/20 p-3">
              {selectedTags.length === 0 ? (
                <p className="flex min-h-[72px] items-center text-sm text-muted-foreground">
                  선택한 태그가 없습니다. 저장 시 AI가 관련 태그를 자동으로 추가합니다.
                </p>
              ) : (
                <div
                  className={cn(
                    "dashboard-scrollbar max-h-[196px] overflow-x-hidden overflow-y-auto pr-2",
                    tagScrollbarClassName,
                  )}
                >
                  <div className="flex flex-wrap gap-2">
                    {selectedTags.map((tag) => (
                      <span
                        key={tag.id}
                        className={cn(
                          "inline-flex max-w-full items-center gap-2 rounded-full border px-3 py-2 text-sm font-semibold",
                          tag.source === "AI"
                            ? "border-primary/25 bg-primary/10 text-primary"
                            : "border-border bg-background text-foreground",
                        )}
                      >
                        <span className="max-w-[14rem] truncate">#{tag.name}</span>
                        <span className="text-xs font-medium text-muted-foreground">
                          {tag.usageCount}회
                        </span>
                        <button
                          type="button"
                          className="-mr-1 flex size-6 shrink-0 items-center justify-center rounded-full text-muted-foreground"
                          aria-label={`${tag.name} 태그 제거`}
                          onClick={() => onRemoveTag(tag.id)}
                        >
                          <X className="size-3.5" />
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
        <DialogFooter className="mt-5 border-t border-border/70 pt-4">
          <Button
            type="button"
            variant="default"
            size="lg"
            className="min-w-24 px-5 font-semibold"
            onClick={() => onOpenChange(false)}
          >
            태그 반영
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
