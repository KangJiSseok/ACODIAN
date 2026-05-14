import {
  AlertTriangle,
  ArrowDown,
  ArrowUp,
  CheckCheck,
  CircleDashed,
  LoaderCircle,
  Minus,
  Pause,
  Sparkles,
  X,
} from "lucide-react"
import type {
  AiProcessingStatus,
  ImportanceLevel,
  WorklogStatus,
} from "../_types/worklog.types"
import type { WorklogBadgeMeta } from "./worklogBadge"
import {
  getAiStatusLabel,
  getImportanceLabel,
  getWorklogStatusLabel,
} from "../_utils/worklogFormat"

const statusMetaMap: Record<WorklogStatus, WorklogBadgeMeta> = {
  PENDING: {
    icon: CircleDashed,
    label: getWorklogStatusLabel("PENDING"),
    variant: "outline",
    className: "border-border bg-muted text-muted-foreground",
  },
  IN_PROGRESS: {
    icon: LoaderCircle,
    label: getWorklogStatusLabel("IN_PROGRESS"),
    variant: "default",
    className: "border-primary/25 bg-primary/10 text-primary",
  },
  DONE: {
    icon: CheckCheck,
    label: getWorklogStatusLabel("DONE"),
    variant: "success",
    className: "border-success/25 bg-success/10 text-[color:var(--success)]",
  },
  ON_HOLD: {
    icon: Pause,
    label: getWorklogStatusLabel("ON_HOLD"),
    variant: "warning",
    className: "border-warning/25 bg-warning/10 text-[color:var(--warning)]",
  },
  FAILED: {
    icon: AlertTriangle,
    label: getWorklogStatusLabel("FAILED"),
    variant: "destructive",
    className: "border-destructive/25 bg-destructive/10 text-destructive",
  },
  CANCELLED: {
    icon: X,
    label: getWorklogStatusLabel("CANCELLED"),
    variant: "destructive",
    className: "border-destructive/25 bg-destructive/10 text-destructive",
  },
}

const importanceMetaMap: Record<ImportanceLevel, WorklogBadgeMeta> = {
  URGENT: {
    icon: AlertTriangle,
    label: getImportanceLabel("URGENT"),
    variant: "destructive",
    className: "border-destructive/25 bg-destructive/10 text-destructive",
  },
  HIGH: {
    icon: ArrowUp,
    label: getImportanceLabel("HIGH"),
    variant: "secondary",
    className: "border-primary/25 bg-primary/10 text-primary",
  },
  NORMAL: {
    icon: Minus,
    label: getImportanceLabel("NORMAL"),
    variant: "secondary",
    className: "border-secondary bg-secondary text-secondary-foreground",
  },
  LOW: {
    icon: ArrowDown,
    label: getImportanceLabel("LOW"),
    variant: "secondary",
    className: "border-border bg-muted text-muted-foreground",
  },
}

const aiStatusMetaMap: Record<AiProcessingStatus, WorklogBadgeMeta> = {
  PENDING: {
    icon: Sparkles,
    label: getAiStatusLabel("PENDING"),
    variant: "outline",
    className: "border-border bg-muted text-muted-foreground",
  },
  PROCESSING: {
    icon: LoaderCircle,
    label: getAiStatusLabel("PROCESSING"),
    variant: "default",
    className: "border-primary/25 bg-primary/10 text-primary",
    iconClassName: "animate-spin",
  },
  DONE: {
    icon: CheckCheck,
    label: getAiStatusLabel("DONE"),
    variant: "success",
    className: "border-success/25 bg-success/10 text-[color:var(--success)]",
  },
  FAILED: {
    icon: AlertTriangle,
    label: getAiStatusLabel("FAILED"),
    variant: "destructive",
    className: "border-destructive/25 bg-destructive/10 text-destructive",
  },
}

export const worklogStatusLegendOrder: WorklogStatus[] = [
  "IN_PROGRESS",
  "PENDING",
  "DONE",
  "ON_HOLD",
  "CANCELLED",
]

export const worklogImportanceLegendOrder: ImportanceLevel[] = [
  "URGENT",
  "HIGH",
  "NORMAL",
  "LOW",
]

export function getStatusBadgeMeta(status: WorklogStatus) {
  return statusMetaMap[status]
}

export function getImportanceBadgeMeta(importance: ImportanceLevel) {
  return importanceMetaMap[importance]
}

export function getAiStatusBadgeMeta(status: AiProcessingStatus) {
  return aiStatusMetaMap[status]
}
