"use client";

import Link from "next/link";
import {
  type ChangeEvent,
  type FormEvent,
  useMemo,
  useState,
} from "react";
import {
  ArrowLeft,
  Pencil,
  Plus,
} from "lucide-react";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { selectAuthUser, useAuthStore } from "@/app/_common/store/auth.store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select, type SelectOption } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import type { DepartmentSummary } from "../../department/_types/department.types";
import {
  useCreateUserEvaluation,
  useCreateUserSkill,
  useUpdateUser,
} from "../_hooks";
import type {
  EmploymentStatusCode,
  UserDetail as UserDetailType,
  UserEvaluationSummary,
  UserSkillSummary,
} from "../_types/user.types";
import { canWriteUserInsight } from "../_utils/userAccess.utils";

type UserDetailTab = "profile" | "skills" | "evaluations";

const tabs: Array<{ value: UserDetailTab; label: string }> = [
  { value: "profile", label: "사용자 정보" },
  { value: "skills", label: "스킬 설정" },
  { value: "evaluations", label: "관리자 평가" },
];

type UserDetailFormValues = {
  departmentId: string;
  positionName: string;
  titleName: string;
  employmentStatus: string;
};

type UserSkillFormValues = {
  skillName: string;
  skillLevel: string;
};

type UserEvaluationFormValues = {
  content: string;
};

const employmentStatusOptions: SelectOption[] = [
  { value: "ACTIVE", label: "재직" },
  { value: "LEAVE", label: "휴직" },
  { value: "RETIRED", label: "퇴사" },
];

const skillLevelOptions: SelectOption[] = [1, 2, 3, 4, 5].map((level) => ({
  value: String(level),
  label: `Lv.${level}`,
}));

export default function UserDetail({
  user,
  departments,
  skills,
  evaluations,
  isSkillsLoading = false,
  isEvaluationsLoading = false,
}: {
  user: UserDetailType;
  departments: DepartmentSummary[];
  skills: UserSkillSummary[];
  evaluations: UserEvaluationSummary[];
  isSkillsLoading?: boolean;
  isEvaluationsLoading?: boolean;
}) {
  const [activeTab, setActiveTab] = useState<UserDetailTab>("profile");
  const currentUser = useAuthStore(selectAuthUser);
  const canCreateUserInsights = canWriteUserInsight(currentUser, user);

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button asChild variant="outline" className="h-10">
          <Link href="/user">
            <ArrowLeft className="size-4" />
            사용자 목록
          </Link>
        </Button>
      </div>

      <Card className="rounded-[28px]">
        <CardContent className="space-y-8 p-6 sm:p-8">
          <div className="space-y-2">
            <h2 className="text-lg font-semibold text-foreground">
              사용자 정보 및 직책 설정
            </h2>
            <p className="text-sm leading-6 text-muted-foreground">
              소속 부서, 직급, 직책, 상태만 이 화면에서 바로 수정하고,
              나머지 사용자 정보는 조회 전용으로 확인합니다.
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
              key={user.userId}
              user={user}
              departments={departments}
            />
          ) : null}
          {activeTab === "skills" ? (
            <SkillsTab
              userId={user.userId}
              skills={skills}
              isLoading={isSkillsLoading}
              canCreate={canCreateUserInsights}
            />
          ) : null}
          {activeTab === "evaluations" ? (
            <EvaluationsTab
              userId={user.userId}
              evaluations={evaluations}
              isLoading={isEvaluationsLoading}
              canCreate={canCreateUserInsights}
            />
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function ProfileTab({
  user,
  departments,
}: {
  user: UserDetailType;
  departments: DepartmentSummary[];
}) {
  const updateUser = useUpdateUser(user.userId);
  const [values, setValues] = useState<UserDetailFormValues>(() =>
    createUserDetailFormValues(user),
  );
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const departmentOptions = useMemo(
    () => buildDepartmentOptions(user, departments),
    [departments, user],
  );
  const otherTeams = useMemo(
    () => user.teams.filter((team) => !team.isPrimary),
    [user.teams],
  );
  const isSaving = updateUser.isPending;

  function handleChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value } = event.target;

    setValues((prev) => ({
      ...prev,
      [name]: value,
    }));
    setMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    try {
      await updateUser.mutateAsync({
        userName: null,
        email: null,
        positionName: valueOrNull(values.positionName),
        titleName: valueOrNull(values.titleName),
        departmentId: numberOrNull(values.departmentId),
        phone: null,
        employmentStatus: values.employmentStatus as EmploymentStatusCode,
        joinDate: null,
        primaryTeamId: null,
        profileImage: null,
      });
      setMessage({
        type: "success",
        text: "사용자 정보가 저장되었습니다.",
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: getApiErrorMessage(error, "사용자 정보 저장에 실패했습니다."),
      });
    }
  }

  return (
    <form className="space-y-8" onSubmit={handleSubmit}>
      <div className="space-y-4">
        <p className="text-sm font-semibold text-muted-foreground">기본 정보</p>
        <div className="grid gap-4 xl:grid-cols-3">
          <ReadOnlyField label="이름" value={user.userName} />
          <ReadOnlyField label="이메일" value={user.email} />
          <ReadOnlyProfileImage user={user} />
        </div>
      </div>

      <div className="border-t border-border/70 pt-8">
        <p className="mb-4 text-sm font-semibold text-muted-foreground">
          조직 배치
        </p>
        <div className="grid gap-4 xl:grid-cols-3">
          <SelectField
            label="소속 부서"
            name="departmentId"
            value={values.departmentId}
            options={departmentOptions}
            onChange={handleChange}
            disabled={isSaving || departmentOptions.length === 0}
          />
          <EditableField
            label="직급"
            name="positionName"
            value={values.positionName}
            onChange={handleChange}
            disabled={isSaving}
          />
          <EditableField
            label="직책"
            name="titleName"
            value={values.titleName}
            onChange={handleChange}
            disabled={isSaving}
          />
          <OtherTeamsSelect
            label="다른 소속 팀"
            teams={otherTeams}
            disabled={isSaving}
          />
        </div>
      </div>

      <div className="border-t border-border/70 pt-8">
        <p className="mb-4 text-sm font-semibold text-muted-foreground">
          인사 정보
        </p>
        <div className="grid gap-4 xl:grid-cols-3">
          <ReadOnlyField label="연락처" value={user.phone} />
          <SelectField
            label="상태"
            name="employmentStatus"
            value={values.employmentStatus}
            options={employmentStatusOptions}
            onChange={handleChange}
            disabled={isSaving}
          />
          <ReadOnlyField label="입사일" value={formatDateInput(user.joinDate)} />
        </div>
      </div>

      {message ? (
        <p
          className={cn(
            "rounded-2xl border px-4 py-3 text-sm font-semibold",
            message.type === "success"
              ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-700"
              : "border-destructive/30 bg-destructive/5 text-destructive",
          )}
        >
          {message.text}
        </p>
      ) : null}

      <div className="flex justify-end">
        <Button
          type="submit"
          disabled={isSaving}
          className="h-12 min-w-36 rounded-2xl !text-primary-foreground hover:!text-primary-foreground"
        >
          {isSaving ? "저장 중" : "수정 저장"}
        </Button>
      </div>
    </form>
  );
}

