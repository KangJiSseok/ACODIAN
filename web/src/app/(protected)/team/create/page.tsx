"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import { useAuth } from "@/app/_common/hooks/useAuth";
import { canCreateTeams } from "@/app/_common/utils/organizationAccess.utils";
import TeamForm from "../_components/teamForm";
import { useCreateTeam } from "../_hooks";
import type { CreateTeamRequest, UpdateTeamRequest } from "../_types/team.types";

export default function TeamCreatePage() {
  const router = useRouter();
  const { user } = useAuth();
  const createTeam = useCreateTeam();
  const canCreateTeam = canCreateTeams(user);

  async function handleSubmit(payload: CreateTeamRequest | UpdateTeamRequest) {
    await createTeam.mutateAsync(payload as CreateTeamRequest);
    router.push("/team");
  }

  if (!user) {
    return (
      <section className="space-y-6">
        <PageHeader
          title="팀 등록"
          description="로그인 사용자 정보를 확인하는 중입니다."
        />
      </section>
    );
  }

  if (!canCreateTeam) {
    return (
      <section className="space-y-6">
        <PageHeader
          title="팀 등록"
          description="팀 등록은 본부장과 사업부장만 사용할 수 있습니다."
        />
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          팀장과 팀원은 팀 정보를 조회할 수 있습니다.{" "}
          <Link href="/team" className="font-semibold underline underline-offset-4">
            팀 목록으로 돌아가기
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="space-y-6">
      <PageHeader
        title="팀 등록"
        description="팀의 운영 정보와 구성원을 한 번에 등록합니다."
      />
      <TeamForm
        submitLabel="팀 생성"
        isSubmitting={createTeam.isPending}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
