import {
  keepPreviousData,
  useInfiniteQuery,
  useMutation,
  useQuery,
  type UseQueryOptions,
} from "@tanstack/react-query"
import { worklogService } from "../_service/worklog.service"
import type { PageResponse } from "@/app/_common/types/api.types"
import type {
  GetWorklogsParams,
  SearchPredecessorCandidatesParams,
  SearchSemanticWorklogsParams,
  SearchTagsParams,
  SearchWorklogsParams,
  WorklogListItem,
  WorklogOptionPredecessorCandidate,
  WorklogOptionTagItem,
} from "../_types/worklog.types"

type WorklogPageQueryOptions = Pick<
  UseQueryOptions<PageResponse<WorklogListItem>>,
  "enabled"
>

type PredecessorCandidatesQueryOptions = Pick<
  UseQueryOptions<PageResponse<WorklogOptionPredecessorCandidate>>,
  "enabled"
>

type TagSearchQueryOptions = Pick<
  UseQueryOptions<PageResponse<WorklogOptionTagItem>>,
  "enabled" | "retry"
>

type TagInfiniteSearchQueryOptions = {
  enabled?: boolean
  retry?: boolean | number
}

export const worklogKeys = {
  all: ["worklogs"] as const,
  lists: () => [...worklogKeys.all, "list"] as const,
  list: (params: GetWorklogsParams = {}) =>
    [...worklogKeys.lists(), params] as const,
  searches: () => [...worklogKeys.all, "search"] as const,
  search: (params: SearchWorklogsParams = {}) =>
    [...worklogKeys.searches(), params] as const,
  filterOptions: () => [...worklogKeys.all, "filter-options"] as const,
  predecessorCandidates: (params: SearchPredecessorCandidatesParams) =>
    [...worklogKeys.all, "predecessor-candidates", params] as const,
  tagSearch: (params: SearchTagsParams = {}) =>
    [...worklogKeys.all, "tags", params] as const,
  tagInfiniteSearch: (params: SearchTagsParams = {}) =>
    [...worklogKeys.all, "tags", "infinite", params] as const,
  tagNames: (tagNames: string[]) =>
    [...worklogKeys.all, "tags", "names", tagNames] as const,
  detail: (worklogId: number) => [...worklogKeys.all, "detail", worklogId] as const,
}

export function useWorklogList(
  params: GetWorklogsParams = {},
  options: WorklogPageQueryOptions = {}
) {
  return useQuery({
    queryKey: worklogKeys.list(params),
    queryFn: () => worklogService.getWorklogs(params),
    ...options,
  })
}

export function useWorklogSearch(
  params: SearchWorklogsParams = {},
  options: WorklogPageQueryOptions = {}
) {
  return useQuery({
    queryKey: worklogKeys.search(params),
    queryFn: () => worklogService.searchWorklogs(params),
    ...options,
  })
}

export function useWorklogSemanticSearch() {
  return useMutation({
    mutationFn: (params: SearchSemanticWorklogsParams) =>
      worklogService.searchSemanticWorklogs(params),
  })
}

export function useWorklogFilterOptions() {
  return useQuery({
    queryKey: worklogKeys.filterOptions(),
    queryFn: () => worklogService.getFilterOptions(),
  })
}

export function usePredecessorCandidates(
  params: SearchPredecessorCandidatesParams,
  options: PredecessorCandidatesQueryOptions = {}
) {
  return useQuery({
    queryKey: worklogKeys.predecessorCandidates(params),
    queryFn: () => worklogService.searchPredecessorCandidates(params),
    enabled: Number.isFinite(params.teamId) && params.teamId > 0,
    placeholderData: keepPreviousData,
    ...options,
  })
}

export function useWorklogTagSearch(
  params: SearchTagsParams = {},
  options: TagSearchQueryOptions = {}
) {
  return useQuery({
    queryKey: worklogKeys.tagSearch(params),
    queryFn: () => worklogService.searchTags(params),
    placeholderData: keepPreviousData,
    ...options,
  })
}

export function useWorklogTagInfiniteSearch(
  query: string,
  options: TagInfiniteSearchQueryOptions = {}
) {
  const pageSize = 5
  const normalizedQuery = query.trim()
  const { enabled: optionEnabled, retry } = options
  const enabled = (optionEnabled ?? true) && normalizedQuery.length > 0

  return useInfiniteQuery({
    queryKey: worklogKeys.tagInfiniteSearch({
      query: normalizedQuery || undefined,
      pageSize,
    }),
    queryFn: ({ pageParam }) =>
      worklogService.searchTags({
        query: normalizedQuery || undefined,
        page: pageParam,
        pageSize,
      }),
    initialPageParam: 1,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.page + 1 : undefined,
    enabled,
    retry: retry ?? false,
  })
}

export function useResolvedWorklogTags(
  tagNames: string[],
  options: Pick<
    UseQueryOptions<WorklogOptionTagItem[]>,
    "enabled" | "retry"
  > = {}
) {
  const normalizedTagNames = Array.from(
    new Set(tagNames.map((tagName) => tagName.trim()).filter(Boolean))
  )

  return useQuery({
    queryKey: worklogKeys.tagNames(normalizedTagNames),
    queryFn: async () => {
      const pages = await Promise.all(
        normalizedTagNames.map((tagName) =>
          worklogService.searchTags({ query: tagName, pageSize: 5 })
        )
      )

      return pages
        .flatMap((page) => page.items)
        .filter((tag) => normalizedTagNames.includes(tag.tagName))
    },
    enabled: normalizedTagNames.length > 0,
    retry: false,
    ...options,
  })
}

export function useWorklogDetail(worklogId: number) {
  return useQuery({
    queryKey: worklogKeys.detail(worklogId),
    queryFn: () => worklogService.getById(worklogId),
    enabled: Number.isFinite(worklogId),
  })
}
