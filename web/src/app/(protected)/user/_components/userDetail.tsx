"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import {
  ArrowLeft,
  CalendarDays,
  ChevronDown,
  ImageUp,
  Pencil,
  Plus,
  Trash2,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import type {
  UserDetail as UserDetailType,
  UserEvaluationSummary,
  UserSkillSummary,
} from "../_types/user.types";
import {
  getEmploymentStatusBadgeVariant,
  getEmploymentStatusLabel,
} from "../_utils/userStatus.utils";

type UserDetailTab = "profile" | "skills" | "evaluations";

const tabs: Array<{ value: UserDetailTab; label: string }> = [
  { value: "profile", label: "사용자 정보" },
  { value: "skills", label: "스킬 설정" },
  { value: "evaluations", label: "관리자 평가" },
];

export default function UserDetail({
  user,
  skills,
  evaluations,
  isSkillsLoading = false,
  isEvaluationsLoading = false,
}: {
  user: UserDetailType;
  skills: UserSkillSummary[];
  evaluations: UserEvaluationSummary[];
  isSkillsLoading?: boolean;
  isEvaluationsLoading?: boolean;
}) {
  const [activeTab, setActiveTab] = useState<UserDetailTab>("profile");
  const primaryTeam = useMemo(
    () => user.teams.find((team) => team.isPrimary) ?? null,
    [user.teams],
  );
  const otherTeamNames = useMemo(
    () =>
      user.teams
        .filter((team) => !team.isPrimary)
        .map((team) => team.teamName)
        .join(", "),
    [user.teams],
  );

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button asChild variant="outline" className="h-10">
          <Link href="/user">
            <ArrowLeft className="size-4" />
            사용자 목록
          </Link>
        </Button>
        <div className="flex flex-wrap justify-end gap-3">
          <Button
            type="button"
            variant="default"
            disabled
            title="사용자 수정 기능은 다음 단계에서 연결합니다."
            className="h-10 min-w-28 !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
          >
            <Pencil className="size-4" />
            수정
          </Button>
          <Button
            type="button"
            variant="destructive"
            disabled
            title="사용자 삭제 기능은 현재 API 계약에 없습니다."
            className="h-10 min-w-28 !text-destructive hover:!text-destructive [&_svg]:!text-destructive"
          >
            <Trash2 className="size-4" />
            삭제
          </Button>
        </div>
      </div>

      <Card className="rounded-[28px]">
        <CardContent className="space-y-8 p-6 sm:p-8">
          <div className="space-y-2">
            <h2 className="text-lg font-semibold text-foreground">
              사용자 정보 및 직책 설정
            </h2>
            <p className="text-sm leading-6 text-muted-foreground">
              사용자 기본 정보, 스킬 조정, 관리자 평가를 하나의 탭 영역에서
              관리합니다.
            </p>
          </div>

          <div
            role="tablist"
            aria-label="사용자 상세 탭"
            className="inline-flex flex-wrap rounded-2xl bg-muted/45 p-1"
          >
            {tabs.map((tab) => (
              <button
                key={tab.value}
                type="button"
                role="tab"
                aria-selected={activeTab === tab.value}
                className={cn(
                  "h-12 min-w-32 rounded-2xl px-5 text-sm font-semibold text-muted-foreground transition-all",
                  activeTab === tab.value &&
                    "bg-background text-foreground shadow-sm ring-1 ring-border/80",
                )}
                onClick={() => setActiveTab(tab.value)}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {activeTab === "profile" ? (
            <ProfileTab
              user={user}
              primaryTeamName={primaryTeam?.teamName ?? "-"}
              otherTeamNames={otherTeamNames || "-"}
            />
          ) : null}
          {activeTab === "skills" ? (
            <SkillsTab skills={skills} isLoading={isSkillsLoading} />
          ) : null}
          {activeTab === "evaluations" ? (
            <EvaluationsTab
              evaluations={evaluations}
              isLoading={isEvaluationsLoading}
            />
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function ProfileTab({
  user,
  primaryTeamName,
  otherTeamNames,
}: {
  user: UserDetailType;
  primaryTeamName: string;
  otherTeamNames: string;
}) {
  return (
    <section className="space-y-8">
      <div className="space-y-4">
        <p className="text-sm font-semibold text-muted-foreground">기본 정보</p>
        <div className="grid gap-4 xl:grid-cols-3">
          <ReadOnlyField label="이름" value={user.userName} />
          <ReadOnlyField label="이메일" value={user.email} />
          <ProfileImageField user={user} />
        </div>
      </div>

      <div className="border-t border-border/70 pt-8">
        <p className="mb-4 text-sm font-semibold text-muted-foreground">
          조직 배치
        </p>
        <div className="grid gap-4 xl:grid-cols-3">
          <ReadOnlyField
            label="소속 부서"
            value={user.departmentName ?? "-"}
            selectLike
          />
          <ReadOnlyField
            label="직급"
            value={user.positionName ?? "-"}
            selectLike
          />
          <ReadOnlyField
            label="직책"
            value={user.titleName ?? "-"}
            selectLike
          />
          <ReadOnlyField
            label="주소속팀"
            value={primaryTeamName}
            selectLike
          />
          <ReadOnlyField
            label="다른 소속 팀"
            value={otherTeamNames}
            selectLike
          />
        </div>
      </div>

      <div className="border-t border-border/70 pt-8">
        <p className="mb-4 text-sm font-semibold text-muted-foreground">
          인사 정보
        </p>
        <div className="grid gap-4 xl:grid-cols-3">
          <ReadOnlyField label="연락처" value={user.phone ?? "-"} />
          <StatusField status={user.employmentStatus} />
          <ReadOnlyField
            label="입사일"
            value={formatDateInput(user.joinDate)}
            trailing={<CalendarDays className="size-4 text-foreground" />}
          />
        </div>
      </div>

      <div className="flex justify-end">
        <Button
          type="button"
          disabled
          className="h-12 min-w-36 rounded-2xl !text-primary-foreground hover:!text-primary-foreground"
        >
          수정 저장
        </Button>
      </div>
    </section>
  );
}

function SkillsTab({
  skills,
  isLoading,
}: {
  skills: UserSkillSummary[];
  isLoading: boolean;
}) {
  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-4">
          <div className="space-y-2">
            <h3 className="text-base font-semibold text-foreground">
              스킬 설정
            </h3>
            <p className="text-sm leading-6 text-muted-foreground">
              본부장/사업부장은 스킬을 추가하고 Lv.1~5로 조정할 수 있습니다.
            </p>
          </div>
          <p className="text-sm leading-6 text-muted-foreground">
            스킬별 숙련도를 1~5레벨로 확인합니다.
          </p>
        </div>
        <Button
          type="button"
          disabled
          className="h-11 min-w-32 rounded-2xl !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
        >
          <Plus className="size-4" />
          스킬 추가
        </Button>
      </div>

      {isLoading ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          스킬 정보를 불러오는 중입니다.
        </div>
      ) : null}

      {!isLoading && skills.length === 0 ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          등록된 스킬이 없습니다.
        </div>
      ) : null}

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {skills.map((skill) => (
          <div
            key={skill.skillId}
            className="flex min-h-20 items-center justify-between gap-4 rounded-2xl border border-border/80 bg-background/70 px-5 py-4"
          >
            <div className="flex min-w-0 items-center gap-3">
              <p className="truncate text-base font-semibold text-foreground">
                {skill.skillName}
              </p>
              <Badge variant="default">Lv.{skill.skillLevel}</Badge>
            </div>
            <Button type="button" variant="outline" disabled className="h-10">
              수정
            </Button>
          </div>
        ))}
      </div>
    </section>
  );
}

function EvaluationsTab({
  evaluations,
  isLoading,
}: {
  evaluations: UserEvaluationSummary[];
  isLoading: boolean;
}) {
  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-2">
          <h3 className="text-base font-semibold text-foreground">
            관리자 평가
          </h3>
          <p className="text-sm leading-6 text-muted-foreground">
            관리자 전용 평가 메모를 확인합니다.
          </p>
        </div>
        <Button
          type="button"
          disabled
          className="h-11 min-w-32 rounded-2xl !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
        >
          <Pencil className="size-4" />
          평가 작성
        </Button>
      </div>

      {isLoading ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          평가 정보를 불러오는 중입니다.
        </div>
      ) : null}

      {!isLoading && evaluations.length === 0 ? (
        <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
          등록된 평가가 없습니다.
        </div>
      ) : null}

      <div className="grid gap-3">
        {evaluations.map((evaluation) => (
          <article
            key={evaluation.evaluationId}
            className="rounded-2xl border border-border/80 bg-background/70 px-5 py-4"
          >
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="font-semibold text-foreground">
                {evaluation.evaluatorUserName}
              </p>
              <span className="text-xs font-medium text-muted-foreground">
                {formatDateTime(evaluation.createdAt)}
              </span>
            </div>
            <p className="mt-3 text-sm leading-6 text-muted-foreground">
              {evaluation.content}
            </p>
          </article>
        ))}
      </div>
    </section>
  );
}

function ReadOnlyField({
  label,
  value,
  selectLike = false,
  trailing,
}: {
  label: string;
  value: string;
  selectLike?: boolean;
  trailing?: ReactNode;
}) {
  return (
    <label className="space-y-2">
      <span className="text-sm font-semibold text-foreground">{label}</span>
      <span className="flex h-14 w-full items-center justify-between gap-3 rounded-2xl border border-input bg-input/80 px-4 text-sm font-medium text-foreground shadow-sm">
        <span className="truncate">{value}</span>
        {selectLike ? (
          <ChevronDown className="size-4 shrink-0 text-foreground" />
        ) : (
          trailing
        )}
      </span>
    </label>
  );
}

function ProfileImageField({ user }: { user: UserDetailType }) {
  return (
    <div className="space-y-2">
      <p className="text-sm font-semibold text-foreground">프로필 이미지</p>
      <div className="flex h-14 items-center gap-3 rounded-2xl border border-input bg-input/80 px-4 shadow-sm">
        <Avatar className="size-10">
          {user.profileImageUrl ? (
            <AvatarImage src={user.profileImageUrl} alt={user.userName} />
          ) : null}
          <AvatarFallback>{user.userName.slice(0, 1)}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-foreground">
            이미지 선택
          </p>
          <p className="truncate text-xs text-muted-foreground">
            선택된 파일 없음
          </p>
        </div>
        <ImageUp className="size-4 shrink-0 text-primary" />
      </div>
    </div>
  );
}

function StatusField({ status }: { status: UserDetailType["employmentStatus"] }) {
  return (
    <div className="space-y-2">
      <p className="text-sm font-semibold text-foreground">상태</p>
      <div className="flex h-14 items-center justify-between gap-3 rounded-2xl border border-input bg-input/80 px-4 shadow-sm">
        <Badge variant={getEmploymentStatusBadgeVariant(status)}>
          {getEmploymentStatusLabel(status)}
        </Badge>
        <ChevronDown className="size-4 shrink-0 text-foreground" />
      </div>
    </div>
  );
}

function formatDateInput(value: string | null | undefined) {
  if (!value) {
    return "-";
  }

  return value.slice(0, 10);
}

function formatDateTime(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}
