import { cn } from "@/lib/utils";

interface ResultCountProps {
  label: string;
  count: number;
  unit: string;
  className?: string;
}

export function ResultCount({
  label,
  count,
  unit,
  className,
}: ResultCountProps) {
  return (
    <p className={cn("text-sm font-medium text-muted-foreground", className)}>
      {label}{" "}
      <span className="ml-1 font-semibold text-foreground">
        {count.toLocaleString("ko-KR")}
        {unit}
      </span>
    </p>
  );
}