function SkillsTab({
  userId,
  skills,
  isLoading,
  canCreate,
}: {
  userId: number;
  skills: UserSkillSummary[];
  isLoading: boolean;
  canCreate: boolean;
}) {
  const createSkill = useCreateUserSkill(userId);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [values, setValues] = useState<UserSkillFormValues>({
    skillName: "",
    skillLevel: "3",
  });
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const isSaving = createSkill.isPending;

  function handleChange(
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value } = event.target;

    setValues((prev) => ({
      ...prev,
      [name]: value,
    }));
    setMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canCreate) {
      setMessage({
        type: "error",
        text: "자신보다 낮은 직책의 사용자에게만 스킬을 추가할 수 있습니다.",
      });
      return;
    }

    const skillName = values.skillName.trim();
    const skillLevel = Number(values.skillLevel);

    if (!skillName) {
      setMessage({
        type: "error",
        text: "스킬명을 입력해주세요.",
      });
      return;
    }

    try {
      await createSkill.mutateAsync({ skillName, skillLevel });
      setValues({ skillName: "", skillLevel: "3" });
      setIsFormOpen(false);
      setMessage({
        type: "success",
        text: "스킬이 추가되었습니다.",
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: getApiErrorMessage(error, "스킬 추가에 실패했습니다."),
      });
    }
  }

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
            {!canCreate ? (
              <p className="text-sm leading-6 text-muted-foreground">
                자신보다 낮은 직책의 사용자에게만 스킬을 추가할 수 있습니다.
              </p>
            ) : null}
        </div>
        <Button
          type="button"
          disabled={!canCreate || isSaving}
          onClick={() => {
            if (!canCreate) {
              return;
            }
            setIsFormOpen((prev) => !prev);
            setMessage(null);
          }}
          className="h-11 min-w-32 rounded-2xl !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
        >
          <Plus className="size-4" />
          스킬 추가
        </Button>
      </div>

      {isFormOpen && canCreate ? (
        <form
          className="grid gap-3 rounded-2xl border border-border/80 bg-background/70 p-4 md:grid-cols-[minmax(0,1fr)_9rem_auto]"
          onSubmit={handleSubmit}
        >
          <label className="space-y-2">
            <span className="text-sm font-semibold text-foreground">스킬명</span>
            <Input
              name="skillName"
              value={values.skillName}
              maxLength={100}
              disabled={isSaving}
              onChange={handleChange}
              className="h-11 bg-input/80 font-medium"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-semibold text-foreground">레벨</span>
            <Select
              name="skillLevel"
              value={values.skillLevel}
              options={skillLevelOptions}
              disabled={isSaving}
              onChange={handleChange}
              className="h-11 bg-input/80 font-medium"
            />
          </label>
          <div className="flex items-end gap-2">
            <Button
              type="submit"
              disabled={isSaving}
              className="h-11 min-w-24 rounded-2xl !text-primary-foreground hover:!text-primary-foreground"
            >
              {isSaving ? "저장 중" : "스킬 저장"}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={isSaving}
              className="h-11"
              onClick={() => {
                setIsFormOpen(false);
                setMessage(null);
              }}
            >
              취소
            </Button>
          </div>
        </form>
      ) : null}

      {message ? (
        <p
          className={cn(
            "rounded-2xl border px-4 py-3 text-sm font-semibold",
            message.type === "success"
              ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-700"
              : "border-destructive/30 bg-destructive/5 text-destructive",
          )}
        >
          {message.text}
        </p>
      ) : null}

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
  userId,
  evaluations,
  isLoading,
  canCreate,
}: {
  userId: number;
  evaluations: UserEvaluationSummary[];
  isLoading: boolean;
  canCreate: boolean;
}) {
  const createEvaluation = useCreateUserEvaluation(userId);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [values, setValues] = useState<UserEvaluationFormValues>({
    content: "",
  });
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const isSaving = createEvaluation.isPending;

  function handleChange(event: ChangeEvent<HTMLTextAreaElement>) {
    setValues({ content: event.target.value });
    setMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canCreate) {
      setMessage({
        type: "error",
        text: "자신보다 낮은 직책의 사용자에게만 평가를 작성할 수 있습니다.",
      });
      return;
    }

    const content = values.content.trim();

    if (!content) {
      setMessage({
        type: "error",
        text: "평가 내용을 입력해주세요.",
      });
      return;
    }

    try {
      await createEvaluation.mutateAsync({ content });
      setValues({ content: "" });
      setIsFormOpen(false);
      setMessage({
        type: "success",
        text: "평가가 추가되었습니다.",
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: getApiErrorMessage(error, "평가 추가에 실패했습니다."),
      });
    }
  }

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
          {!canCreate ? (
            <p className="text-sm leading-6 text-muted-foreground">
              자신보다 낮은 직책의 사용자에게만 평가를 작성할 수 있습니다.
            </p>
          ) : null}
        </div>
        <Button
          type="button"
          disabled={!canCreate || isSaving}
          onClick={() => {
            if (!canCreate) {
              return;
            }
            setIsFormOpen((prev) => !prev);
            setMessage(null);
          }}
          className="h-11 min-w-32 rounded-2xl !text-primary-foreground hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
        >
          <Pencil className="size-4" />
          평가 작성
        </Button>
      </div>

      {isFormOpen && canCreate ? (
        <form
          className="space-y-3 rounded-2xl border border-border/80 bg-background/70 p-4"
          onSubmit={handleSubmit}
        >
          <label className="space-y-2">
            <span className="text-sm font-semibold text-foreground">
              평가 내용
            </span>
            <Textarea
              name="content"
              value={values.content}
              disabled={isSaving}
              onChange={handleChange}
              className="min-h-32 bg-input/80 font-medium"
            />
          </label>
          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={isSaving}
              className="h-11"
              onClick={() => {
                setIsFormOpen(false);
                setMessage(null);
              }}
            >
              취소
            </Button>
            <Button
              type="submit"
              disabled={isSaving}
              className="h-11 min-w-24 rounded-2xl !text-primary-foreground hover:!text-primary-foreground"
            >
              {isSaving ? "저장 중" : "평가 저장"}
            </Button>
          </div>
        </form>
      ) : null}

      {message ? (
        <p
          className={cn(
            "rounded-2xl border px-4 py-3 text-sm font-semibold",
            message.type === "success"
              ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-700"
              : "border-destructive/30 bg-destructive/5 text-destructive",
          )}
        >
          {message.text}
        </p>
      ) : null}

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
}: {
  label: string;
  value: string | null | undefined;
}) {
  return (
    <div className="space-y-2">
      <p className="text-sm font-semibold text-foreground">{label}</p>
      <div className="flex min-h-14 items-center rounded-2xl border border-border/70 bg-muted/35 px-4">
        <span className="min-w-0 break-words text-sm font-medium text-muted-foreground">
          {value || "-"}
        </span>
      </div>
    </div>
  );
}

