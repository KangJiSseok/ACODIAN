"use client";

import { useRouter } from "next/navigation";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import TeamForm from "../_components/teamForm";
import { useCreateTeam } from "../_hooks";
import type { CreateTeamRequest, UpdateTeamRequest } from "../_types/team.types";

export default function TeamCreatePage() {
  const router = useRouter();
  const createTeam = useCreateTeam();

  async function handleSubmit(payload: CreateTeamRequest | UpdateTeamRequest) {
    await createTeam.mutateAsync(payload as CreateTeamRequest);
    router.push("/team");
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
