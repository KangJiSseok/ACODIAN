"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { useAuth } from "@/app/_common/hooks/useAuth";
import TeamForm from "../../_components/teamForm";
import { useTeamDetail, useTeamUsers, useUpdateTeam } from "../../_hooks";
import type { CreateTeamRequest, UpdateTeamRequest } from "../../_types/team.types";
import { canManageTeam } from "../../_utils/teamAccess.utils";

export default function TeamEditPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const teamId = Number(params.id);
  const { user } = useAuth();
  const { data: team, isLoading, error } = useTeamDetail(teamId);
  const { data: users, isLoading: isUsersLoading } = useTeamUsers(teamId);
  const updateTeam = useUpdateTeam(teamId);
  const canEditTeam = team ? canManageTeam(user, team) : false;

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

      {team && !canEditTeam ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          팀 수정은 해당 팀 관리자만 사용할 수 있습니다.{" "}
          <Link href={`/team/detail/${team.teamId}`} className="font-semibold underline underline-offset-4">
            팀 상세로 돌아가기
          </Link>
        </div>
      ) : null}

      {team && users && canEditTeam ? (
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
