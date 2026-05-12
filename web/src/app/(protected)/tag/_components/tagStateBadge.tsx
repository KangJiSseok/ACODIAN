import { ArrowLeftRight, CheckCheck, CircleAlert } from "lucide-react"
import type { TagMergeState } from "../_types/tag.types"
import { cn } from "@/lib/utils"

const tagStateMeta: Record<
  TagMergeState,
  {
    label: string
    icon: typeof CheckCheck
    className: string
  }
> = {
  ACTIVE: {
    label: "운영중",
    icon: CheckCheck,
    className:
      "border-[color:var(--success)]/35 bg-[color:var(--success)]/12 text-[color:var(--success)] shadow-sm",
  },
  REVIEW: {
    label: "검토 필요",
    icon: CircleAlert,
    className:
      "border-[color:var(--warning)]/30 bg-[color:var(--warning)]/10 text-[color:var(--warning)] shadow-sm",
  },
  MERGE_CANDIDATE: {
    label: "병합 후보",
    icon: ArrowLeftRight,
    className:
      "border-border bg-muted text-muted-foreground shadow-sm",
  },
}

export function TagStateBadge({ state }: { state: TagMergeState }) {
  const { icon: Icon, label, className } = tagStateMeta[state]

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[11px] font-medium tracking-[0.01em]",
        className,
      )}
    >
      <Icon className="size-3.5" />
      <span>{label}</span>
    </span>
  )
}
