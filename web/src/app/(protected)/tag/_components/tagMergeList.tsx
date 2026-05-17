"use client"

import { useMemo, useState } from "react"
import {
  ArrowRight,
  GitMerge,
  Pencil,
  RefreshCw,
  Search,
  X,
} from "lucide-react"
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
import { useTagList, useTagMergeCandidates } from "../_hooks"

export function TagMergeList() {
  const {
    data: candidates = [],
    isError,
    isLoading,
    refetch,
  } = useTagMergeCandidates()
  const [editingCandidateId, setEditingCandidateId] = useState<string | null>(
    null
  )
  const [draftSourceTagNames, setDraftSourceTagNames] = useState<string[]>([])
  const [tagSearchQuery, setTagSearchQuery] = useState("")
  const [editedSourceTagNamesById, setEditedSourceTagNamesById] = useState<
    Record<string, string[]>
  >({})
  const { data: tagPage, isLoading: isTagSearchLoading } = useTagList({
    query: tagSearchQuery,
    page: 1,
    pageSize: 8,
  })
  const tagSearchResults = tagPage?.items ?? []
  const editingCandidate = candidates.find(
    (candidate) => candidate.id === editingCandidateId
  )
  const normalizedDraftSourceTagNames = useMemo(
    () => new Set(draftSourceTagNames.map((tagName) => tagName.trim())),
    [draftSourceTagNames]
  )

  function getSourceTagNames(candidateId: string, fallback: string[]) {
    return editedSourceTagNamesById[candidateId] ?? fallback
  }

  function openEditDialog(candidateId: string, sourceTagNames: string[]) {
    setEditingCandidateId(candidateId)
    setDraftSourceTagNames(sourceTagNames)
    setTagSearchQuery("")
  }

  function closeEditDialog() {
    setEditingCandidateId(null)
    setDraftSourceTagNames([])
    setTagSearchQuery("")
  }

  function onAddTag(tagName: string) {
    const normalizedTagName = tagName.trim()

    if (!normalizedTagName || normalizedDraftSourceTagNames.has(normalizedTagName)) {
      return
    }

    setDraftSourceTagNames((current) => [...current, normalizedTagName])
    setTagSearchQuery("")
  }

  function onRemoveTag(tagName: string) {
    setDraftSourceTagNames((current) =>
      current.filter((currentTagName) => currentTagName !== tagName)
    )
  }

  function applyDraftSourceTags() {
    if (!editingCandidateId) {
      return
    }

    setEditedSourceTagNamesById((current) => ({
      ...current,
      [editingCandidateId]: draftSourceTagNames,
    }))
    closeEditDialog()
  }

  if (isLoading) {
    return (
      <div className="rounded-3xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
        태그 병합 후보를 불러오는 중입니다.
      </div>
    )
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center gap-4 rounded-3xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
        <p>태그 병합 후보를 불러오지 못했습니다.</p>
        <Button
          type="button"
          variant="outline"
          className="h-10 px-4"
          onClick={() => void refetch()}
        >
          <RefreshCw className="size-4" />
          다시 시도
        </Button>
      </div>
    )
  }

  if (candidates.length === 0) {
    return (
      <div className="rounded-3xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
        표시할 태그 병합 후보가 없습니다.
      </div>
    )
  }

  return (
    <>
      <div className="space-y-3">
        {candidates.map((candidate) => {
          const sourceTagNames = getSourceTagNames(
            candidate.id,
            candidate.sourceTagNames
          )

          return (
            <div
              key={candidate.id}
              className="rounded-2xl border border-border/75 bg-card px-5 py-4 shadow-[var(--shadow-panel)]"
            >
              <div className="grid gap-5 xl:grid-cols-[minmax(12rem,0.8fr)_2rem_minmax(0,1.4fr)_auto] xl:items-center">
                <TagChipGroup
                  label="병합될 태그명"
                  tags={[candidate.targetTagName]}
                  tone="target"
                />

                <div className="hidden justify-center text-muted-foreground lg:flex">
                  <ArrowRight className="size-5" />
                </div>

                <TagChipGroup
                  label="합쳐질 태그들"
                  tags={sourceTagNames}
                  tone="source"
                />

                <div className="flex flex-wrap gap-2 xl:justify-end">
                  <Button
                    type="button"
                    variant="secondary"
                    className="h-11 px-5 text-sm font-semibold sm:min-w-24"
                    onClick={() => openEditDialog(candidate.id, sourceTagNames)}
                  >
                    <Pencil className="size-4" />
                    수정
                  </Button>
                  <Button
                    type="button"
                    variant="default"
                    className="h-11 px-5 text-sm font-semibold sm:min-w-24"
                  >
                    <GitMerge className="size-4" />
                    병합
                  </Button>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      <Dialog
        open={Boolean(editingCandidate)}
        onOpenChange={(open) => {
          if (!open) {
            closeEditDialog()
          }
        }}
      >
        <DialogContent className="grid max-h-[84vh] w-[min(560px,calc(100vw-2rem))] max-w-none grid-rows-[auto_minmax(0,1fr)_auto] overflow-hidden rounded-2xl p-0">
          <DialogHeader className="px-6 pb-4 pt-6">
            <DialogTitle>합쳐질 태그 수정</DialogTitle>
            <DialogDescription>
              {editingCandidate?.targetTagName
                ? `#${editingCandidate.targetTagName}으로 합쳐질 태그를 조정합니다.`
                : "합쳐질 태그를 조정합니다."}
            </DialogDescription>
          </DialogHeader>

          <div className="min-h-0 space-y-5 overflow-y-auto px-6 py-1">
            <section className="space-y-2">
              <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">
                태그 검색
              </p>
              <div className="relative">
                <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={tagSearchQuery}
                  onChange={(event) => setTagSearchQuery(event.target.value)}
                  className="h-11 rounded-xl pl-11 text-sm"
                  placeholder="태그명으로 검색"
                />
              </div>

              {tagSearchQuery.trim() ? (
                <div className="max-h-52 overflow-y-auto rounded-2xl border border-border bg-popover p-2">
                  {isTagSearchLoading ? (
                    <p className="px-3 py-3 text-sm text-muted-foreground">
                      태그를 검색하는 중입니다.
                    </p>
                  ) : tagSearchResults.length === 0 ? (
                    <p className="px-3 py-3 text-sm text-muted-foreground">
                      조건에 맞는 태그가 없습니다.
                    </p>
                  ) : (
                    tagSearchResults.map((tag) => {
                      const isSelected = normalizedDraftSourceTagNames.has(
                        tag.name
                      )

                      return (
                        <button
                          key={tag.id}
                          type="button"
                          className="flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2.5 text-left hover:bg-muted"
                          disabled={isSelected}
                          onClick={() => onAddTag(tag.name)}
                        >
                          <span>
                            <span className="block text-sm font-semibold text-popover-foreground">
                              #{tag.name}
                            </span>
                            <span className="mt-1 block text-xs text-muted-foreground">
                              사용 횟수 {tag.usageCount}회
                            </span>
                          </span>
                          {isSelected ? (
                            <span className="text-xs font-semibold text-primary">
                              추가됨
                            </span>
                          ) : null}
                        </button>
                      )
                    })
                  )}
                </div>
              ) : null}
            </section>

            <section className="space-y-2">
              <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">
                선택된 태그
              </p>
              <div className="min-h-24 rounded-2xl border border-border/70 bg-muted/25 p-3">
                {draftSourceTagNames.length === 0 ? (
                  <p className="flex min-h-16 items-center text-sm text-muted-foreground">
                    합쳐질 태그가 없습니다.
                  </p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {draftSourceTagNames.map((tagName) => (
                      <span
                        key={tagName}
                        className="inline-flex max-w-full items-center gap-2 rounded-full border border-border bg-background px-3 py-2 text-sm font-semibold text-foreground"
                      >
                        <span className="max-w-[13rem] truncate">#{tagName}</span>
                        <button
                          type="button"
                          className="-mr-1 flex size-6 shrink-0 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                          aria-label={`${tagName} 태그 제거`}
                          onClick={() => onRemoveTag(tagName)}
                        >
                          <X className="size-3.5" />
                        </button>
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </section>
          </div>

          <DialogFooter className="border-t border-border/70 bg-muted/20 px-6 py-4">
            <Button
              type="button"
              variant="outline"
              className="h-11 px-5 text-sm font-semibold sm:min-w-24"
              onClick={closeEditDialog}
            >
              취소
            </Button>
            <Button
              type="button"
              variant="default"
              className="h-11 px-5 text-sm font-semibold sm:min-w-32"
              onClick={applyDraftSourceTags}
            >
              변경사항 반영
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}

function TagChipGroup({
  label,
  tags,
  tone,
}: {
  label: string
  tags: string[]
  tone: "target" | "source"
}) {
  return (
    <div className="min-w-0 space-y-2">
      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">
        {label}
      </p>
      <div className="flex flex-wrap gap-2">
        {tags.map((tag) => (
          <span
            key={tag}
            className={
              tone === "target"
                ? "rounded-full border border-primary/30 bg-primary/10 px-3 py-1.5 text-sm font-semibold text-primary shadow-sm"
                : "rounded-full border border-border/80 bg-muted/45 px-3 py-1.5 text-sm font-medium text-foreground"
            }
          >
            #{tag}
          </span>
        ))}
      </div>
    </div>
  )
}
