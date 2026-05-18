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
  UpdateTagMergeCandidateRequest,
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

function toMergeTagItem(source: TagMergeSourceApiItem | string): TagItem | null {
  if (typeof source === "string") {
    return null
  }

  const id = Number(source.tagId ?? source.id)
  const name =
    toStringValue(source.tagName) ??
    toStringValue(source.name) ??
    toStringValue(source.sourceTagName)

  if (!Number.isFinite(id) || id <= 0 || !name) {
    return null
  }

  return {
    id,
    name,
    usageCount: toNumber(source.usageCount),
  }
}

function toSourceTags(item: TagMergeCandidateApiItem) {
  const sources =
    item.mergeCandidateTags ??
    item.sourceTagNames ??
    item.mergedTagNames ??
    item.mergeSourceTagNames ??
    item.sourceTags ??
    item.tags ??
    []

  return sources
    .map((source) => toMergeTagItem(source))
    .filter((tag): tag is TagItem => tag !== null)
}

function toTagMergeCandidate(
  item: TagMergeCandidateApiItem,
  index: number
): TagMergeCandidate | null {
  const rawId = item.mergeCandidateId ?? item.id
  const numericId = Number(rawId)
  const targetTag =
    toMergeTagItem(item.mergeTargetTag ?? "") ??
    toMergeTagItem({
      id: item.id,
      tagName:
        item.targetTagName ??
        item.mergedTagName ??
        item.mergeTargetTagName ??
        item.tagName,
    })
  const sourceTags = toSourceTags(item)

  if (
    !Number.isFinite(numericId) ||
    numericId <= 0 ||
    !targetTag ||
    sourceTags.length === 0
  ) {
    return null
  }

  return {
    id: toStringValue(rawId) ?? `${targetTag.name}-${index}`,
    numericId,
    statusCode: item.statusCode ?? "PENDING",
    resultDescription: item.resultDescription,
    targetTag,
    sourceTags,
    targetTagName: targetTag.name,
    sourceTagNames: sourceTags.map((sourceTag) => sourceTag.name),
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
    const response = await apiClient.get<TagMergeCandidatesApiResponse>(
      "/tags/merge-candidates",
      {
        params: {
          statusCode: "PENDING",
        },
      }
    )

    return toTagMergeCandidates(response)
  },

  async updateMergeCandidate(
    mergeCandidateId: number,
    request: UpdateTagMergeCandidateRequest
  ) {
    const response = await apiClient.patch<
      TagMergeCandidateApiItem,
      UpdateTagMergeCandidateRequest
    >(`/tags/merge-candidates/${mergeCandidateId}`, request)

    return toTagMergeCandidate(response, 0)
  },

  async mergeCandidate(mergeCandidateId: number) {
    await apiClient.post<void>(`/tags/merge-candidates/${mergeCandidateId}/merge`)
  },
}
