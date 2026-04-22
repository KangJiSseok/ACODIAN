import type { AiProcessingStatus } from "../_types/worklog.types"
import { getAiStatusBadgeMeta } from "./worklogBadgeConfig"
import { WorklogBadge } from "./worklogBadge"

export function FileAiStatusBadge({
  status,
  iconOnly = false,
}: {
  status: AiProcessingStatus
  iconOnly?: boolean
}) {
  return <WorklogBadge meta={getAiStatusBadgeMeta(status)} iconOnly={iconOnly} />
}
