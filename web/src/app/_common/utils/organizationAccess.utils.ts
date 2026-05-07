import type { NavItem } from "@/app/_common/components/layout/sidebar.config";
import type { AuthUser } from "@/app/_common/store/auth.store";

const DIRECTOR_KEYWORD = "본부장";
const DEPARTMENT_HEAD_KEYWORD = "사업부장";

function profileTexts(user: AuthUser | null | undefined) {
  return [user?.titleName, user?.positionName].filter(Boolean) as string[];
}

export function isDirectorProfile(user: AuthUser | null | undefined) {
  return profileTexts(user).some((text) => text.includes(DIRECTOR_KEYWORD));
}

export function isDepartmentHeadProfile(user: AuthUser | null | undefined) {
  return profileTexts(user).some((text) =>
    text.includes(DEPARTMENT_HEAD_KEYWORD),
  );
}

export function canManageDepartments(user: AuthUser | null | undefined) {
  return isDirectorProfile(user);
}

export function canManageUsers(user: AuthUser | null | undefined) {
  return isDirectorProfile(user) || isDepartmentHeadProfile(user);
}

export function canCreateTeams(user: AuthUser | null | undefined) {
  return isDirectorProfile(user) || isDepartmentHeadProfile(user);
}

export function canViewTeams(user: AuthUser | null | undefined) {
  return Boolean(user);
}

export function canAccessOrganizationPath(
  user: AuthUser | null | undefined,
  href: string,
) {
  if (href.startsWith("/department")) {
    return canManageDepartments(user);
  }

  if (href.startsWith("/user")) {
    return canManageUsers(user);
  }

  if (href.startsWith("/team")) {
    return canViewTeams(user);
  }

  return true;
}

export function filterNavItemsForUser(
  user: AuthUser | null | undefined,
  items: NavItem[],
) {
  return items
    .map((item) => {
      if (!item.submenus) {
        return item;
      }

      const submenus = item.submenus.filter((submenu) => {
        return canAccessOrganizationPath(user, submenu.href);
      });

      return submenus.length > 0 ? { ...item, submenus } : null;
    })
    .filter((item): item is NavItem => item !== null);
}
