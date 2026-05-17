import { apiClient } from "@/app/_common/service/api-client"
import type { PageResponse } from "@/app/_common/types/api.types"
import type {
  SearchTagsParams,
  TagItem,
  TagSearchApiItem,
} from "../_types/tag.types"

function toNumber(value: number | string | null | undefined) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function toTagItem(item: TagSearchApiItem): TagItem | null {
  const id = Number(item.tagId ?? item.id)
  const name = item.tagName ?? item.name

  if (!Number.isFinite(id) || !name) {
    return null
  }

  return {
    id,
    name,
    usageCount: toNumber(item.usageCount),
    createdAt: item.createdAt ?? undefined,
    updatedAt: item.updatedAt ?? undefined,
  }
}

function normalizeSearchParams(params: SearchTagsParams) {
  return {
    page: params.page ?? 1,
    pageSize: params.pageSize ?? 20,
    query: params.query?.trim() || undefined,
  }
}

export const tagService = {
  async search(params: SearchTagsParams = {}) {
    const normalizedParams = normalizeSearchParams(params)
    const response = await apiClient.get<PageResponse<TagSearchApiItem>>(
      "/tags/search",
      {
        params: normalizedParams,
      }
    )

    return {
      ...response,
      items: response.items
        .map(toTagItem)
        .filter((tag): tag is TagItem => tag !== null),
    }
  },
}
