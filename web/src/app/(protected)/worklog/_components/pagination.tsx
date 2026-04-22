import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from "lucide-react"
import { cn } from "@/lib/utils"

export function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}) {
  if (totalPages <= 1) return null

  const visiblePages = Array.from({ length: totalPages }, (_, index) => index + 1).filter((value) => {
    if (totalPages <= 7) return true
    return value === 1 || value === totalPages || Math.abs(value - page) <= 1
  })

  return (
    <div className="flex items-center justify-center gap-1.5 pt-4">
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(1)}
        className="inline-flex size-9 items-center justify-center rounded-xl border border-transparent text-muted-foreground transition-all hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-35"
      >
        <ChevronsLeft className="size-4" />
      </button>
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
        className="inline-flex size-9 items-center justify-center rounded-xl border border-transparent text-muted-foreground transition-all hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-35"
      >
        <ChevronLeft className="size-4" />
      </button>
      <div className="flex items-center gap-1">
        {visiblePages.map((value, index) => {
          const prev = visiblePages[index - 1]
          const showGap = prev && value - prev > 1

          return (
            <div key={value} className="flex items-center gap-1">
              {showGap ? <span className="px-1.5 text-xs text-muted-foreground">…</span> : null}
              <button
                type="button"
                onClick={() => onPageChange(value)}
                className={cn(
                  "inline-flex size-9 items-center justify-center rounded-xl border text-sm font-semibold transition-all",
                  value === page
                    ? "border-border bg-accent text-accent-foreground shadow-[var(--shadow-panel)]"
                    : "border-transparent text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                )}
              >
                {value}
              </button>
            </div>
          )
        })}
      </div>
      <button
        type="button"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
        className="inline-flex size-9 items-center justify-center rounded-xl border border-transparent text-muted-foreground transition-all hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-35"
      >
        <ChevronRight className="size-4" />
      </button>
      <button
        type="button"
        disabled={page >= totalPages}
        onClick={() => onPageChange(totalPages)}
        className="inline-flex size-9 items-center justify-center rounded-xl border border-transparent text-muted-foreground transition-all hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-35"
      >
        <ChevronsRight className="size-4" />
      </button>
    </div>
  )
}
