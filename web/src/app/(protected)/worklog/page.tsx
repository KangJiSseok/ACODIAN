"use client"

import Link from "next/link"
import { useMemo, useState } from "react"
import { ChevronDown, RefreshCw, Search, SlidersHorizontal } from "lucide-react"
import { canCreateWorklog } from "./_utils/accessControl"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { Pagination } from "@/app/_common/components/data-display/pagination"
import { LegendHelpDialog } from "./_components/legendHelpDialog"
import { useAuth } from "./_hooks/useAuth"
import { ImportanceBadge } from "./_components/importanceBadge"
import { StatusBadge } from "./_components/statusBadge"
import {
  useWorklogList,
  useWorklogFilterOptions,
  useWorklogSearch,
} from "./_hooks/useWorklogList"
import { WorklogList } from "./_components/worklogList"
import {
  worklogImportanceLegendOrder,
  worklogStatusLegendOrder,
} from "./_components/worklogBadgeConfig"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select } from "@/components/ui/select"
import { cn } from "@/lib/utils"
import {
  getImportanceLabel,
  getWorklogStatusLabel,
} from "./_utils/worklogFormat"
import type {
  ImportanceLevel,
  SearchWorklogsParams,
  WorklogStatus,
} from "./_types/worklog.types"

const WORKLOG_PAGE_SIZE = 10

const DEFAULT_FILTERS = {
  teamId: "all",
  status: "all",
  importance: "all",
  authorId: "all",
  tagId: "all",
  period: "ALL",
}

type WorklogFilterState = typeof DEFAULT_FILTERS

