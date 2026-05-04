"use client";

import {
  BriefcaseBusiness,
  CalendarDays,
  CheckCircle2,
  Image as ImageIcon,
  Mail,
  Phone,
  ShieldCheck,
  UserRound,
  UsersRound,
} from "lucide-react";
import { useAuth } from "@/app/_common/hooks/useAuth";
import { cn } from "@/lib/utils";

// 백엔드 EmploymentStatus enum 값을 화면용 한글 라벨로 변환한다.
const employmentStatusLabels: Record<string, string> = {
  ACTIVE: "재직",
  LEAVE: "휴직",
  RETIRED: "퇴사",
};

function formatDate(value: string | null | undefined) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(date);
}

function getInitial(name: string | undefined) {
  return name?.trim().slice(0, 1) || "?";
}

export default function MyPage() {
  const { user } = useAuth();

  // /users/me 응답의 팀 목록에서 대표 소속 팀과 전체 소속 팀 라벨을 만든다.
  const primaryTeam = user?.teams.find((team) => team.isPrimary);
  const teamNames = user?.teams.map((team) => team.teamName).join(", ") || "-";
  const displayStatus = user?.employmentStatus
    ? (employmentStatusLabels[user.employmentStatus] ?? user.employmentStatus)
    : "-";
  const roleLabel = [user?.positionName, user?.titleName]
    .filter(Boolean)
    .join(" · ");

  return (
    <section className="mx-auto max-w-[92rem]">
      <div className="workspace-panel rounded-3xl p-5 shadow-[0_26px_80px_-46px_rgba(15,23,42,0.45)] md:p-8">
        <div className="mb-8 flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.34em] text-muted-foreground">
              나의 정보
            </p>
            <h1 className="mt-2 text-xl font-black tracking-[-0.04em] text-foreground">
              프로필 정보 및 계정 설정
            </h1>
          </div>
          <div className="flex size-11 items-center justify-center rounded-2xl border border-primary/15 bg-primary/5 text-primary">
            <UserRound className="size-5" />
          </div>
        </div>

        <div className="grid gap-8 xl:grid-cols-[minmax(0,1.35fr)_minmax(22rem,0.85fr)]">
          <div className="space-y-8">
            <section className="workspace-panel-soft rounded-3xl p-5 md:p-6">
              <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
                <div
                  className="flex size-24 shrink-0 items-center justify-center overflow-hidden rounded-full bg-primary/10 text-3xl font-black text-primary ring-1 ring-primary/15"
                  style={
                    user?.profileImageUrl
                      ? {
                          backgroundImage: `url(${user.profileImageUrl})`,
                          backgroundPosition: "center",
                          backgroundSize: "cover",
                        }
                      : undefined
                  }
                >
                  {user?.profileImageUrl ? null : getInitial(user?.userName)}
                </div>

                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="truncate text-2xl font-black tracking-[-0.04em] text-foreground">
                      {user?.userName ?? "-"}
                    </h2>
                    {user?.titleName ? (
                      <span className="rounded-full border border-primary/20 bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
                        {user.titleName}
                      </span>
                    ) : null}
                    <span className="rounded-full border border-emerald-400/30 bg-emerald-400/10 px-2.5 py-1 text-xs font-bold text-emerald-600">
                      {displayStatus}
                    </span>
                  </div>
                  <p className="mt-3 truncate text-sm font-semibold text-muted-foreground">
                    {user?.email ?? "-"}
                  </p>
                  <p className="mt-3 text-sm font-bold text-foreground">
                    주 소속 팀{" "}
                    <span className="text-primary">
                      {primaryTeam?.teamName ?? "-"}
                    </span>
                  </p>
                </div>
              </div>
            </section>

            <section className="space-y-4">
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-muted-foreground">
                직접 수정 가능
              </p>
              <ReadOnlyField
                icon={ImageIcon}
                label="프로필 이미지 URL"
                value={user?.profileImageUrl ?? "-"}
              />
              <div className="grid gap-4 md:grid-cols-2">
                <ReadOnlyField
                  icon={Phone}
                  label="연락처"
                  value={user?.phone ?? "-"}
                />
                <ReadOnlyField
                  icon={UsersRound}
                  label="주 소속 팀"
                  value={primaryTeam?.teamName ?? "-"}
                />
              </div>
            </section>

            <section className="border-t border-border pt-8">
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-muted-foreground">
                비밀번호 변경
              </p>
              {/* 비밀번호는 서버에서 내려받지 않으므로 변경 API 연결 전까지 마스킹 UI만 표시한다. */}
              {/* 입력 필드 연결 시 새 비밀번호와 확인 값의 일치 여부를 검증한다. */}
              <div className="mt-4 grid gap-4 md:grid-cols-2">
                <ReadOnlyField label="현재 비밀번호" value="••••••••" />
                <ReadOnlyField label="새 비밀번호" value="••••••••" />
                <ReadOnlyField label="새 비밀번호 확인" value="••••••••" />
              </div>
            </section>
          </div>

          <aside className="border-border xl:border-l xl:pl-8">
            <p className="mb-5 text-xs font-bold uppercase tracking-[0.22em] text-muted-foreground">
              조회 전용 인사 정보
            </p>
            <div className="space-y-3">
              <InfoCard icon={Mail} label="이메일" value={user?.email ?? "-"} />
              <InfoCard
                icon={CalendarDays}
                label="입사일"
                value={formatDate(user?.joinDate)}
              />
              <InfoCard
                icon={BriefcaseBusiness}
                label="소속 부서"
                value={user?.departmentName ?? "-"}
              />
              <InfoCard
                icon={UsersRound}
                label="주 소속 팀"
                value={primaryTeam?.teamName ?? "-"}
              />
              <InfoCard
                icon={UsersRound}
                label="소속 팀 목록"
                value={teamNames}
              />
              <InfoCard
                icon={ShieldCheck}
                label="직급 / 직책"
                value={roleLabel || "-"}
              />
              <InfoCard
                icon={CheckCircle2}
                label="상태"
                value={displayStatus}
              />
            </div>
          </aside>
        </div>
      </div>
    </section>
  );
}

function ReadOnlyField({
  icon: Icon,
  label,
  value,
}: {
  icon?: typeof UserRound;
  label: string;
  value: string;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-foreground">
        {label}
      </span>
      <span className="flex h-14 items-center gap-3 rounded-2xl border border-border bg-input px-4 text-sm font-semibold text-foreground shadow-[0_12px_30px_-22px_rgba(15,23,42,0.48)]">
        {Icon ? (
          <Icon className="size-4 shrink-0 text-muted-foreground" />
        ) : null}
        <span className="truncate">{value}</span>
      </span>
    </label>
  );
}

function InfoCard({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof UserRound;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-[0_12px_28px_-24px_rgba(15,23,42,0.45)]">
      <div className="flex items-center gap-2 text-xs font-bold text-muted-foreground">
        <Icon className="size-4" />
        <span>{label}</span>
      </div>
      <p
        className={cn(
          "mt-3 break-words text-sm font-bold leading-6 text-foreground",
          value === "-" && "text-muted-foreground",
        )}
      >
        {value}
      </p>
    </div>
  );
}
