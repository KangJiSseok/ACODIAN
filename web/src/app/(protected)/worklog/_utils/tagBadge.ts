export function getTagSourceBadgeClass(source: "AI" | "MANUAL") {
  return source === "AI"
    ? "border-primary/25 bg-primary/10 text-primary shadow-sm"
    : "border-secondary bg-secondary text-secondary-foreground shadow-sm"
}
