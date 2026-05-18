import {
  Bell,
  Building2,
  Files,
  FolderOpen,
  LayoutDashboard,
  Tags,
  type LucideIcon,
} from "lucide-react";

export type NavSubItem = {
  href: string;
  label: string;
  exact?: boolean;
};

export type NavItem = {
  label: string;
  icon: LucideIcon;
  href?: string;
  exact?: boolean;
  submenus?: NavSubItem[];
};

export const navItems: NavItem[] = [
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
    icon: Tags,
    submenus: [
      { label: "태그 목록", href: "/tag", exact: true },
      { label: "태그 병합", href: "/tag/merge" },
    ],
  },
  {
    label: "알림",
    href: "/notification",
    icon: Bell,
  },
];

export function findNavItemByHref(href: string) {
  return navItems.find((item) => item.href === href);
}

export function findNavSubItemByHref(href: string) {
  for (const item of navItems) {
    const submenu = item.submenus?.find((sub) => sub.href === href);
    if (submenu) {
      return { item, submenu };
    }
  }

  return null;
}
