import { useQuery } from "@tanstack/react-query"
import { worklogService } from "../_service/worklog.service"
import type { GetWorklogsParams } from "../_types/worklog.types"

export const worklogKeys = {
  all: ["worklogs"] as const,
  list: (params: GetWorklogsParams = {}) =>
    [...worklogKeys.all, "list", params] as const,
}

export function useWorklogList(params: GetWorklogsParams = {}) {
  return useQuery({
    queryKey: worklogKeys.list(params),
    queryFn: () => worklogService.getWorklogs(params),
  })
}
