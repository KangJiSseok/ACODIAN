"use client";

import { useParams } from "next/navigation";
import PageHeader from "@/app/_common/components/layout/pageHeader";
import UserDetail from "../../_components/userDetail";
import {
  useUserDetail,
  useUserEvaluations,
  useUserSkills,
} from "../../_hooks";

export default function UserDetailPage() {
  const params = useParams<{ id: string }>();
  const userId = Number(params.id);
  const { data: user, isLoading, error } = useUserDetail(userId);
  const { data: skills, isLoading: isSkillsLoading } = useUserSkills(userId);
  const { data: evaluations, isLoading: isEvaluationsLoading } =
    useUserEvaluations(userId);

  return (
    <section className="space-y-6">
      <PageHeader
        title={user?.userName ?? "사용자 상세"}
        description="프로필, 스킬, 관리자 평가를 탭으로 확인합니다."
      />

      {isLoading ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          사용자 정보를 불러오는 중입니다.
        </div>
      ) : null}

      {error ? (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          사용자 정보를 불러오지 못했습니다.
        </div>
      ) : null}

      {!isLoading && !error && !user ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          사용자를 찾을 수 없습니다.
        </div>
      ) : null}

      {user ? (
        <UserDetail
          user={user}
          skills={skills?.skills ?? []}
          evaluations={evaluations?.items ?? []}
          isSkillsLoading={isSkillsLoading}
          isEvaluationsLoading={isEvaluationsLoading}
        />
      ) : null}
    </section>
  );
}
