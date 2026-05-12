"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import {
  BriefcaseBusiness,
  ChevronDown,
  type LucideIcon,
  Mail,
  Phone,
  RefreshCw,
  Search,
  SlidersHorizontal,
} from "lucide-react";
import { Pagination } from "@/app/_common/components/data-display/pagination";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { selectAuthUser, useAuthStore } from "@/app/_common/store/auth.store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { useUserList } from "./_hooks";
import type { GetUsersParams, UserSummary } from "./_types/user.types";
import {
  getEmploymentStatusBadgeVariant,
  getEmploymentStatusLabel,
} from "./_utils/userStatus.utils";
import {
  canUseUserDepartmentFilter,
  resolveUserListDepartmentId,
} from "./_utils/userAccess.utils";

const ALL_FILTER_VALUE = "all";
const USER_PAGE_SIZE = 4;

export default function UserPage() {
  const currentUser = useAuthStore(selectAuthUser);
  const [query, setQuery] = useState("");
  const [showFilters, setShowFilters] = useState(false);
  const [departmentId, setDepartmentId] = useState(ALL_FILTER_VALUE);
  const [positionName, setPositionName] = useState(ALL_FILTER_VALUE);
  const [employmentStatus, setEmploymentStatus] = useState(ALL_FILTER_VALUE);
  const [page, setPage] = useState(1);
  const canUseDepartmentFilter = canUseUserDepartmentFilter(currentUser);
  const scopedDepartmentId = resolveUserListDepartmentId(
    currentUser,
    departmentId,
    ALL_FILTER_VALUE,
  );

  const userListParams = useMemo<GetUsersParams>(
    () => ({
      page,
      pageSize: USER_PAGE_SIZE,
      userName: query.trim() || undefined,
      departmentId: scopedDepartmentId,
      positionName:
        positionName === ALL_FILTER_VALUE ? undefined : positionName,
      employmentStatus:
        employmentStatus === ALL_FILTER_VALUE ? undefined : employmentStatus,
    }),
    [employmentStatus, page, positionName, query, scopedDepartmentId],
  );
  const filterListParams = useMemo<GetUsersParams>(
    () => ({
      pageSize: 100,
      departmentId: resolveUserListDepartmentId(
        currentUser,
        ALL_FILTER_VALUE,
        ALL_FILTER_VALUE,
      ),
    }),
    [currentUser],
  );

  const {
    data: userPage,
    isLoading,
    error,
    refetch,
  } = useUserList(userListParams);
  const { data: filterUserPage } = useUserList(filterListParams);
  const users = useMemo(
    () => dedupeUsersById(userPage?.items ?? []),
    [userPage?.items],
  );
  const filterSourceUsers = useMemo(
    () => dedupeUsersById(filterUserPage?.items ?? users),
    [filterUserPage?.items, users],
  );

  const filterOptions = useMemo(
    () => buildFilterOptions(filterSourceUsers),
    [filterSourceUsers],
  );

  function resetFilters() {
    setQuery("");
    setDepartmentId(ALL_FILTER_VALUE);
    setPositionName(ALL_FILTER_VALUE);
    setEmploymentStatus(ALL_FILTER_VALUE);
    setPage(1);
  }

  function updateQuery(value: string) {
    setQuery(value);
    setPage(1);
  }

  function updateDepartmentId(value: string) {
    setDepartmentId(value);
    setPage(1);
  }

  function updatePositionName(value: string) {
    setPositionName(value);
    setPage(1);
  }

  function updateEmploymentStatus(value: string) {
    setEmploymentStatus(value);
    setPage(1);
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="사용자 관리"
        description="조직 구성원의 소속, 직급, 재직 상태를 확인합니다."
      />

      <section className="space-y-4">
        <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
          사용자 탐색
        </h2>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={query}
                onChange={(event) => updateQuery(event.target.value)}
                placeholder="이름으로 검색하세요"
                className="h-12 pl-11"
                aria-label="사용자 검색"
              />
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                className="h-10"
                type="button"
                onClick={() => setShowFilters((prev) => !prev)}
              >
                <SlidersHorizontal className="size-4" />
                필터
                <ChevronDown
                  className={cn(
                    "ml-1 size-4 transition-transform duration-300 ease-out",
                    showFilters && "rotate-180",
                  )}
                />
              </Button>
            </div>
          </div>

          <div
            className={cn(
              "grid overflow-hidden transition-[grid-template-rows,opacity,margin] duration-300 ease-out",
              showFilters
                ? "mt-0 grid-rows-[1fr] opacity-100"
                : "mt-[-4px] grid-rows-[0fr] opacity-0",
            )}
          >
            <div className="overflow-hidden">
              <div className="space-y-4 pt-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                    User Filters
                  </p>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-9 w-9"
                    type="button"
                    onClick={resetFilters}
                    aria-label="필터 초기화"
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                </div>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {canUseDepartmentFilter ? (
                    <FilterField label="부서">
                      <Select
                        aria-label="부서 필터"
                        value={departmentId}
                        onChange={(event) =>
                          updateDepartmentId(event.target.value)
                        }
                        options={[
                          { label: "전체 부서", value: ALL_FILTER_VALUE },
                          ...filterOptions.departments,
                        ]}
                      />
                    </FilterField>
                  ) : null}
                  <FilterField label="직급">
                    <Select
                      aria-label="직급 필터"
                      value={positionName}
                      onChange={(event) =>
                        updatePositionName(event.target.value)
                      }
                      options={[
                        { label: "전체 직급", value: ALL_FILTER_VALUE },
                        ...filterOptions.positions,
                      ]}
                    />
                  </FilterField>
                  <FilterField label="재직 상태">
                    <Select
                      aria-label="재직 상태 필터"
                      value={employmentStatus}
                      onChange={(event) =>
                        updateEmploymentStatus(event.target.value)
                      }
                      options={[
                        { label: "전체 상태", value: ALL_FILTER_VALUE },
                        ...filterOptions.statuses,
                      ]}
                    />
                  </FilterField>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="space-y-4">
        <div className="border-t-2 border-foreground/70 pt-5">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
                사용자 목록
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                사용자 카드를 선택하면 상세 페이지로 이동합니다.
              </p>
            </div>
            <p className="text-sm font-medium text-muted-foreground">
              표시 중인 사용자 {userPage?.totalCount ?? 0}명
            </p>
          </div>
        </div>

        {isLoading ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            사용자 목록을 불러오는 중입니다.
          </div>
        ) : null}

        {error ? (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
            사용자 목록을 불러오지 못했습니다.
            <button
              type="button"
              className="ml-3 font-semibold underline underline-offset-4"
              onClick={() => void refetch()}
            >
              다시 시도
            </button>
          </div>
        ) : null}

        {!isLoading && !error && users.length === 0 ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            조건에 맞는 사용자가 없습니다.
          </div>
        ) : null}

        <div className="grid gap-4">
          {users.map((user) => (
            <UserCard key={user.userId} user={user} />
          ))}
        </div>

        <Pagination
          page={userPage?.page ?? page}
          totalPages={userPage?.totalPages ?? 1}
          onPageChange={setPage}
        />
      </section>
    </section>
  );
}

