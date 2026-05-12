import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-[11px] font-semibold tracking-[0.01em] transition-colors",
  {
    variants: {
      variant: {
        default: "border-primary/20 bg-primary/12 text-primary",
        secondary: "border-secondary bg-secondary text-secondary-foreground",
        outline: "border-border bg-background/70 text-foreground",
        success: "border-success/25 bg-success/12 text-[color:var(--success)]",
        warning: "border-warning/25 bg-warning/12 text-[color:var(--warning)]",
        destructive: "border-destructive/25 bg-destructive/12 text-destructive",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />
}