function EditableField({
  label,
  name,
  value,
  type = "text",
  disabled = false,
  onChange,
}: {
  label: string;
  name: keyof UserDetailFormValues;
  value: string;
  type?: string;
  disabled?: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}) {
  return (
    <label className="space-y-2">
      <span className="text-sm font-semibold text-foreground">{label}</span>
      <Input
        name={name}
        type={type}
        value={value}
        disabled={disabled}
        onChange={onChange}
        className="h-14 bg-input/80 font-medium"
      />
    </label>
  );
}

function SelectField({
  label,
  name,
  value,
  options,
  disabled = false,
  onChange,
}: {
  label: string;
  name: keyof UserDetailFormValues;
  value: string;
  options: SelectOption[];
  disabled?: boolean;
  onChange: (event: ChangeEvent<HTMLSelectElement>) => void;
}) {
  return (
    <label className="space-y-2">
      <span className="text-sm font-semibold text-foreground">{label}</span>
      <Select
        name={name}
        value={value}
        options={options}
        disabled={disabled}
        onChange={onChange}
        className="h-14 bg-input/80 font-medium"
      />
    </label>
  );
}

function OtherTeamsSelect({
  label,
  teams,
  disabled = false,
}: {
  label: string;
  teams: UserDetailType["teams"];
  disabled?: boolean;
}) {
  const options = useMemo<SelectOption[]>(() => {
    if (teams.length === 0) {
      return [{ value: "none", label: "다른 소속 팀 없음" }];
    }

    return [
      {
        value: "all",
        label: `다른 소속 팀 전체 보기 (${teams.length})`,
      },
      ...teams.map((team) => ({
        value: String(team.teamId),
        label: buildTeamOptionLabel(team),
      })),
    ];
  }, [teams]);
  const [value, setValue] = useState(options[0]?.value ?? "none");

  return (
    <label className="space-y-2">
      <span className="text-sm font-semibold text-foreground">{label}</span>
      <Select
        key={options.map((option) => option.value).join("|")}
        aria-label="다른 소속 팀 전체 보기"
        value={value}
        options={options}
        disabled={disabled || teams.length === 0}
        onChange={(event) => setValue(event.target.value)}
        className="h-14 bg-input/80 font-medium"
      />
    </label>
  );
}

