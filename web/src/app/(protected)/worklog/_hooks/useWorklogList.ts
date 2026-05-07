import { useQuery } from "@tanstack/react-query"
import { worklogService } from "../_service/worklog.service"
import type {
  GetWorklogsParams,
  SearchWorklogsParams,
} from "../_types/worklog.types"

export const worklogKeys = {
  all: ["worklogs"] as const,
  list: (params: GetWorklogsParams = {}) =>
    [...worklogKeys.all, "list", params] as const,
  search: (params: SearchWorklogsParams = {}) =>
    [...worklogKeys.all, "search", params] as const,
  filterOptions: () => [...worklogKeys.all, "filter-options"] as const,
}

export function useWorklogList(params: GetWorklogsParams = {}) {
  return useQuery({
    queryKey: worklogKeys.list(params),
    queryFn: () => worklogService.getWorklogs(params),
  })
}

export function useWorklogSearch(params: SearchWorklogsParams = {}) {
  return useQuery({
    queryKey: worklogKeys.search(params),
    queryFn: () => worklogService.searchWorklogs(params),
  })
}

export function useWorklogFilterOptions() {
  return useQuery({
    queryKey: worklogKeys.filterOptions(),
    queryFn: () => worklogService.getFilterOptions(),
  })
}
