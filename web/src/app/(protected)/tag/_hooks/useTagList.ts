import { useMemo } from "react"
import { tagService } from "../_service/tag.service"

export function useTagList() {
  const tags = useMemo(() => tagService.list(), [])

  return { tags }
}
