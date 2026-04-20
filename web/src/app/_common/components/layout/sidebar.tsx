import Link from "next/link";

// 업무 도메인 라우트를 좌측 전역 메뉴에서 바로 이동할 수 있게 정리합니다.
const navigationItems = [
  { href: "/", label: "대시보드" },
  { href: "/department", label: "부서 관리" },
  { href: "/team", label: "팀 관리" },
  { href: "/user", label: "사용자 관리" },
  { href: "/worklog", label: "업무일지" },
  { href: "/search", label: "시맨틱 검색" },
  { href: "/file", label: "파일 관리" },
  { href: "/notification", label: "알림" },
  { href: "/tag", label: "태그 관리" },
];

export default function Sidebar() {
  return (
    // 데스크톱 기본 좌측 메뉴 뼈대를 먼저 만들어 두고 이후 메뉴 상태만 확장합니다.
    <aside className="workspace-sidebar hidden border-r border-sidebar-border/60 px-4 py-6 text-sidebar-foreground md:block">
      <div className="mb-8">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-sidebar-primary">
          navigation
        </p>
        <h2 className="mt-2 text-xl font-semibold">AX-WMS Web</h2>
      </div>
      <nav className="flex flex-col gap-2">
        {navigationItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className="rounded-2xl px-4 py-3 text-sm font-medium text-sidebar-foreground/80 transition hover:bg-sidebar-accent hover:text-sidebar-foreground"
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
