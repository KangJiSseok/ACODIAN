"use client";

import { useParams } from "next/navigation";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import TeamDetail from "../../_components/teamDetail";
import { useTeamDetail, useTeamUsers } from "../../_hooks";

export default function TeamDetailPage() {
  const params = useParams<{ id: string }>();
  const teamId = Number(params.id);
  const { data: team, isLoading, error } = useTeamDetail(teamId);
  const { data: users } = useTeamUsers(teamId);

  return (
    <section className="space-y-6">
      <PageHeader
        title={team?.teamName ?? "팀 상세"}
        description="팀 멤버와 운영 정보를 확인합니다."
      />

      {isLoading ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          팀 정보를 불러오는 중입니다.
        </div>
      ) : null}

      {error ? (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          팀 정보를 불러오지 못했습니다.
        </div>
      ) : null}

      {team ? <TeamDetail team={team} users={users?.items ?? []} /> : null}
    </section>
  );
}