function ReadOnlyProfileImage({ user }: { user: UserDetailType }) {
  return (
    <div className="space-y-2">
      <p className="text-sm font-semibold text-foreground">프로필 이미지</p>
      <div className="flex min-h-14 items-center gap-3 rounded-2xl border border-border/70 bg-muted/35 px-4">
        <Avatar className="size-10">
          {user.profileImageUrl ? (
            <AvatarImage src={user.profileImageUrl} alt={user.userName} />
          ) : null}
          <AvatarFallback>{user.userName.slice(0, 1)}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-foreground">
            {user.userName}
          </p>
          <p className="truncate text-xs text-muted-foreground">
            {user.profileImageUrl ? "등록된 이미지" : "이미지 없음"}
          </p>
        </div>
      </div>
    </div>
  );
}

function createUserDetailFormValues(user: UserDetailType): UserDetailFormValues {
  return {
    departmentId: user.departmentId ? String(user.departmentId) : "",
    positionName: user.positionName ?? "",
    titleName: user.titleName ?? "",
    employmentStatus: user.employmentStatus,
  };
}

function formatDateInput(value: string | null | undefined) {
  if (!value) {
    return "";
  }

  return value.slice(0, 10);
}

function buildDepartmentOptions(
  user: UserDetailType,
  departments: DepartmentSummary[],
) {
  const options = departments.map((department) => ({
    value: String(department.departmentId),
    label: department.departmentName,
  }));

  if (
    user.departmentId &&
    !options.some((option) => option.value === String(user.departmentId))
  ) {
    options.unshift({
      value: String(user.departmentId),
      label: user.departmentName ?? `부서 ${user.departmentId}`,
    });
  }

  return options;
}

function buildTeamOptionLabel(team: UserDetailType["teams"][number]) {
  return [
    team.teamName,
    team.isLeader ? "팀 대표" : null,
    team.teamRole,
  ]
    .filter(Boolean)
    .join(" · ");
}

function valueOrNull(value: string) {
  const trimmed = value.trim();

  return trimmed || null;
}

function numberOrNull(value: string) {
  return value ? Number(value) : null;
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
