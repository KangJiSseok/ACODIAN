import type { LucideIcon } from "lucide-react"
import { Badge, type BadgeProps } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

export type WorklogBadgeMeta = {
  icon: LucideIcon
  label: string
  variant: NonNullable<BadgeProps["variant"]>
  className: string
  iconClassName?: string
}

export function WorklogBadge({
  meta,
  iconOnly = false,
}: {
  meta: WorklogBadgeMeta
  iconOnly?: boolean
}) {
  const { icon: Icon, label, variant, className, iconClassName } = meta

  return (
    <Badge
      variant={variant}
      aria-label={label}
      title={label}
      className={cn(
        "border",
        className,
        iconOnly && "size-8 shrink-0 justify-center rounded-full p-0"
      )}
    >
      {iconOnly ? (
        <Icon className={cn("size-4", iconClassName)} />
      ) : (
        <>
          <Icon className={cn("size-3.5", iconClassName)} />
          <span>{label}</span>
        </>
      )}
    </Badge>
  )
}
