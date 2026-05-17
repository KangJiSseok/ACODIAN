import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { tagService } from "../_service/tag.service"
import type { SearchTagsParams } from "../_types/tag.types"

export const tagKeys = {
  all: ["tags"] as const,
  searches: () => [...tagKeys.all, "search"] as const,
  search: (params: SearchTagsParams = {}) =>
    [...tagKeys.searches(), params] as const,
}

export function useTagList(params: SearchTagsParams = {}) {
  return useQuery({
    queryKey: tagKeys.search(params),
    queryFn: () => tagService.search(params),
    placeholderData: keepPreviousData,
  })
}
