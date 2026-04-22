import * as React from "react"

import { cn } from "@/lib/utils"

export interface SelectOption {
  label: string
  value: string
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  options: SelectOption[]
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, options, ...props }, ref) => {
    return (
      <select
        ref={ref}
        className={cn(
          "h-11 w-full rounded-2xl border border-input bg-input/90 px-4 text-sm text-foreground shadow-sm outline-none transition-all focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/20 focus-visible:ring-offset-2 focus-visible:ring-offset-background",
          className
        )}
        {...props}
      >
        {options.map((option) => (
          <option
            key={option.value}
            value={option.value}
            style={{ color: "var(--foreground)", backgroundColor: "var(--background)" }}
          >
            {option.label}
          </option>
        ))}
      </select>
    )
  }
)

Select.displayName = "Select"
