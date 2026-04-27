import type { TagSource } from "../_types/tag.types"

export function getTagSourceBadgeClass(source: TagSource) {
  return source === "AI"
    ? "border-cyan-300 bg-cyan-100 text-cyan-800 shadow-sm dark:border-cyan-400/55 dark:bg-cyan-400/16 dark:text-cyan-100"
    : "border-secondary bg-secondary text-secondary-foreground shadow-sm"
}
