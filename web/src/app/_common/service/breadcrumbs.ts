import {
  findNavItemByHref,
  findNavSubItemByHref,
} from "@/app/_common/components/layout/sidebar.config";

export type BreadcrumbItem = {
  label: string;
  href?: string;
};

function resolveExactBreadcrumbs(pathname: string): BreadcrumbItem[] | null {
  if (pathname === "/") {
    return [{ label: "대시보드" }];
  }

  if (pathname === "/my-page") {
    return [{ label: "마이페이지" }];
  }

  const submenuMatch = findNavSubItemByHref(pathname);
  if (submenuMatch) {
    return [
      { label: submenuMatch.item.label },
      { label: submenuMatch.submenu.label, href: submenuMatch.submenu.href },
    ];
  }

  const itemMatch = findNavItemByHref(pathname);
  if (itemMatch) {
    return [{ label: itemMatch.label }];
  }

  return null;
}

function resolveNestedBreadcrumbs(
  basePath: string,
  trailingLabel: string,
): BreadcrumbItem[] | null {
  const exactBreadcrumbs = resolveExactBreadcrumbs(basePath);
  if (!exactBreadcrumbs) {
    return null;
  }

  return [...exactBreadcrumbs, { label: trailingLabel }];
}

export function resolveBreadcrumbs(pathname: string): BreadcrumbItem[] {
  const exactMatch = resolveExactBreadcrumbs(pathname);
  if (exactMatch) {
    return exactMatch;
  }

  const nestedRules = [
    {
      predicate: pathname.startsWith("/department/create"),
      basePath: "/department",
      label: "부서 등록",
    },
    {
      predicate: pathname.startsWith("/team/create"),
      basePath: "/team",
      label: "팀 등록",
    },
    {
      predicate: pathname.startsWith("/user/create"),
      basePath: "/user",
      label: "사용자 등록",
    },
    {
      predicate: pathname.startsWith("/worklog/detail/"),
      basePath: "/worklog",
      label: "업무일지 상세",
    },
    {
      predicate: pathname.startsWith("/worklog/edit/"),
      basePath: "/worklog",
      label: "업무일지 수정",
    },
    {
      predicate: pathname.startsWith("/team/detail/"),
      basePath: "/team",
      label: "팀 상세",
    },
    {
      predicate: pathname.startsWith("/team/edit/"),
      basePath: "/team",
      label: "팀 수정",
    },
    {
      predicate: pathname.startsWith("/user/detail/"),
      basePath: "/user",
      label: "사용자 상세",
    },
    {
      predicate: pathname.startsWith("/user/edit/"),
      basePath: "/user",
      label: "사용자 수정",
    },
    {
      predicate: pathname.startsWith("/department/edit/"),
      basePath: "/department",
      label: "부서 수정",
    },
  ];

  for (const rule of nestedRules) {
    if (!rule.predicate) {
      continue;
    }

    const breadcrumb = resolveNestedBreadcrumbs(
      rule.basePath,
      rule.label,
    );

    if (breadcrumb) {
      return breadcrumb;
    }
  }

  return [{ label: "AX-WMS" }];
}
