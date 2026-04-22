import { CircleHelp } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

type LegendSection = {
  title: string
  content: React.ReactNode
}

export function LegendHelpDialog({
  title,
  description,
  sections,
  buttonLabel = "아이콘 도움말",
  className,
  align = "start",
}: {
  title: string
  description: string
  sections: LegendSection[]
  buttonLabel?: string
  className?: string
  align?: "start" | "end"
}) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!open) return

    const handlePointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false)
      }
    }

    document.addEventListener("mousedown", handlePointerDown)
    document.addEventListener("keydown", handleKeyDown)

    return () => {
      document.removeEventListener("mousedown", handlePointerDown)
      document.removeEventListener("keydown", handleKeyDown)
    }
  }, [open])

  return (
    <div ref={containerRef} className="relative inline-flex">
      <Button
        type="button"
        variant="outline"
        size="icon"
        className={className}
        aria-label={buttonLabel}
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
      >
        <CircleHelp className="size-4" />
      </Button>
      <div
        aria-hidden={!open}
        className={cn(
          "absolute top-[calc(100%+12px)] z-50 w-[min(24rem,calc(100vw-2rem))] transition-transform duration-200 ease-out",
          align === "end" ? "right-0 origin-top-right" : "left-0 origin-top-left",
          open
            ? "visible pointer-events-auto translate-y-0 scale-100"
            : "invisible pointer-events-none -translate-y-1 scale-95"
        )}
      >
        <div
          className={cn(
            "absolute -top-2 h-4 w-4 rotate-45 border border-border bg-popover shadow-sm",
            align === "end" ? "right-3" : "left-3"
          )}
        />
        <div className="relative rounded-2xl border border-border bg-popover p-4 text-popover-foreground shadow-2xl">
          <div className="space-y-1">
            <h3 className="text-sm font-semibold text-popover-foreground">{title}</h3>
            <p className="text-sm leading-6 text-muted-foreground">{description}</p>
          </div>
          <div className="mt-4 space-y-5">
            {sections.map((section) => (
              <section key={section.title} className="space-y-3">
                <h4 className="text-sm font-semibold text-popover-foreground">
                  {section.title}
                </h4>
                <div className="flex flex-wrap gap-2">{section.content}</div>
              </section>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
