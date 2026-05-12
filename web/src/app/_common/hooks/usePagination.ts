import { useCallback, useMemo, useState } from "react"

export function usePagination<T>(items: T[], pageSize = 10) {
  const [page, setPageState] = useState(1)
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize))
  const currentPage = Math.min(page, totalPages)
  const setPage = useCallback(
    (nextPage: number) => {
      setPageState(Math.max(1, Math.min(nextPage, totalPages)))
    },
    [totalPages]
  )

  const paginated = useMemo(() => {
    const start = (currentPage - 1) * pageSize
    return items.slice(start, start + pageSize)
  }, [items, currentPage, pageSize])

  return {
    page: currentPage,
    setPage,
    totalPages,
    items: paginated,
  }
}
