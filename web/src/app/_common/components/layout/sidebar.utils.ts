import { navItems } from "./sidebar.config";

export function getNestedCreateSubmenu(href: string, currentPath: string) {
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

export function getActiveGroupLabel(pathname: string) {
  const activeGroup = navItems.find((item) =>
    item.submenus?.some(
      (sub) => pathname === sub.href || pathname.startsWith(`${sub.href}/`),
    ),
  );

  return activeGroup?.label ?? null;
}
