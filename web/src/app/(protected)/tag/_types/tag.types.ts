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
  targetTagName: string
  sourceTagNames: string[]
}

export interface TagMergeSourceApiItem {
  id?: number | string
  tagId?: number | string
  name?: string | null
  tagName?: string | null
  sourceTagName?: string | null
}

export interface TagMergeCandidateApiItem {
  id?: number | string
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
