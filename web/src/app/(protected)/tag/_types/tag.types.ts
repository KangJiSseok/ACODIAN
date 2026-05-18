export interface TagItem {
  id: number
  name: string
  usageCount: number
  createdAt?: string
  updatedAt?: string
}

export interface TagSearchApiItem {
  id?: number
  tagId?: number
  name?: string
  tagName?: string
  usageCount?: number | string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchTagsParams {
  query?: string
  page?: number
  pageSize?: number
}

export interface TagMergeCandidate {
  id: string
  numericId: number
  statusCode: string
  resultDescription?: string | null
  targetTag: TagItem
  sourceTags: TagItem[]
  targetTagName: string
  sourceTagNames: string[]
}

export interface TagMergeSourceApiItem {
  id?: number | string
  tagId?: number | string
  name?: string | null
  tagName?: string | null
  sourceTagName?: string | null
  usageCount?: number | string | null
}

export interface TagMergeCandidateApiItem {
  id?: number | string
  mergeCandidateId?: number | string
  statusCode?: string | null
  resultDescription?: string | null
  mergeTargetTag?: TagMergeSourceApiItem | null
  mergeCandidateTags?: TagMergeSourceApiItem[] | null
  targetTagName?: string | null
  mergedTagName?: string | null
  mergeTargetTagName?: string | null
  tagName?: string | null
  sourceTagNames?: string[] | null
  mergedTagNames?: string[] | null
  mergeSourceTagNames?: string[] | null
  tags?: Array<TagMergeSourceApiItem | string> | null
  sourceTags?: Array<TagMergeSourceApiItem | string> | null
}

export type TagMergeCandidatesApiResponse =
  | TagMergeCandidateApiItem[]
  | {
      items?: TagMergeCandidateApiItem[]
      candidates?: TagMergeCandidateApiItem[]
      mergeCandidates?: TagMergeCandidateApiItem[]
    }

export interface UpdateTagMergeCandidateRequest {
  mergeTargetTagId: number
  resultDescription?: string | null
  mergeCandidateTagIds: number[]
}
