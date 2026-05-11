import { useQuery, type UseQueryOptions } from "@tanstack/react-query"
import { worklogService } from "../_service/worklog.service"
import type { PageResponse } from "@/app/_common/types/api.types"
import type {
  GetWorklogsParams,
  SearchWorklogsParams,
  WorklogListItem,
} from "../_types/worklog.types"

type WorklogPageQueryOptions = Pick<
  UseQueryOptions<PageResponse<WorklogListItem>>,
  "enabled"
>

export const worklogKeys = {
  all: ["worklogs"] as const,
  list: (params: GetWorklogsParams = {}) =>
    [...worklogKeys.all, "list", params] as const,
  search: (params: SearchWorklogsParams = {}) =>
    [...worklogKeys.all, "search", params] as const,
  filterOptions: () => [...worklogKeys.all, "filter-options"] as const,
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

export function useWorklogFilterOptions() {
  return useQuery({
    queryKey: worklogKeys.filterOptions(),
    queryFn: () => worklogService.getFilterOptions(),
  })
}

export function useWorklogDetail(worklogId: number) {
  return useQuery({
    queryKey: worklogKeys.detail(worklogId),
    queryFn: () => worklogService.getById(worklogId),
    enabled: Number.isFinite(worklogId),
  })
}
