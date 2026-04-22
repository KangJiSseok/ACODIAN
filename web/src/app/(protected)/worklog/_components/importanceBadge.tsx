import type { ImportanceLevel } from "../_types/worklog.types"
import { getImportanceBadgeMeta } from "./worklogBadgeConfig"
import { WorklogBadge } from "./worklogBadge"

export function ImportanceBadge({
  importance,
  iconOnly = false,
}: {
  importance: ImportanceLevel
  iconOnly?: boolean
}) {
  return (
    <WorklogBadge
      meta={getImportanceBadgeMeta(importance)}
      iconOnly={iconOnly}
    />
  )
}
