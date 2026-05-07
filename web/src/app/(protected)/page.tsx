"use client";

import Link from "next/link";
import { useMemo } from "react";
import ScaffoldPage from "@/app/_common/components/layout/scaffoldPage";
import { useAuth } from "@/app/_common/hooks/useAuth";
import { canAccessOrganizationPath } from "@/app/_common/utils/organizationAccess.utils";

// 초기 스캐폴드 단계에서 주요 도메인으로 이동할 수 있는 진입 링크를 둡니다.
const dashboardLinks = [
  { href: "/department", label: "부서 관리" },
  { href: "/team", label: "팀 관리" },
  { href: "/user", label: "사용자 관리" },
  { href: "/worklog", label: "업무일지" },
  { href: "/search", label: "시맨틱 검색" },
];

export default function DashboardPage() {
  const { user } = useAuth();
  const visibleDashboardLinks = useMemo(
    () =>
      dashboardLinks.filter((item) =>
        canAccessOrganizationPath(user, item.href),
      ),
    [user],
  );

  return (
    <ScaffoldPage
      title="AX-WMS 대시보드"
      description="도메인별 라우트와 기본 레이아웃이 연결된 상태입니다."
    >
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
    </ScaffoldPage>
  );
}
