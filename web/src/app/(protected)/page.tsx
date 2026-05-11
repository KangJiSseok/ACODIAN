"use client";

import ScaffoldPage from "@/app/_common/components/layout/scaffoldPage";
import { useAuth } from "@/app/_common/hooks/useAuth";
import { isDirectorProfile } from "@/app/_common/utils/organizationAccess.utils";
import {
  DirectorDashboardError,
  DirectorDashboardLoading,
  DirectorDashboardView,
} from "./_dashboard/_components/directorDashboard";
import { DashboardShortcutGrid } from "./_dashboard/_components/dashboardShortcutGrid";
import { useDirectorDashboard } from "./_dashboard/_hooks/useDirectorDashboard";

export default function DashboardPage() {
  const { user } = useAuth();
  const isDirector = isDirectorProfile(user);
  const {
    data: directorDashboard,
    isLoading,
    error,
  } = useDirectorDashboard(isDirector);

  return (
    <ScaffoldPage
      title={isDirector ? "본부장 대시보드" : "AX-WMS 대시보드"}
      description={
        isDirector
          ? "전사 업무 진행률과 부서별 workload를 확인합니다."
          : "도메인별 라우트와 기본 레이아웃이 연결된 상태입니다."
      }
    >
      {!isDirector ? <DashboardShortcutGrid user={user} /> : null}

      {isDirector && isLoading ? <DirectorDashboardLoading /> : null}
      {isDirector && error ? <DirectorDashboardError /> : null}
      {isDirector && directorDashboard ? (
        <DirectorDashboardView dashboard={directorDashboard} />
      ) : null}
    </ScaffoldPage>
  );
}
