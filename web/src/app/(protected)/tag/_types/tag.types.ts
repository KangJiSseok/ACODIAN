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
