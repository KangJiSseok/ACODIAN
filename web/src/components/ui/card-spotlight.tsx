"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

export function CardSpotlight({
  className,
  children,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  const [position, setPosition] = React.useState({ x: 120, y: 80 })
  const [hovered, setHovered] = React.useState(false)

  return (
    <div
      className={cn(
        "group/card-spotlight relative overflow-hidden rounded-2xl border border-border/70 bg-card/95 text-card-foreground shadow-[var(--shadow-panel)] backdrop-blur supports-[backdrop-filter]:bg-card/88",
        className
      )}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onMouseMove={(event) => {
        const rect = event.currentTarget.getBoundingClientRect()
        setPosition({
          x: event.clientX - rect.left,
          y: event.clientY - rect.top,
        })
      }}
      {...props}
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover/card-spotlight:opacity-100 group-focus-within/card-spotlight:opacity-100"
        style={{
          background: hovered
            ? `radial-gradient(320px circle at ${position.x}px ${position.y}px, color-mix(in srgb, var(--primary) 18%, transparent), transparent 48%)`
            : undefined,
        }}
      />
      <div
        className="pointer-events-none absolute inset-0 bg-[linear-gradient(180deg,rgba(255,255,255,0.07),transparent_22%,transparent_100%)] opacity-0 transition-opacity duration-300 group-hover/card-spotlight:opacity-100 group-focus-within/card-spotlight:opacity-100 dark:bg-[linear-gradient(180deg,rgba(255,255,255,0.05),transparent_24%,transparent_100%)]"
      />
      <div className="pointer-events-none absolute inset-px rounded-[calc(theme(borderRadius.2xl)-1px)] border border-white/6 dark:border-white/8" />
      <div className="relative z-10">{children}</div>
    </div>
  )
}
