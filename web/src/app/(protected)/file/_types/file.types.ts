import type { FileRecord } from "@/app/(protected)/worklog/_mock/worklog.mock"
import type { AiProcessingStatus } from "@/app/(protected)/worklog/_types/worklog.types"

export type FileItem = FileRecord

export interface FileFiltersValue {
  type: string
  period: "ALL" | "7D" | "30D" | "90D"
  aiStatus: AiProcessingStatus | "ALL"
}
