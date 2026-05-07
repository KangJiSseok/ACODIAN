"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import {
  BriefcaseBusiness,
  ChevronDown,
  ChevronUp,
  type LucideIcon,
  Mail,
  Phone,
  RefreshCw,
  Search,
  SlidersHorizontal,
} from "lucide-react";
import { Pagination } from "@/app/_common/components/data-display/pagination";
import { usePagination } from "@/app/_common/hooks/usePagination";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { useUserList } from "./_hooks";
import type { UserSummary } from "./_types/user.types";
import {
  getEmploymentStatusBadgeVariant,
  getEmploymentStatusLabel,
} from "./_utils/userStatus.utils";

const ALL_FILTER_VALUE = "all";
const USER_PAGE_SIZE = 4;

export default function UserPage() {
  const { data: users = [], isLoading, error, refetch } = useUserList();
  const [query, setQuery] = useState("");
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [departmentId, setDepartmentId] = useState(ALL_FILTER_VALUE);
  const [positionName, setPositionName] = useState(ALL_FILTER_VALUE);
  const [employmentStatus, setEmploymentStatus] = useState(ALL_FILTER_VALUE);

  const filterOptions = useMemo(() => buildFilterOptions(users), [users]);
  const filteredUsers = useMemo(
    () =>
      filterUsers(users, {
        query,
        departmentId,
        positionName,
        employmentStatus,
      }),
    [departmentId, employmentStatus, positionName, query, users],
  );
  const pagination = usePagination(filteredUsers, USER_PAGE_SIZE);

  function resetFilters() {
    setQuery("");
    setDepartmentId(ALL_FILTER_VALUE);
    setPositionName(ALL_FILTER_VALUE);
    setEmploymentStatus(ALL_FILTER_VALUE);
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="사용자 관리"
        description="조직 구성원의 소속, 직급, 재직 상태를 확인합니다."
      />

      <section className="space-y-5">
        <h2 className="text-[17px] font-semibold tracking-[-0.03em] text-foreground">
          사용자 탐색
        </h2>

        <div className="flex flex-col gap-3 lg:flex-row">
          <div className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="이름, 이메일, 부서, 직급으로 검색하세요"
              className="h-14 rounded-2xl pl-12 text-base"
            />
          </div>
          <Button
            type="button"
            variant="secondary"
            className="h-14 min-w-32 justify-center rounded-2xl px-6 text-sm font-semibold"
            onClick={() => setIsFilterOpen((current) => !current)}
          >
            <SlidersHorizontal className="size-4" />
            필터
            {isFilterOpen ? (
              <ChevronUp className="size-4" />
            ) : (
              <ChevronDown className="size-4" />
            )}
          </Button>
        </div>

        {isFilterOpen ? (
          <div className="space-y-5 pt-5">
            <div className="flex items-center justify-between gap-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-muted-foreground">
                User Filters
              </p>
              <button
                type="button"
                className="inline-flex size-9 items-center justify-center rounded-xl text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
                onClick={resetFilters}
                aria-label="필터 초기화"
              >
                <RefreshCw className="size-4" />
              </button>
            </div>

            <div className="grid gap-4 lg:grid-cols-3">
              <FilterField label="부서">
                <Select
                  value={departmentId}
                  onChange={(event) => setDepartmentId(event.target.value)}
                  options={[
                    { label: "전체 부서", value: ALL_FILTER_VALUE },
                    ...filterOptions.departments,
                  ]}
                  className="h-14 rounded-2xl text-base font-semibold"
                />
              </FilterField>
              <FilterField label="직급">
                <Select
                  value={positionName}
                  onChange={(event) => setPositionName(event.target.value)}
                  options={[
                    { label: "전체 직급", value: ALL_FILTER_VALUE },
                    ...filterOptions.positions,
                  ]}
                  className="h-14 rounded-2xl text-base font-semibold"
                />
              </FilterField>
              <FilterField label="재직 상태">
                <Select
                  value={employmentStatus}
                  onChange={(event) => setEmploymentStatus(event.target.value)}
                  options={[
                    { label: "전체 상태", value: ALL_FILTER_VALUE },
                    ...filterOptions.statuses,
                  ]}
                  className="h-14 rounded-2xl text-base font-semibold"
                />
              </FilterField>
            </div>
          </div>
        ) : null}
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
              표시 중인 사용자{" "}
              <span className="text-lg font-semibold text-foreground">
                {filteredUsers.length}명
              </span>
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

        {!isLoading && !error && filteredUsers.length === 0 ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            조건에 맞는 사용자가 없습니다.
          </div>
        ) : null}

        <div className="grid gap-4">
          {pagination.items.map((user) => (
            <UserCard key={user.userId} user={user} />
          ))}
        </div>

        <Pagination
          page={pagination.page}
          totalPages={pagination.totalPages}
          onPageChange={pagination.setPage}
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

          <div className="flex shrink-0 items-center justify-between gap-4 border-t border-border/60 pt-4 lg:w-52 lg:flex-col lg:items-end lg:border-t-0 lg:pt-0">
            <div className="text-right">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                입사일
              </p>
              <p className="mt-2 text-sm font-semibold text-foreground">
                {formatDate(user.joinDate)}
              </p>
            </div>
            <span className="inline-flex h-11 min-w-28 items-center justify-center rounded-2xl bg-primary px-5 text-sm font-semibold text-primary-foreground transition-colors group-hover/card-spotlight:bg-primary/90">
              상세 보기
            </span>
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
    <label className="space-y-2">
      <span className="text-sm font-semibold text-muted-foreground">
        {label}
      </span>
      {children}
    </label>
  );
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

function filterUsers(
  users: UserSummary[],
  filters: {
    query: string;
    departmentId: string;
    positionName: string;
    employmentStatus: string;
  },
) {
  const normalizedQuery = filters.query.trim().toLowerCase();

  return users.filter((user) => {
    const queryMatches =
      !normalizedQuery ||
      [
        user.userName,
        user.email,
        user.departmentName,
        user.positionName,
        user.titleName,
        user.teamName,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(normalizedQuery);
    const departmentMatches =
      filters.departmentId === ALL_FILTER_VALUE ||
      String(user.departmentId) === filters.departmentId;
    const positionMatches =
      filters.positionName === ALL_FILTER_VALUE ||
      user.positionName === filters.positionName;
    const statusMatches =
      filters.employmentStatus === ALL_FILTER_VALUE ||
      user.employmentStatus === filters.employmentStatus;

    return (
      queryMatches && departmentMatches && positionMatches && statusMatches
    );
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
