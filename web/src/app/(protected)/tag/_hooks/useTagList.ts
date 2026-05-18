import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { tagService } from "../_service/tag.service"
import type {
  SearchTagsParams,
  TagMergeCandidate,
  UpdateTagMergeCandidateRequest,
} from "../_types/tag.types"

export const tagKeys = {
  all: ["tags"] as const,
  searches: () => [...tagKeys.all, "search"] as const,
  search: (params: SearchTagsParams = {}) =>
    [...tagKeys.searches(), params] as const,
  mergeCandidates: () => [...tagKeys.all, "merge-candidates"] as const,
}

export function useTagList(params: SearchTagsParams = {}) {
  return useQuery({
    queryKey: tagKeys.search(params),
    queryFn: () => tagService.search(params),
    placeholderData: keepPreviousData,
  })
}

export function useTagMergeCandidates() {
  return useQuery({
    queryKey: tagKeys.mergeCandidates(),
    queryFn: () => tagService.getMergeCandidates(),
  })
}

export function useUpdateTagMergeCandidate() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      mergeCandidateId,
      request,
    }: {
      mergeCandidateId: number
      request: UpdateTagMergeCandidateRequest
    }) => tagService.updateMergeCandidate(mergeCandidateId, request),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: tagKeys.mergeCandidates() }),
  })
}

export function useMergeTagCandidate() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (candidate: TagMergeCandidate) =>
      tagService.mergeCandidate(candidate.numericId),
    onSuccess: (_result, mergedCandidate) => {
      const mergedSourceTagIds = new Set(
        mergedCandidate.sourceTags.map((tag) => tag.id)
      )

      queryClient.setQueryData<TagMergeCandidate[]>(
        tagKeys.mergeCandidates(),
        (current = []) =>
          current
            .filter(
              (candidate) =>
                candidate.numericId !== mergedCandidate.numericId
            )
            .map((candidate) => {
              const sourceTags = candidate.sourceTags.filter(
                (tag) => !mergedSourceTagIds.has(tag.id)
              )

              return {
                ...candidate,
                sourceTags,
                sourceTagNames: sourceTags.map((tag) => tag.name),
              }
            })
      )
    },
  })
}
