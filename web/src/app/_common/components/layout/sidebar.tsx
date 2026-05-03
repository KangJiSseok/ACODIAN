"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";

import { ChevronDown, LogOut, PanelLeftClose } from "lucide-react";
import { cn } from "@/lib/utils";

import { type NavItem, navItems } from "./sidebar.config";
import { getActiveGroupLabel, getNestedCreateSubmenu } from "./sidebar.utils";
import { useAuth } from "@/app/_common/hooks/useAuth";
import { useUiStore } from "@/app/_common/store/ui.store";

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const activeGroupLabel = getActiveGroupLabel(pathname);
  const { user, logout } = useAuth();
  const sidebarCollapsed = useUiStore((state) => state.sidebarCollapsed);
  const toggleSidebarCollapsed = useUiStore(
    (state) => state.toggleSidebarCollapsed,
  );

  const displayName = user?.userName ?? "사용자";
  const displayTitle = user?.titleName ?? user?.positionName ?? "프로필";
  const profileImageUrl = user?.profileImageUrl;
  const profileInitial = displayName.slice(0, 1);

  async function handleLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <aside
      className={cn(
        "dark workspace-sidebar relative z-20 hidden h-screen shrink-0 flex-col overflow-hidden border-r border-white/10 text-white transition-[width,min-width,max-width,flex-basis] duration-300 ease-out md:flex",
        sidebarCollapsed
          ? "w-[4.75rem] min-w-[4.75rem] max-w-[4.75rem] basis-[4.75rem]"
          : "w-[17.5rem] min-w-[17.5rem] max-w-[17.5rem] basis-[17.5rem]",
      )}
    >
      <div
        className="flex w-[17.5rem] flex-1 flex-col overflow-y-auto px-2 py-5"
      >
        <div
          className={cn(
            "mb-5 grid h-10 shrink-0 items-center transition-all duration-300 ease-out",
            sidebarCollapsed
              ? "w-[3.75rem] grid-cols-[3.75rem]"
              : "w-full grid-cols-[3.75rem_minmax(0,1fr)_2.25rem]",
          )}
        >
          {sidebarCollapsed ? (
            <button
              type="button"
              onClick={toggleSidebarCollapsed}
              className="flex h-10 w-[3.75rem] shrink-0 items-center justify-center"
              aria-label="사이드바 펼치기"
              aria-expanded={false}
              title="사이드바 펼치기"
            >
              <span className="flex size-9 items-center justify-center text-[16px] font-black tracking-[0.02em] text-white transition-colors hover:text-white/80">
                IB
              </span>
            </button>
          ) : (
            <Link
              href="/"
              className="flex h-10 w-[3.75rem] items-center justify-center"
              title="IBANK"
              aria-label="IBANK"
            >
              <span className="flex size-9 items-center justify-center text-[16px] font-black tracking-[0.02em] text-white transition-colors hover:text-white/80">
                IB
              </span>
            </Link>
          )}

          {!sidebarCollapsed ? (
            <button
              type="button"
              onClick={toggleSidebarCollapsed}
              className="col-start-3 flex size-9 shrink-0 items-center justify-center justify-self-end rounded-xl text-white/62 transition-colors hover:bg-white/8 hover:text-white"
              aria-label="사이드바 접기"
              aria-expanded
              title="사이드바 접기"
            >
              <PanelLeftClose className="size-5" />
            </button>
          ) : null}
        </div>

        <div
          className={cn(
            "flex flex-col gap-2",
            sidebarCollapsed ? "w-[3.75rem]" : "w-full",
          )}
        >
          {navItems.map((item) => {
            if (item.submenus) {
              const shouldOpen = activeGroupLabel === item.label;
              return (
                <CollapsibleSidebarItem
                  key={item.label}
                  item={item}
                  isOpen={shouldOpen}
                  isCollapsed={sidebarCollapsed}
                  currentPath={pathname}
                  onToggle={() => {
                    if ((sidebarCollapsed || !shouldOpen) && item.submenus?.[0]) {
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
                title={sidebarCollapsed ? item.label : undefined}
                aria-label={sidebarCollapsed ? item.label : undefined}
                className={cn(
                  "group relative grid h-12 items-center overflow-hidden rounded-xl transition-[background-color,color] duration-200",
                  sidebarCollapsed
                    ? "w-[3.75rem] grid-cols-[3.75rem]"
                    : "w-full grid-cols-[3.75rem_minmax(0,1fr)] text-left",
                  isActive
                    ? "bg-white/10 text-white"
                    : "text-white hover:bg-white/8",
                )}
              >
                <div className="flex w-[3.75rem] justify-center">
                  <div className="flex size-8 items-center justify-center text-current transition-colors">
                    <item.icon className="size-4" />
                  </div>
                </div>
                {!sidebarCollapsed ? (
                  <div className="min-w-0 overflow-hidden text-left">
                    <p className="truncate text-[14px] font-semibold tracking-[-0.02em]">
                      {item.label}
                    </p>
                  </div>
                ) : null}
              </Link>
            );
          })}
        </div>

        <div className="mt-auto">
          <div className="relative grid w-full grid-cols-[3.75rem_minmax(0,1fr)] items-center">
            <Link
              href="/my-page"
              className={cn(
                "col-span-2 grid h-14 w-full min-w-0 grid-cols-[3.75rem_minmax(0,1fr)] items-center rounded-xl text-white transition-colors hover:bg-white/8",
                sidebarCollapsed
                  ? "pointer-events-auto"
                  : "pr-12",
              )}
              title={sidebarCollapsed ? displayName : undefined}
              aria-label={sidebarCollapsed ? displayName : undefined}
            >
              <div className="flex w-[3.75rem] justify-center">
                <div
                  className="flex size-10 items-center justify-center overflow-hidden rounded-full bg-white/10 text-sm font-bold text-white"
                  style={
                    profileImageUrl
                      ? {
                          backgroundImage: `url(${profileImageUrl})`,
                          backgroundPosition: "center",
                          backgroundSize: "cover",
                        }
                      : undefined
                  }
                >
                  {profileImageUrl ? null : profileInitial}
                </div>
              </div>

              {!sidebarCollapsed ? (
                <div className="min-w-0 overflow-hidden">
                  <p className="truncate text-[14px] font-bold leading-tight text-white">
                    {displayName}
                  </p>
                  <p className="mt-1 truncate text-[12px] font-semibold text-white/55">
                    {displayTitle}
                  </p>
                </div>
              ) : null}
            </Link>

            {!sidebarCollapsed ? (
              <button
                type="button"
                onClick={handleLogout}
                className="absolute right-0 top-1/2 flex size-10 -translate-y-1/2 shrink-0 items-center justify-center rounded-xl text-rose-300/80 transition-colors hover:bg-white/8 hover:text-rose-200"
                aria-label="로그아웃"
                title="로그아웃"
              >
                <LogOut className="size-5" />
              </button>
            ) : null}
          </div>
        </div>
      </div>
    </aside>
  );
}

function CollapsibleSidebarItem({
  item,
  isOpen,
  isCollapsed,
  currentPath,
  onToggle,
}: {
  item: NavItem;
  isOpen: boolean;
  isCollapsed: boolean;
  currentPath: string;
  onToggle: () => void;
}) {
  const matchingSubmenus =
    item.submenus?.filter(
      (sub) =>
        currentPath === sub.href || currentPath.startsWith(`${sub.href}/`),
    ) ?? [];

  const isActiveGroup = matchingSubmenus.length > 0;
  const strongestMatchLength = matchingSubmenus.reduce(
    (max, sub) => Math.max(max, sub.href.length),
    0,
  );

  return (
    <div className="flex flex-col">
      <button
        type="button"
        onClick={onToggle}
        title={isCollapsed ? item.label : undefined}
        aria-label={isCollapsed ? item.label : undefined}
        className={cn(
          "group relative grid h-12 items-center overflow-hidden rounded-xl transition-[background-color,color] duration-200",
          isCollapsed
            ? "w-[3.75rem] grid-cols-[3.75rem]"
            : "w-full grid-cols-[3.75rem_minmax(0,1fr)_2.25rem] text-left",
          isActiveGroup
            ? "bg-white/10 text-white"
            : isOpen
              ? "bg-white/8 text-white"
              : "text-white hover:bg-white/8",
        )}
      >
        <div className="flex w-[3.75rem] justify-center">
          <div className="flex size-8 items-center justify-center text-current transition-colors">
            <item.icon className="size-4" />
          </div>
        </div>
        {!isCollapsed ? (
          <div className="min-w-0 overflow-hidden text-left">
            <p className="truncate text-[14px] font-semibold tracking-[-0.02em]">
              {item.label}
            </p>
          </div>
        ) : null}

        {!isCollapsed ? (
          <ChevronDown
            className={cn(
              "size-4 justify-self-center text-current opacity-70 transition-transform duration-300 ease-out",
              isOpen && "rotate-180",
            )}
          />
        ) : null}
      </button>

      {!isCollapsed ? (
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
                const nestedChild = getNestedCreateSubmenu(
                  sub.href,
                  currentPath,
                );

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
                          ? "bg-white/10 text-white before:bg-white"
                          : "text-white hover:bg-white/8 before:bg-transparent",
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
                            ? "bg-white/10 text-white before:bg-white"
                            : "text-white hover:bg-white/8 before:bg-transparent",
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
      ) : null}
    </div>
  );
}