function UserCard({ user }: { user: UserSummary }) {
  const rankLabel = [user.positionName, user.titleName]
    .filter(Boolean)
    .join(" · ");

  return (
    <Link
      href={`/user/detail/${user.userId}`}
      className="block rounded-[26px] outline-none transition-transform focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
    >
      <CardSpotlight className="cursor-pointer rounded-[26px] transition-all duration-300 hover:-translate-y-1">
        <CardContent className="flex flex-col gap-5 p-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex min-w-0 flex-1 gap-4">
            <Avatar user={user} />

            <div className="min-w-0 flex-1 space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="truncate text-lg font-semibold tracking-[-0.04em] text-foreground transition-colors group-hover/card-spotlight:text-primary">
                  {user.userName}
                </h3>
                {user.titleName ? (
                  <Badge variant="default">{user.titleName}</Badge>
                ) : null}
                <Badge
                  variant={getEmploymentStatusBadgeVariant(user.employmentStatus)}
                >
                  {getEmploymentStatusLabel(user.employmentStatus)}
                </Badge>
              </div>

              <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
                <InlineInfo icon={Mail} value={user.email} />
                <InlineInfo icon={Phone} value={user.phone ?? "-"} />
                <InlineInfo icon={BriefcaseBusiness} value={rankLabel || "-"} />
              </div>

              <div className="flex flex-wrap gap-x-5 gap-y-2 text-sm">
                <Meta label="부서" value={user.departmentName ?? "-"} />
                <Meta label="주 소속 팀" value={user.teamName ?? "-"} />
              </div>
            </div>
          </div>

          <div className="flex shrink-0 justify-end border-t border-border/60 pt-4 lg:w-40 lg:border-t-0 lg:pt-0">
            <div className="text-right">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                입사일
              </p>
              <p className="mt-2 text-sm font-semibold text-foreground">
                {formatDate(user.joinDate)}
              </p>
            </div>
          </div>
        </CardContent>
      </CardSpotlight>
    </Link>
  );
}

function Avatar({ user }: { user: UserSummary }) {
  return (
    <div
      className={cn(
        "flex size-13 shrink-0 items-center justify-center overflow-hidden rounded-full border border-border/70 bg-primary/10 text-base font-bold text-primary",
        user.profileImageUrl && "bg-cover bg-center",
      )}
      style={
        user.profileImageUrl
          ? { backgroundImage: `url(${user.profileImageUrl})` }
          : undefined
      }
    >
      {user.profileImageUrl ? null : user.userName.slice(0, 1)}
    </div>
  );
}

function InlineInfo({
  icon: Icon,
  value,
}: {
  icon: LucideIcon;
  value: string;
}) {
  return (
    <span className="inline-flex min-w-0 items-center gap-1.5">
      <Icon className="size-3.5 shrink-0" />
      <span className="truncate">{value}</span>
    </span>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className="text-xs font-semibold text-muted-foreground">
        {label}
      </span>
      <span className="font-semibold text-foreground">{value}</span>
    </span>
  );
}

function FilterField({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-2">
      <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      {children}
    </div>
  );
}

function dedupeUsersById(users: UserSummary[]) {
  const seenUserIds = new Set<number>();

  return users.filter((user) => {
    if (seenUserIds.has(user.userId)) {
      return false;
    }

    seenUserIds.add(user.userId);
    return true;
  });
}

function buildFilterOptions(users: UserSummary[]) {
  return {
    departments: uniqueOptions(
      users
        .filter((user) => user.departmentId && user.departmentName)
        .map((user) => ({
          label: user.departmentName as string,
          value: String(user.departmentId),
        })),
    ),
    positions: uniqueOptions(
      users
        .filter((user) => user.positionName)
        .map((user) => ({
          label: user.positionName as string,
          value: user.positionName as string,
        })),
    ),
    statuses: uniqueOptions(
      users.map((user) => ({
        label: getEmploymentStatusLabel(user.employmentStatus),
        value: user.employmentStatus,
      })),
    ),
  };
}

function uniqueOptions(options: Array<{ label: string; value: string }>) {
  const seen = new Set<string>();

  return options.filter((option) => {
    if (seen.has(option.value)) {
      return false;
    }

    seen.add(option.value);
    return true;
  });
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value));
}
