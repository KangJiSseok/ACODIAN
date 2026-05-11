import Link from "next/link";
import { useMemo } from "react";
import type { AuthUser } from "@/app/_common/store/auth.store";
import { canAccessOrganizationPath } from "@/app/_common/utils/organizationAccess.utils";

const dashboardLinks = [
  { href: "/department", label: "부서 관리" },
  { href: "/team", label: "팀 관리" },
  { href: "/user", label: "사용자 관리" },
  { href: "/worklog", label: "업무일지" },
  { href: "/search", label: "시맨틱 검색" },
];

export function DashboardShortcutGrid({
  user,
}: {
  user: AuthUser | null | undefined;
}) {
  const visibleDashboardLinks = useMemo(
    () =>
      dashboardLinks.filter((item) =>
        canAccessOrganizationPath(user, item.href),
      ),
    [user],
  );

  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {visibleDashboardLinks.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          className="workspace-panel-soft rounded-2xl p-5 transition hover:-translate-y-0.5 hover:border-primary/40"
        >
          <p className="text-sm text-muted-foreground">바로가기</p>
          <h2 className="mt-2 text-lg font-semibold">{item.label}</h2>
        </Link>
      ))}
    </div>
  );
}
