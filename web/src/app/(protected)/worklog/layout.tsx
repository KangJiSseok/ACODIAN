import type { ReactNode } from "react"

export default function WorklogLayout({ children }: { children: ReactNode }) {
  return (
    <div className="worklog-root isolate flex min-h-[calc(100svh-7rem)] flex-col gap-5 pb-10 lg:gap-6">
      {children}
    </div>
  )
}
