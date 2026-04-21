"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";

import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

import { type NavItem, navItems } from "./sidebar.config";
import { getActiveGroupLabel, getNestedCreateSubmenu } from "./sidebar.utils";

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const activeGroupLabel = getActiveGroupLabel(pathname);

  return (
    <aside className="dark workspace-sidebar relative z-20 hidden h-full w-[17.5rem] shrink-0 flex-col overflow-hidden border-r border-white/10 text-white md:flex">
      <div className="flex flex-1 flex-col overflow-y-auto px-3 py-6">
        <div className="flex flex-col gap-2">
          {navItems.map((item) => {
            if (item.submenus) {
              const shouldOpen = activeGroupLabel === item.label;
              return (
                <CollapsibleSidebarItem
                  key={item.label}
                  item={item}
                  isOpen={shouldOpen}
                  currentPath={pathname}
                  onToggle={() => {
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
                    ? "bg-white/10 text-white"
                    : "text-white/68 hover:bg-white/8 hover:text-white",
                )}
              >
                <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-white/6 text-white/58 transition-colors group-hover:text-white">
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
            <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-white/62">
              workspace
            </p>
            <p className="mt-3 text-lg font-semibold text-white">AX-WMS</p>
            <p className="mt-2 text-sm leading-6 text-white/62">
              프로필 카드와 권한별 메뉴는 인증 기능이 붙으면 이어서 확장하면
              됩니다.
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
}

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
        className={cn(
          "group relative flex items-center justify-between gap-3 rounded-xl px-3 py-3 transition-all",
          isActiveGroup
            ? "bg-white/10 text-white"
            : isOpen
              ? "bg-white/8 text-white"
              : "text-white/68 hover:bg-white/8 hover:text-white",
        )}
      >
        <div className="flex items-center gap-3">
          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-white/6 text-white/58 transition-colors group-hover:text-white">
            <item.icon className="size-4" />
          </div>
          <p className="truncate text-[14px] font-semibold tracking-[-0.02em]">
            {item.label}
          </p>
        </div>

        <ChevronDown
          className={cn(
            "size-4 text-white/56 transition-transform duration-300 ease-out",
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
                        ? "bg-white/10 text-white before:bg-white"
                        : "text-white/64 hover:bg-white/8 hover:text-white before:bg-transparent",
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
                          : "text-white/64 hover:bg-white/8 hover:text-white before:bg-transparent",
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
