import { apiClient } from "@/app/_common/service/api-client"
import type { PageResponse } from "@/app/_common/types/api.types"
import type {
  SearchTagsParams,
  TagItem,
  TagMergeCandidate,
  TagMergeCandidateApiItem,
  TagMergeCandidatesApiResponse,
  TagMergeSourceApiItem,
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

function toStringValue(value: unknown) {
  if (typeof value === "string") {
    const trimmed = value.trim()
    return trimmed || undefined
  }

  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value)
  }

  return undefined
}

function toSourceTagName(source: TagMergeSourceApiItem | string) {
  if (typeof source === "string") {
    return toStringValue(source)
  }

  return (
    toStringValue(source.tagName) ??
    toStringValue(source.name) ??
    toStringValue(source.sourceTagName)
  )
}

function toSourceTagNames(item: TagMergeCandidateApiItem) {
  const names =
    item.sourceTagNames ??
    item.mergedTagNames ??
    item.mergeSourceTagNames ??
    item.sourceTags ??
    item.tags ??
    []

  return names
    .map((source) => toSourceTagName(source))
    .filter((name): name is string => Boolean(name))
}

function toTagMergeCandidate(
  item: TagMergeCandidateApiItem,
  index: number
): TagMergeCandidate | null {
  const targetTagName =
    toStringValue(item.targetTagName) ??
    toStringValue(item.mergedTagName) ??
    toStringValue(item.mergeTargetTagName) ??
    toStringValue(item.tagName)
  const sourceTagNames = toSourceTagNames(item)

  if (!targetTagName || sourceTagNames.length === 0) {
    return null
  }

  return {
    id: toStringValue(item.id) ?? `${targetTagName}-${index}`,
    targetTagName,
    sourceTagNames,
  }
}

function getMergeCandidateItems(
  response: TagMergeCandidatesApiResponse
): TagMergeCandidateApiItem[] {
  if (Array.isArray(response)) {
    return response
  }

  return response.items ?? response.candidates ?? response.mergeCandidates ?? []
}

function toTagMergeCandidates(response: TagMergeCandidatesApiResponse) {
  return getMergeCandidateItems(response)
    .map(toTagMergeCandidate)
    .filter((candidate): candidate is TagMergeCandidate => candidate !== null)
}

const mockTagMergeCandidates: TagMergeCandidatesApiResponse = [
  {
    id: "mock-merge-1",
    targetTagName: "결산",
    sourceTagNames: ["월말결산", "결산업무"],
  },
]

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

  async getMergeCandidates() {
    return toTagMergeCandidates(mockTagMergeCandidates)
  },
}
