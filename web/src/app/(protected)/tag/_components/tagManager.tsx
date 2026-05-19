"use client"

import { useState } from "react"
import { RefreshCw, Search } from "lucide-react"
import { Pagination } from "@/app/_common/components/data-display/pagination"
import { ResultCount } from "@/app/_common/components/data-display/resultCount"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useTagList } from "../_hooks"

const TAG_PAGE_SIZE = 20

export function TagManager() {
  const [query, setQuery] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [page, setPage] = useState(1)
  const {
    data: tagPage,
    isError,
    isLoading,
  } = useTagList({
    query,
    page,
    pageSize: TAG_PAGE_SIZE,
  })
  const tags = tagPage?.items ?? []

  function submitSearch() {
    setQuery(searchInput)
    setPage(1)
  }

  return (
    <div className="space-y-6">
      <div className="space-y-4">
        <form
          className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"
          onSubmit={(event) => {
            event.preventDefault()
            submitSearch()
          }}
        >
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              className="h-12 rounded-2xl pl-11 pr-12 text-sm"
              placeholder="태그 이름으로 검색하세요"
            />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="absolute right-3 top-1/2 size-8 -translate-y-1/2 rounded-xl text-muted-foreground"
              aria-label="검색어 초기화"
              onClick={() => {
                setSearchInput("")
                setQuery("")
                setPage(1)
              }}
            >
              <RefreshCw className="size-4" />
            </Button>
          </div>
          <div className="flex w-full items-center gap-2 sm:w-auto">
            <Button
              type="submit"
              variant="default"
              className="h-12 min-w-24 justify-center px-5 text-sm font-semibold"
            >
              <Search className="size-4" />
              검색
            </Button>
          </div>
        </form>

        <div className="h-0.5 bg-foreground/80 dark:bg-foreground/70" />

        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <ResultCount
              label="조회된 태그"
              count={tagPage?.totalCount ?? 0}
              unit="개"
            />
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="rounded-3xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
          태그 목록을 불러오는 중입니다.
        </div>
      ) : isError ? (
        <div className="rounded-3xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
          태그 목록을 불러오지 못했습니다.
        </div>
      ) : tags.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
          조건에 맞는 태그가 없습니다.
        </div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {tags.map((tag) => (
            <Card
              key={tag.id}
              className="rounded-[24px] border-border/75 transition-all duration-300 hover:-translate-y-1"
            >
              <div className="space-y-4 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 space-y-1">
                    <p className="truncate font-medium text-foreground">
                      #{tag.name}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      사용 횟수 {tag.usageCount}회
                    </p>
                  </div>
                </div>

                <div className="rounded-2xl border border-dashed border-border/60 bg-muted/25 p-3 text-sm leading-6 text-muted-foreground">
                  {tag.description || "등록된 태그 설명이 없습니다."}
                  {tag.updatedAt ? (
                    <span className="mt-1 block text-xs">
                      최근 수정 {tag.updatedAt}
                    </span>
                  ) : null}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Pagination
        page={tagPage?.page ?? page}
        totalPages={tagPage?.totalPages ?? 1}
        onPageChange={setPage}
      />
    </div>
  )
}
