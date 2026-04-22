import type { WorklogStatus } from "../_types/worklog.types"
import { getStatusBadgeMeta } from "./worklogBadgeConfig"
import { WorklogBadge } from "./worklogBadge"

export function StatusBadge({
  status,
  iconOnly = false,
}: {
  status: WorklogStatus
  iconOnly?: boolean
}) {
  return <WorklogBadge meta={getStatusBadgeMeta(status)} iconOnly={iconOnly} />
}