export default function WorklogPage() {
  const { user } = useAuth()
  const [page, setPage] = useState(1)
  const [searchInput, setSearchInput] = useState("")
  const [query, setQuery] = useState("")
  const [showFilters, setShowFilters] = useState(false)
  const [filters, setFilters] = useState<WorklogFilterState>(DEFAULT_FILTERS)
  const canCreate = canCreateWorklog(user)
  const { data: filterOptions } = useWorklogFilterOptions()
  const memberOptions = useMemo(() => {
    const indexed = new Map<number, string>()
    const teams =
      filters.teamId === "all"
        ? filterOptions?.teams ?? []
        : (filterOptions?.teams ?? []).filter(
            (team) => String(team.teamId) === filters.teamId
          )

    teams.forEach((team) => {
      team.members.forEach((member) => {
        indexed.set(member.userId, member.userName)
      })
    })

    return Array.from(indexed, ([userId, userName]) => ({ userId, userName }))
  }, [filterOptions, filters.teamId])
  const hasSearchCondition =
    query.trim().length > 0 ||
    Object.values(filters).some((value) => value !== "all" && value !== "ALL")
  const listParams = useMemo(
    () => ({
      page,
      pageSize: WORKLOG_PAGE_SIZE,
    }),
    [page]
  )
  const searchParams = useMemo<SearchWorklogsParams>(
    () => ({
      page,
      pageSize: WORKLOG_PAGE_SIZE,
      keyword: query.trim() || undefined,
      teamId: filters.teamId === "all" ? undefined : Number(filters.teamId),
      statusCode:
        filters.status === "all" ? undefined : (filters.status as WorklogStatus),
      importanceCode:
        filters.importance === "all"
          ? undefined
          : (filters.importance as ImportanceLevel),
      authorId:
        filters.authorId === "all" ? undefined : Number(filters.authorId),
      tagId: filters.tagId === "all" ? undefined : Number(filters.tagId),
      period:
        filters.period === "ALL"
          ? undefined
          : (filters.period as SearchWorklogsParams["period"]),
    }),
    [filters, page, query]
  )
  const listQuery = useWorklogList(listParams, {
    enabled: !hasSearchCondition,
  })
  const searchQuery = useWorklogSearch(searchParams, {
    enabled: hasSearchCondition,
  })
  const activeQuery = hasSearchCondition ? searchQuery : listQuery
  const worklogPage = activeQuery.data
  const isLoading = activeQuery.isLoading
  const isError = activeQuery.isError
  const worklogs = worklogPage?.items ?? []
  const activeFilterCount = Object.values(filters).filter(
    (value) => value !== "all" && value !== "ALL"
  ).length

  function updateFilters(nextFilters: WorklogFilterState) {
    setFilters(nextFilters)
    setPage(1)
  }

  function submitSearch() {
    setQuery(searchInput.trim())
    setPage(1)
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="업무 검색" />
      <div className="space-y-4">
        <div>
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            업무 탐색
          </h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            업무 제목과 내용을 검색하고 팀, 상태, 중요도 등 조건을 조합해 필요한
            업무일지를 빠르게 찾아보세요.
          </p>
        </div>

        <div className="flex flex-col gap-4">
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
                className="h-12 pl-11"
                placeholder="업무 제목 또는 내용으로 검색하세요"
              />
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
              <Button
                type="button"
                variant="secondary"
                className="h-12 justify-center"
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
                    showFilters && "rotate-180"
                  )}
                />
              </Button>
            </div>
          </form>

          <div
            className={cn(
              "grid overflow-hidden transition-[grid-template-rows,opacity,margin] duration-300 ease-out",
              showFilters
                ? "mt-0 grid-rows-[1fr] opacity-100"
                : "mt-[-4px] grid-rows-[0fr] opacity-0"
            )}
          >
            <div className="overflow-hidden">
              <div className="space-y-4 pb-4 pt-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                    Worklog Filters
                  </p>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-9 w-9"
                    aria-label="필터 초기화"
                    onClick={() => {
                      setSearchInput("")
                      setQuery("")
                      updateFilters(DEFAULT_FILTERS)
                    }}
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                </div>
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      팀
                    </p>
                    <Select
                      value={filters.teamId}
                      options={[
                        { label: "전체 팀", value: "all" },
                        ...(filterOptions?.teams ?? []).map((team) => ({
                          label: team.teamName,
                          value: String(team.teamId),
                        })),
                      ]}
                      onChange={(event) =>
                        updateFilters({
                          ...filters,
                          teamId: event.target.value,
                          authorId: "all",
                        })
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      상태
                    </p>
                    <Select
                      value={filters.status}
                      options={[
                        { label: "전체 상태", value: "all" },
                        ...worklogStatusLegendOrder.map((status) => ({
                          label: getWorklogStatusLabel(status),
                          value: status,
                        })),
                      ]}
                      onChange={(event) =>
                        updateFilters({ ...filters, status: event.target.value })
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      중요도
                    </p>
                    <Select
                      value={filters.importance}
                      options={[
                        { label: "전체 중요도", value: "all" },
                        { label: getImportanceLabel("URGENT"), value: "URGENT" },
                        { label: getImportanceLabel("HIGH"), value: "HIGH" },
                        { label: getImportanceLabel("NORMAL"), value: "NORMAL" },
                        { label: getImportanceLabel("LOW"), value: "LOW" },
                      ]}
                      onChange={(event) =>
                        updateFilters({
                          ...filters,
                          importance: event.target.value,
                        })
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      작성자
                    </p>
                    <Select
                      value={filters.authorId}
                      options={[
                        { label: "전체 작성자", value: "all" },
                        ...memberOptions.map((member) => ({
                          label: member.userName,
                          value: String(member.userId),
                        })),
                      ]}
                      onChange={(event) =>
                        updateFilters({ ...filters, authorId: event.target.value })
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      태그
                    </p>
                    <Select
                      value={filters.tagId}
                      options={[
                        { label: "전체 태그", value: "all" },
                        ...(filterOptions?.tags ?? []).map((tag) => ({
                          label: `#${tag.tagName}`,
                          value: String(tag.tagId),
                        })),
                      ]}
                      onChange={(event) =>
                        updateFilters({ ...filters, tagId: event.target.value })
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      기간
                    </p>
                    <Select
                      value={filters.period}
                      options={[
                        { label: "전체 기간", value: "ALL" },
                        { label: "최근 7일", value: "LAST_7" },
                        { label: "최근 30일", value: "LAST_30" },
                        { label: "최근 90일", value: "LAST_90" },
                      ]}
                      onChange={(event) =>
                        updateFilters({ ...filters, period: event.target.value })
                      }
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="pt-2">
          <div className="grid gap-3 pb-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
            <div className="flex items-center gap-2">
              <p className="text-sm text-muted-foreground">
                표시 중인 업무{" "}
                <span className="ml-1 font-semibold text-foreground">
                  {worklogPage?.totalCount ?? 0}건
                </span>
              </p>
              <LegendHelpDialog
                title="업무 아이콘 안내"
                description="업무 카드에서 보이는 상태와 중요도 아이콘 의미를 빠르게 확인할 수 있습니다."
                buttonLabel="업무 아이콘 안내 열기"
                sections={[
                  {
                    title: "상태",
                    content: worklogStatusLegendOrder.map((status) => (
                      <StatusBadge key={status} status={status} />
                    )),
                  },
                  {
                    title: "중요도",
                    content: worklogImportanceLegendOrder.map((importance) => (
                      <ImportanceBadge key={importance} importance={importance} />
                    )),
                  },
                ]}
                className="h-8 w-8"
              />
            </div>
            {canCreate ? (
              <Button
                asChild
                variant="default"
                className="h-10 min-w-32 px-6 text-sm font-semibold"
              >
                <Link href="/worklog/create">업무 등록</Link>
              </Button>
            ) : null}
          </div>

          {isLoading ? (
            <div className="workspace-empty rounded-xl px-6 py-10 text-center text-sm">
              업무 목록을 불러오는 중입니다.
            </div>
          ) : isError ? (
            <div className="workspace-empty rounded-xl px-6 py-10 text-center text-sm">
              업무 목록을 불러오지 못했습니다.
            </div>
          ) : (
            <WorklogList worklogs={worklogs} />
          )}

          <div className="pt-6">
            <Pagination
              page={worklogPage?.page ?? page}
              totalPages={worklogPage?.totalPages ?? 1}
              onPageChange={setPage}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
