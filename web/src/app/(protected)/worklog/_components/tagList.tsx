import { tags } from "../_mock/worklog.mock"
import { getTagSourceBadgeClass } from "../_utils/tagBadge"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

export function TagList({ tagIds }: { tagIds: number[] }) {
  const selectedTags = tags.filter((tag) => tagIds.includes(tag.id))

  if (selectedTags.length === 0) {
    return <p className="text-sm text-muted-foreground">연결된 태그가 없습니다.</p>
  }

  return (
    <div className="flex flex-wrap gap-2">
      {selectedTags.map((tag) => (
        <Badge
          key={tag.id}
          variant="outline"
          className={cn("rounded-full px-3 py-1.5", getTagSourceBadgeClass(tag.source))}
        >
          #{tag.name}
        </Badge>
      ))}
    </div>
  )
}
