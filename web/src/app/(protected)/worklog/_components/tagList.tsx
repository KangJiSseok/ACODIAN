import { Badge } from "@/components/ui/badge"

export function TagList({
  tagNames,
}: {
  tagNames?: string[]
}) {
  if (!tagNames || tagNames.length === 0) {
    return <p className="text-sm text-muted-foreground">연결된 태그가 없습니다.</p>
  }

  return (
    <div className="flex flex-wrap gap-2">
      {tagNames.map((tagName) => (
        <Badge
          key={tagName}
          variant="outline"
          className="rounded-full px-3 py-1.5"
        >
          #{tagName}
        </Badge>
      ))}
    </div>
  )
}
