"use client";

import { useParams, useRouter } from "next/navigation";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import TeamForm from "../../_components/teamForm";
import { useTeamDetail, useTeamUsers, useUpdateTeam } from "../../_hooks";
import type { CreateTeamRequest, UpdateTeamRequest } from "../../_types/team.types";

export default function TeamEditPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const teamId = Number(params.id);
  const { data: team, isLoading, error } = useTeamDetail(teamId);
  const { data: users, isLoading: isUsersLoading } = useTeamUsers(teamId);
  const updateTeam = useUpdateTeam(teamId);

  async function handleSubmit(payload: CreateTeamRequest | UpdateTeamRequest) {
    await updateTeam.mutateAsync(payload as UpdateTeamRequest);
    router.push("/team");
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title={team ? `${team.teamName} 수정` : "팀 수정"}
        description="팀 기본 정보, 관리자, 팀원 역할을 수정합니다."
      />

      {isLoading || isUsersLoading ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          팀 정보를 불러오는 중입니다.
        </div>
      ) : null}

      {error ? (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          팀 정보를 불러오지 못했습니다.
        </div>
      ) : null}

      {team && users ? (
        <TeamForm
          initialTeam={team}
          initialUsers={users.items}
          submitLabel="수정 저장"
          isSubmitting={updateTeam.isPending}
          onSubmit={handleSubmit}
        />
      ) : null}
    </section>
  );
}
