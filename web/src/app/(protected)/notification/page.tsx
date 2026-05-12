"use client";

import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import { ChevronDown, RefreshCw, SlidersHorizontal } from "lucide-react";
import { Pagination } from "@/app/_common/components/data-display/pagination";
import { useAuth } from "@/app/_common/hooks/useAuth";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import type { AuthUser } from "@/app/_common/store/auth.store";
import { isDirectorProfile } from "@/app/_common/utils/organizationAccess.utils";
import { Button } from "@/components/ui/button";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { useDepartmentDetail, useDepartmentList } from "../department/_hooks";
import { useTeamList } from "../team/_hooks";
import { NotificationList } from "./_components/notificationList";
import {
  useNotificationList,
  useNotificationMutation,
} from "./_hooks";
import type {
  GetNotificationsParams,
  NotificationView,
} from "./_types/notification.types";

const ALL_FILTER_VALUE = "all";
const NOTIFICATION_PAGE_SIZE = 6;

export default function NotificationPage() {
  const { user } = useAuth();
  const isDirector = isDirectorProfile(user);
  const [activeView, setActiveView] = useState<NotificationView>("TOTAL");
  const [showFilters, setShowFilters] = useState(false);
  const [departmentId, setDepartmentId] = useState(ALL_FILTER_VALUE);
  const [teamId, setTeamId] = useState(ALL_FILTER_VALUE);
  const [page, setPage] = useState(1);
  const { markAllRead, markRead, isMarkingAllRead } = useNotificationMutation();

  const selectedDepartmentId = parseFilterNumber(departmentId);
  const selectedTeamId = parseFilterNumber(teamId);
  const readFilter = resolveReadFilter(activeView);

  const listParams = useMemo<GetNotificationsParams>(
    () => ({
      page,
      pageSize: NOTIFICATION_PAGE_SIZE,
      isRead: readFilter,
      departmentId: selectedDepartmentId,
      teamId: selectedTeamId,
    }),
    [page, readFilter, selectedDepartmentId, selectedTeamId],
  );
  const countBaseParams = useMemo<GetNotificationsParams>(
    () => ({
      page: 1,
      pageSize: 1,
      departmentId: selectedDepartmentId,
      teamId: selectedTeamId,
    }),
    [selectedDepartmentId, selectedTeamId],
  );

  const {
    notificationPage,
    notifications,
    isLoading,
    error,
    refetch,
  } = useNotificationList(listParams);
  const totalCountQuery = useNotificationList(countBaseParams);
  const unreadCountQuery = useNotificationList({
    ...countBaseParams,
    isRead: false,
  });
  const readCountQuery = useNotificationList({
    ...countBaseParams,
    isRead: true,
  });
  const { data: departmentData } = useDepartmentList(isDirector);
  const { data: teamPage } = useTeamList({ pageSize: 100 }, isDirector);
  const { data: selectedDepartment } = useDepartmentDetail(
    isDirector ? selectedDepartmentId ?? Number.NaN : Number.NaN,
  );

  const departments = isDirector
    ? departmentData?.departments ?? []
    : getUserDepartmentOptions(user);
  const teamOptionsSource = isDirector
    ? selectedDepartmentId
      ? selectedDepartment?.teams ?? []
      : teamPage?.items ?? []
    : getUserTeamOptions(user);
  const totalCount = totalCountQuery.notificationPage?.totalCount ?? 0;
  const unreadCount = unreadCountQuery.notificationPage?.totalCount ?? 0;
  const readCount = readCountQuery.notificationPage?.totalCount ?? 0;

  function updateView(nextView: NotificationView) {
    setActiveView(nextView);
    setPage(1);
  }

  function updateDepartment(nextDepartmentId: string) {
    setDepartmentId(nextDepartmentId);
    setTeamId(ALL_FILTER_VALUE);
    setPage(1);
  }

  function updateTeam(nextTeamId: string) {
    setTeamId(nextTeamId);
    setPage(1);
  }

  function resetFilters() {
    setDepartmentId(ALL_FILTER_VALUE);
    setTeamId(ALL_FILTER_VALUE);
    setPage(1);
  }

  return (
    <section className="space-y-6">
      <PageHeader title="알림" description="업무 마감 알림과 읽음 상태를 확인합니다." />

      <section className="space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            알림 현황
          </h2>
          <Button
            type="button"
            variant="outline"
            className="h-10 rounded-2xl px-4 text-sm"
            onClick={markAllRead}
            disabled={unreadCount === 0 || isMarkingAllRead}
          >
            전체 읽음 처리
          </Button>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <SummaryCard
            label="전체 알림"
            value={totalCount}
            active={activeView === "TOTAL"}
            onClick={() => updateView("TOTAL")}
          />
          <SummaryCard
            label="읽지 않은 알림"
            value={unreadCount}
            active={activeView === "UNREAD"}
            onClick={() => updateView("UNREAD")}
          />
          <SummaryCard
            label="읽은 알림"
            value={readCount}
            active={activeView === "READ"}
            onClick={() => updateView("READ")}
          />
        </div>
      </section>

      <section className="space-y-4">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            알림 탐색
          </h2>
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
                  Notification Filters
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

              <div className="grid gap-4 md:grid-cols-2">
                <FilterField label="부서">
                  <Select
                    aria-label="부서 필터"
                    value={departmentId}
                    onChange={(event) => updateDepartment(event.target.value)}
                    options={[
                      { label: "전체 부서", value: ALL_FILTER_VALUE },
                      ...departments.map((department) => ({
                        label: department.departmentName,
                        value: String(department.departmentId),
                      })),
                    ]}
                  />
                </FilterField>
                <FilterField label="팀">
                  <Select
                    aria-label="팀 필터"
                    value={teamId}
                    onChange={(event) => updateTeam(event.target.value)}
                    options={[
                      { label: "전체 팀", value: ALL_FILTER_VALUE },
                      ...teamOptionsSource.map((team) => ({
                        label: team.teamName,
                        value: String(team.teamId),
                      })),
                    ]}
                  />
                </FilterField>
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
                알림 목록
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                알림을 선택하면 관련 업무 화면으로 이동합니다.
              </p>
            </div>
            <p className="text-sm font-medium text-muted-foreground">
              표시 중인 알림{" "}
              <span className="text-lg font-semibold text-foreground">
                {notificationPage?.totalCount ?? 0}건
              </span>
            </p>
          </div>
        </div>

        {isLoading ? (
          <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
            알림 목록을 불러오는 중입니다.
          </div>
        ) : null}

        {error ? (
          <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
            알림 목록을 불러오지 못했습니다.
            <button
              type="button"
              className="ml-3 font-semibold underline underline-offset-4"
              onClick={() => void refetch()}
            >
              다시 시도
            </button>
          </div>
        ) : null}

        {!isLoading && !error ? (
          <>
            <NotificationList notifications={notifications} onMarkRead={markRead} />
            <Pagination
              page={notificationPage?.page ?? page}
              totalPages={notificationPage?.totalPages ?? 1}
              onPageChange={setPage}
            />
          </>
        ) : null}
      </section>
    </section>
  );
}

function SummaryCard({
  label,
  value,
  active,
  onClick,
}: {
  label: string;
  value: number;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button type="button" className="text-left" onClick={onClick}>
      <CardSpotlight
        className={cn(
          "rounded-[24px] p-5 transition-all duration-200",
          active && "ring-2 ring-primary/70",
        )}
      >
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
          {label}
        </p>
        <p className="mt-2 text-2xl font-semibold text-foreground">{value}</p>
      </CardSpotlight>
    </button>
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

function getUserDepartmentOptions(user: AuthUser | null | undefined) {
  if (!Number.isFinite(user?.departmentId)) {
    return [];
  }

  return [
    {
      departmentId: user?.departmentId as number,
      departmentName: user?.departmentName ?? "내 부서",
    },
  ];
}

function getUserTeamOptions(user: AuthUser | null | undefined) {
  return user?.teams ?? [];
}

function parseFilterNumber(value: string) {
  if (value === ALL_FILTER_VALUE) {
    return undefined;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function resolveReadFilter(view: NotificationView) {
  if (view === "UNREAD") return false;
  if (view === "READ") return true;
  return undefined;
}
