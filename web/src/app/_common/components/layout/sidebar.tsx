// 서버에서 렌더링 후 클라이언트에서 hydration
"use client";

// usePathname: 현재 URL 경로를 읽는 훅
// useRouter: 라우팅 제어 훅
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import {
  Bell,
  Building2,
  ChevronDown,
  Files,
  FolderOpen,
  LayoutDashboard,
  Tags,
  type LucideIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";

// exact란?
// 현재 URL이 해당 경로와 정확히 일치할 때만 active 상태로 표시할지 결정
type NavSubItem = {
  href: string;
  label: string;
  exact?: boolean;
};

type NavItem = {
  label: string;
  icon: LucideIcon;
  href?: string;
  exact?: boolean;
  submenus?: NavSubItem[];
};

// 업무 도메인 라우트를 좌측 전역 메뉴에서 바로 이동할 수 있게 정리합니다.
const navItems: NavItem[] = [
  {
    label: "대시보드",
    href: "/",
    icon: LayoutDashboard,
    exact: true,
  },
  {
    label: "업무일지",
    icon: Files,
    submenus: [
      { label: "업무일지 조회", href: "/worklog", exact: true },
      { label: "업무일지 등록", href: "/worklog/create" },
    ],
  },
  {
    label: "파일",
    href: "/file",
    icon: FolderOpen,
  },
  {
    label: "조직",
    icon: Building2,
    submenus: [
      { label: "부서 관리", href: "/department", exact: true },
      { label: "팀 관리", href: "/team", exact: true },
      { label: "사용자 관리", href: "/user", exact: true },
    ],
  },
  {
    label: "태그",
    href: "/tag",
    icon: Tags,
  },
  {
    label: "알림",
    href: "/notification",
    icon: Bell,
  },
];

//
export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const [openGroup, setOpenGroup] = useState<string | null>(null);

  // ` ` : 템플릿 리터럴 문법
  // 반드시 뒤에 / 붙이기
  useEffect(() => {
    const activeGroup = navItems.find((item) =>
      item.submenus?.some(
        (sub) => pathname === sub.href || pathname.startsWith(`${sub.href}/`),
      ),
    );

    setOpenGroup(activeGroup?.label ?? null);
  }, [pathname]);

  return (
    <aside className="dark workspace-sidebar relative z-20 hidden h-full w-[17.5rem] shrink-0 flex-col overflow-hidden border-r border-border/70 text-foreground md:flex">
      <div className="flex flex-1 flex-col overflow-y-auto px-3 py-6">
        <div className="flex flex-col gap-2">
          {navItems.map((item) => {
            if (item.submenus) {
              const shouldOpen = openGroup === item.label;

              return (
                <CollapsibleSidebarItem
                  key={item.label}
                  item={item}
                  isOpen={shouldOpen}
                  currentPath={pathname}
                  onToggle={() => {
                    const nextOpen = shouldOpen ? null : item.label;
                    setOpenGroup(nextOpen);

                    if (!shouldOpen && item.submenus?.[0]) {
                      router.push(item.submenus[0].href);
                    }
                  }}
                />
              );
            }

            const isActive = item.exact
              ? pathname === item.href
              : pathname === item.href || pathname.startsWith(`${item.href}/`);

            return (
              <Link
                key={item.href}
                href={item.href!}
                className={cn(
                  "group relative flex items-center gap-3 rounded-xl px-3 py-3 transition-all",
                  isActive
                    ? "bg-accent text-accent-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                )}
              >
                <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground transition-colors group-hover:text-foreground">
                  <item.icon className="size-4" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-[14px] font-semibold tracking-[-0.02em]">
                    {item.label}
                  </p>
                </div>
              </Link>
            );
          })}
        </div>

        <div className="mt-auto px-2 pb-2 pt-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-white/[0.03] p-5">
            <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-muted-foreground">
              workspace
            </p>
            <p className="mt-3 text-lg font-semibold text-foreground">AX-WMS</p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              프로필 카드와 권한별 메뉴는 인증 기능이 붙으면 이어서 확장하면
              됩니다.
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
}

// 해당 컴포넌트에서만 쓰이는 props는 인라인으로 정의
// 서브메뉴가 있는 접을 수 있는 사이드바 항목 컴포넌트
function CollapsibleSidebarItem({
  item,
  isOpen,
  currentPath,
  onToggle,
}: {
  item: NavItem;
  isOpen: boolean;
  currentPath: string;
  onToggle: () => void;
}) {
  // 현재 경로와 일치하는 서브메뉴 목록
  const matchingSubmenus =
    item.submenus?.filter(
      (sub) =>
        currentPath === sub.href || currentPath.startsWith(`${sub.href}/`),
    ) ?? [];

  // 현재 경로와 일치하는 서브메뉴가 하나라도 있으면 active 상태
  const isActiveGroup = matchingSubmenus.length > 0;

  // 일치하는 서브메뉴 중 가장 긴 경로의 길이를 구함
  // 현재 경로와 일치하는 서브메뉴가 여러 개일 때, 가장 긴 경로 항목을 active로 처리하기 위함
  const strongestMatchLength = matchingSubmenus.reduce(
    (max, sub) => Math.max(max, sub.href.length),
    0,
  );

  return (
    <div className="flex flex-col">
      <button
        type="button"
        onClick={onToggle}
        className={cn(
          "group relative flex items-center justify-between gap-3 rounded-xl px-3 py-3 transition-all",
          isActiveGroup
            ? "bg-accent text-accent-foreground"
            : isOpen
              ? "bg-muted text-foreground"
              : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
        )}
      >
        <div className="flex items-center gap-3">
          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground transition-colors group-hover:text-foreground">
            <item.icon className="size-4" />
          </div>
          <p className="truncate text-[14px] font-semibold tracking-[-0.02em]">
            {item.label}
          </p>
        </div>

        <ChevronDown
          className={cn(
            "size-4 text-muted-foreground transition-transform duration-300 ease-out",
            isOpen && "rotate-180",
          )}
        />
      </button>

      <div
        className={cn(
          "grid overflow-hidden transition-[grid-template-rows,opacity,margin] duration-300 ease-out",
          isOpen
            ? "mt-2 grid-rows-[1fr] opacity-100"
            : "mt-0 grid-rows-[0fr] opacity-0",
        )}
      >
        <div className="overflow-hidden">
          <div className="flex flex-col gap-1 px-3 pb-1 pl-[3.25rem] pt-0.5">
            {item.submenus?.map((sub) => {
              const nestedChild = getNestedCreateSubmenu(sub.href, currentPath);

              const isNestedChildActive = nestedChild
                ? currentPath === nestedChild.href ||
                  currentPath.startsWith(`${nestedChild.href}/`)
                : false;

              const isSubActive =
                !isNestedChildActive &&
                (currentPath === sub.href ||
                  currentPath.startsWith(`${sub.href}/`)) &&
                sub.href.length === strongestMatchLength;

              return (
                <div key={sub.href} className="flex flex-col gap-1">
                  <Link
                    href={sub.href}
                    className={cn(
                      "relative flex items-center rounded-md px-3 py-2 text-[13px] font-medium transition-all before:absolute before:-left-3 before:top-1/2 before:h-1 before:w-1 before:-translate-y-1/2 before:rounded-full",
                      isSubActive
                        ? "bg-accent text-accent-foreground before:bg-foreground"
                        : "text-muted-foreground hover:bg-accent hover:text-accent-foreground before:bg-transparent",
                    )}
                  >
                    {sub.label}
                  </Link>

                  {nestedChild ? (
                    <Link
                      href={nestedChild.href}
                      className={cn(
                        "relative ml-4 flex items-center rounded-md px-3 py-2 text-[12px] font-medium transition-all before:absolute before:-left-3 before:top-1/2 before:h-1 before:w-1 before:-translate-y-1/2 before:rounded-full",
                        isNestedChildActive
                          ? "bg-accent text-accent-foreground before:bg-foreground"
                          : "text-muted-foreground hover:bg-accent hover:text-accent-foreground before:bg-transparent",
                      )}
                    >
                      {nestedChild.label}
                    </Link>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

// 현재 경로가 등록 페이지일 때 동적으로 서뷰메뉴 항목을 추가해주는 함수
function getNestedCreateSubmenu(href: string, currentPath: string) {
  if (href === "/department" && currentPath.startsWith("/department/create")) {
    return { label: "부서 등록", href: "/department/create" };
  }

  if (href === "/team" && currentPath.startsWith("/team/create")) {
    return { label: "팀 등록", href: "/team/create" };
  }

  if (href === "/user" && currentPath.startsWith("/user/create")) {
    return { label: "사용자 등록", href: "/user/create" };
  }

  return null;
}
