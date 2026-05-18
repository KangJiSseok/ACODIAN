"use client"

import Link from "next/link"
import { useMemo, useState } from "react"
import {
  ChevronDown,
  Plus,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Sparkles,
} from "lucide-react"
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
  formatDate,
  getImportanceLabel,
  getWorklogStatusLabel,
} from "./_utils/worklogFormat"
import type {
  ImportanceLevel,
  SearchWorklogsParams,
  WorklogListItem,
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

const aiPromptExamples = [
  "지난주 완료된 장애 대응 업무 보여줘",
  "AI 평가셋 관련 업무 요약해줘",
  "담당자별 진행 업무 찾아줘",
  "마감 임박 업무만 우선순위로 정리해줘",
]

type AiMessage = {
  id: number
  role: "user" | "assistant"
  content: string
  isLoading?: boolean
}

type WorklogFilterState = typeof DEFAULT_FILTERS

export default function WorklogPage() {
  const { user } = useAuth()
  const [page, setPage] = useState(1)
  const [searchInput, setSearchInput] = useState("")
  const [query, setQuery] = useState("")
  const [aiMode, setAiMode] = useState(false)
  const [aiDraftQuery, setAiDraftQuery] = useState("")
  const [aiMessages, setAiMessages] = useState<AiMessage[]>([])
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
  const isFetching = activeQuery.isFetching
  const isError = activeQuery.isError
  const worklogs = worklogPage?.items ?? []
  const hasAiConversation = aiMessages.length > 0
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

  function submitAiQuestion(question: string) {
    const normalizedQuestion = question.trim()

    if (!normalizedQuestion) return

    const nextMessageId = Date.now()
    const loadingMessageId = nextMessageId + 1

    setAiMessages((prev) => [
      ...prev,
      {
        id: nextMessageId,
        role: "user",
        content: normalizedQuestion,
      },
      {
        id: loadingMessageId,
        role: "assistant",
        content:
          "AI 검색 조건을 적용했습니다. 오른쪽 추천 카드에서 관련 업무일지를 확인할 수 있습니다.",
      },
    ])
    setAiDraftQuery("")
    setSearchInput(normalizedQuestion)
    setQuery(normalizedQuestion)
    setPage(1)
    setShowFilters(false)
  }

  return (
    <div
      className={cn(
        "flex flex-col gap-6",
        aiMode && "worklog-ai-mode min-h-0 flex-1 overflow-hidden"
      )}
    >
      <PageHeader title="업무 검색" />
      <div
        className={cn(
          "space-y-4",
          aiMode && "min-h-0 flex-1 overflow-hidden",
          aiMode && !hasAiConversation && "min-h-[calc(100vh-12rem)]"
        )}
      >
        <div
          className={cn(
            "transition-all duration-500 ease-out",
            aiMode &&
              "pointer-events-none max-h-0 -translate-y-2 overflow-hidden opacity-0"
          )}
        >
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            업무 탐색
          </h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            업무 제목을 검색하고 팀, 상태, 중요도 등 조건을 조합해 필요한
            업무일지를 빠르게 찾아보세요.
          </p>
        </div>

        {!aiMode || !hasAiConversation ? (
        <div
          className={cn(
            "flex flex-col gap-4 transition-all duration-500 ease-out",
            aiMode && "mx-auto mt-[18vh] w-full max-w-5xl gap-6 px-0 py-0 ai-search-lift"
          )}
        >
          {aiMode ? (
            <div className="text-center transition-all duration-500 ease-out">
              <p className="text-[clamp(1.7rem,4vw,3.15rem)] font-bold leading-tight text-foreground">
                안녕하세요, 어떤 업무일지를 찾으려고 하시나요?
              </p>
              <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
                현재 화면의 필터 조건을 함께 반영합니다. 업무명, 담당자, 상태,
                마감 조건을 자연스럽게 입력해보세요.
              </p>
            </div>
          ) : null}
          <form
            className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"
            onSubmit={(event) => {
              event.preventDefault()
              if (aiMode) {
                submitAiQuestion(aiDraftQuery)
                return
              }
              submitSearch()
            }}
          >
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={aiMode ? aiDraftQuery : searchInput}
                onChange={(event) => {
                  if (aiMode) {
                    setAiDraftQuery(event.target.value)
                    return
                  }
                  setSearchInput(event.target.value)
                }}
                className={cn(
                  "h-12 rounded-2xl pl-11 transition-all duration-500",
                  aiMode &&
                    "h-16 border-border/70 bg-card/90 pr-4 text-[16px] shadow-[0_18px_70px_-42px_rgba(15,23,42,0.65),inset_0_1px_0_rgba(255,255,255,0.08)] backdrop-blur",
                  !aiMode && "pr-32"
                )}
                placeholder={
                  aiMode
                    ? "어떤 업무를 찾고 싶으신가요?"
                    : "업무 제목으로 검색하세요"
                }
              />
              {!aiMode ? (
                <Button
                  type="button"
                  variant="secondary"
                  className="ai-mode-trigger absolute right-2 top-1/2 h-9 -translate-y-1/2 rounded-xl px-3 text-sm font-semibold"
                  onClick={() => {
                    setAiMode(true)
                    setShowFilters(false)
                    setAiDraftQuery(searchInput)
                  }}
                >
                  <Sparkles className="size-4" />
                  AI 검색
                </Button>
              ) : null}
            </div>
            <div
              className={cn(
                "flex w-full items-center gap-2 sm:w-auto",
                aiMode && "hidden"
              )}
            >
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
              showFilters && !aiMode
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

          {aiMode ? (
            <div className="grid gap-2 pt-1 sm:grid-cols-2">
              {aiPromptExamples.map((example) => (
                <button
                  key={example}
                  type="button"
                  className="rounded-2xl border border-border/70 bg-muted/35 px-4 py-3 text-left text-sm font-medium text-foreground transition-all duration-200 hover:border-primary/35 hover:bg-primary/8 hover:text-primary"
                  onClick={() => {
                    setAiDraftQuery(example)
                    submitAiQuestion(example)
                  }}
                >
                  {example}
                </button>
              ))}
            </div>
          ) : null}
        </div>
        ) : null}

        {aiMode ? (
          <AiConversationWorkspace
            messages={aiMessages}
            resultWorklogs={worklogs}
            isResultLoading={isLoading || isFetching}
            isResultError={isError}
            composerValue={aiDraftQuery}
            onComposerChange={setAiDraftQuery}
            onComposerSubmit={() => submitAiQuestion(aiDraftQuery)}
          />
        ) : null}

        <div className="pt-2">
          <div
            className={cn(
              "transition-all duration-500 ease-out",
              aiMode &&
                "pointer-events-none max-h-0 -translate-y-3 overflow-hidden opacity-0"
            )}
          >
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
                <Link href="/worklog/create">
                  <Plus className="size-4" />
                  업무 등록
                </Link>
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
    </div>
  )
}

function AiConversationWorkspace({
  messages,
  resultWorklogs,
  isResultLoading,
  isResultError,
  composerValue,
  onComposerChange,
  onComposerSubmit,
}: {
  messages: AiMessage[]
  resultWorklogs: WorklogListItem[]
  isResultLoading: boolean
  isResultError: boolean
  composerValue: string
  onComposerChange: (value: string) => void
  onComposerSubmit: () => void
}) {
  if (messages.length === 0) {
    return null
  }

  return (
    <section className="mx-auto grid h-[calc(100svh-11rem)] max-h-[calc(100svh-11rem)] w-full max-w-[86rem] min-h-0 gap-3 overflow-hidden pt-0 xl:grid-cols-[minmax(0,1fr)_minmax(280px,350px)] xl:items-stretch">
      <div className="flex min-h-0 min-w-0 flex-col">
        <div className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto px-1 pb-2 pr-2">
          {messages.map((message) => (
            <div
              key={message.id}
              className={cn(
                "flex",
                message.role === "user" ? "justify-end" : "justify-start"
              )}
            >
              <div
                className={cn(
                  "rounded-[20px] px-4 py-3 text-sm leading-6",
                  message.role === "user"
                    ? "max-w-[min(28rem,74%)] bg-primary text-primary-foreground"
                    : "max-w-[min(44rem,80%)] border border-border/60 bg-card/78 text-foreground backdrop-blur",
                  message.isLoading &&
                    "min-w-16 animate-pulse text-center text-lg"
                )}
              >
                {message.content}
              </div>
            </div>
          ))}
        </div>

        <AiBottomComposer
          value={composerValue}
          onChange={onComposerChange}
          onSubmit={onComposerSubmit}
        />
      </div>

      <aside className="mb-3 flex h-[calc(100%-0.75rem)] min-h-0 flex-col rounded-[20px] border border-border/70 bg-card/72 p-3 backdrop-blur">
        <div className="mb-2 flex shrink-0 items-center justify-between gap-3 px-1">
          <div>
            <p className="text-sm font-semibold text-foreground">
              AI 추천 업무일지
            </p>
            <p className="mt-0.5 text-xs text-muted-foreground">
              현재 질문과 화면 필터를 함께 반영한 결과입니다.
            </p>
          </div>
          <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
            {resultWorklogs.length}건
          </span>
        </div>

        <div className="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
          {isResultLoading ? (
            <div className="rounded-2xl border border-dashed border-border/80 px-4 py-8 text-center text-sm text-muted-foreground">
              AI 검색 결과를 불러오는 중입니다.
            </div>
          ) : isResultError ? (
            <div className="rounded-2xl border border-dashed border-border/80 px-4 py-8 text-center text-sm text-muted-foreground">
              AI 검색 결과를 불러오지 못했습니다.
            </div>
          ) : resultWorklogs.length > 0 ? (
            resultWorklogs.map((worklog) => (
              <AiResultCard key={worklog.id} worklog={worklog} />
            ))
          ) : (
            <div className="rounded-2xl border border-dashed border-border/80 px-4 py-8 text-center text-sm text-muted-foreground">
              조건에 맞는 업무 카드가 없습니다.
            </div>
          )}
        </div>
      </aside>
    </section>
  )
}

function AiBottomComposer({
  value,
  onChange,
  onSubmit,
}: {
  value: string
  onChange: (value: string) => void
  onSubmit: () => void
}) {
  return (
    <div className="z-10 mb-3 w-full shrink-0">
      <div className="ai-bottom-composer w-full rounded-[18px] border border-border/80 bg-card/95 px-3 py-2.5 backdrop-blur-xl">
        <div className="relative min-w-0">
          <textarea
            value={value}
            onChange={(event) => onChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key !== "Enter" || event.shiftKey) return
              event.preventDefault()
              onSubmit()
            }}
            className="min-h-[2.75rem] w-full resize-none rounded-[14px] border border-transparent bg-transparent px-1 py-0.5 text-[15px] leading-6 text-foreground outline-none placeholder:text-muted-foreground"
            placeholder="업무일지에 대해 이어서 물어보세요"
          />
          <div className="mt-1 flex items-center gap-2 text-muted-foreground">
            <p className="text-xs">Enter로 전송 · Shift+Enter로 줄바꿈</p>
          </div>
        </div>
      </div>
    </div>
  )
}

function AiResultCard({ worklog }: { worklog: WorklogListItem }) {
  return (
    <Link
      href={`/worklog/detail/${worklog.id}`}
      className="block rounded-2xl border border-border/70 bg-muted/25 p-3 transition-all duration-200 hover:border-primary/35 hover:bg-primary/8"
    >
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge status={worklog.status} />
        <ImportanceBadge importance={worklog.importance} />
      </div>
      <p className="mt-2 line-clamp-2 text-sm font-semibold leading-5 text-foreground">
        {worklog.title}
      </p>
      <p className="mt-1.5 line-clamp-2 text-xs leading-5 text-muted-foreground">
        {worklog.aiSummary}
      </p>
      <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
        <span>{worklog.teamName ?? "팀 미지정"}</span>
        <span>{worklog.authorName ?? "작성자 미지정"}</span>
        <span>마감 {formatDate(worklog.dueDate)}</span>
      </div>
    </Link>
  )
}
