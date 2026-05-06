import type { BadgeProps } from "@/components/ui/badge";

export type EmploymentStatusCode = "ACTIVE" | "LEAVE" | (string & {});

export function getEmploymentStatusLabel(
  status: EmploymentStatusCode | null | undefined,
) {
  if (!status) {
    return "-";
  }

  const labels: Record<string, string> = {
    ACTIVE: "재직",
    LEAVE: "휴직",
  };

  return labels[status] ?? status;
}

export function getEmploymentStatusBadgeVariant(
  status: EmploymentStatusCode | null | undefined,
): NonNullable<BadgeProps["variant"]> {
  if (status === "ACTIVE") {
    return "success";
  }

  if (status === "LEAVE") {
    return "warning";
  }

  return "outline";
}
