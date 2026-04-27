import { HelpCircle } from "lucide-react"
import { TagManager } from "./_components/tagManager"
import { Button } from "@/components/ui/button"

export default function TagPage() {
  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
          태그 현황
        </h1>
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="size-10 rounded-xl"
          aria-label="태그 아이콘 안내"
        >
          <HelpCircle className="size-4" />
        </Button>
      </div>

      <TagManager />
    </section>
  )
}
