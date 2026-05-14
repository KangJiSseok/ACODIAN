"use client"

import { useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { useAuth } from "@/app/_common/hooks/useAuth"
import { isDirectorProfile } from "@/app/_common/utils/organizationAccess.utils"
import { WorklogForm } from "../_components/worklogForm"
import {
  useWorklogOptions,
} from "../_hooks/useWorklogList"
import { worklogService } from "../_service/worklog.service"
import { useTeamList } from "../../team/_hooks/useTeamList"
import type { TeamSummary } from "../../team/_types/team.types"
import type {
  WorklogFormDependencyOption,
  WorklogFormTagOption,
  WorklogFormTeamOption,
  WorklogFormValues,
  WorklogOptionsApiResponse,
  WorklogStatus,
} from "../_types/worklog.types"

const statusCodeMap: Record<string, WorklogStatus> = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "DONE",
  DONE: "DONE",
  ON_HOLD: "ON_HOLD",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
}

export default function WorklogCreatePage() {
  const router = useRouter()
  const { user } = useAuth()
  const {
    data: teamPage,
    isError: isTeamListError,
    isLoading: isTeamListLoading,
  } = useTeamList({ pageSize: 100 })
  const canCreate = Boolean(user && !isDirectorProfile(user))
  const activeTeams = useMemo(
    () => filterActiveTeams(teamPage?.items ?? []),
    [teamPage?.items]
  )
  const defaultTeamId = useMemo(
    () => resolveInitialTeamId(user, activeTeams),
    [activeTeams, user]
  )
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null)
  const effectiveTeamId = selectedTeamId ?? defaultTeamId
  const {
    data: worklogOptions,
    isError: isWorklogOptionsError,
    isLoading: isWorklogOptionsLoading,
  } = useWorklogOptions(effectiveTeamId)
  const formContext = useMemo(
    () => buildCreateFormContext(activeTeams, worklogOptions, effectiveTeamId),
    [activeTeams, effectiveTeamId, worklogOptions]
  )

  if (!canCreate) {
    return <div>업무 등록 권한이 없습니다.</div>
  }

  if (isTeamListLoading || isWorklogOptionsLoading) {
    return <div>업무 등록 정보를 불러오는 중입니다.</div>
  }

  if (
    isTeamListError ||
    isWorklogOptionsError ||
    !worklogOptions ||
    !formContext
  ) {
    return <div>업무 등록 정보를 불러오지 못했습니다.</div>
  }

  return (
    <div className="flex flex-col gap-5 lg:gap-6">
      <PageHeader title="업무 등록" />
      <WorklogForm
        initialValues={formContext.initialValues}
        submitLabel="업무 생성"
        onTeamIdChange={setSelectedTeamId}
        teamOptionsSource={formContext.teamOptionsSource}
        dependencyOptionsSource={formContext.dependencyOptionsSource}
        tagOptionsSource={formContext.tagOptionsSource}
        onSubmit={async (values) => {
          const created = await worklogService.create(values)
          router.push(`/worklog/detail/${created.worklogId}`)
        }}
      />
    </div>
  )
}

function buildCreateFormContext(
  teams: TeamSummary[],
  worklogOptions: WorklogOptionsApiResponse | undefined,
  teamId: number | null | undefined
) {
  if (!worklogOptions || !teamId) return null

  const teamOptionsSource = buildTeamOptions(teams)

  return {
    initialValues: buildInitialValues(teamId),
    teamOptionsSource,
    dependencyOptionsSource: buildDependencyOptions(worklogOptions),
    tagOptionsSource: buildTagOptions(worklogOptions),
  }
}

function resolveInitialTeamId(
  user: ReturnType<typeof useAuth>["user"],
  teams: TeamSummary[]
) {
  const authPrimaryTeamId = user?.teams.find((team) => team.isPrimary)?.teamId
  const activeTeamIds = new Set(teams.map((team) => team.teamId))
  const primaryTeamId = authPrimaryTeamId && activeTeamIds.has(authPrimaryTeamId)
    ? authPrimaryTeamId
    : null
  const firstAuthActiveTeamId = user?.teams.find((team) =>
    activeTeamIds.has(team.teamId)
  )?.teamId

  return primaryTeamId ?? firstAuthActiveTeamId ?? teams[0]?.teamId ?? null
}

function buildInitialValues(teamId: number): WorklogFormValues {
  return {
    title: "",
    requestContent: "",
    workContent: "",
    status: "PENDING",
    importance: "NORMAL",
    actualHours: 0,
    instructionDate: "2026-04-13",
    dueDate: "2026-04-16",
    teamId,
    dependencyIds: [],
    attachmentNames: [],
    attachmentFiles: [],
    attachmentFileItems: [],
    removeFileIds: [],
    tagIds: [],
    removeTagIds: [],
    statusChangeReason: "",
  }
}

function buildTeamOptions(teams: TeamSummary[]): WorklogFormTeamOption[] {
  return teams.map((team) => ({
    id: team.teamId,
    name: team.teamName,
  }))
}

function filterActiveTeams(teams: TeamSummary[]) {
  return teams.filter((team) => team.statusCode === "ACTIVE")
}

function buildDependencyOptions(
  worklogOptions: WorklogOptionsApiResponse
): WorklogFormDependencyOption[] {
  return worklogOptions.predecessorCandidates.map((candidate) => ({
    id: candidate.worklogId,
    title: candidate.title,
    status: statusCodeMap[candidate.statusCode] ?? "PENDING",
    teamId: candidate.teamId,
    teamName: candidate.teamName,
    authorId: candidate.authorId,
    authorName: candidate.authorName,
    aiSummary: candidate.aiSummary ?? "",
    workContent: candidate.workContent,
    isDeleted: false,
    dependencyIds: [],
  }))
}

function buildTagOptions(
  worklogOptions: WorklogOptionsApiResponse
): WorklogFormTagOption[] {
  return worklogOptions.tags.map((tag) => ({
    id: tag.tagId,
    name: tag.tagName,
    usageCount: tag.usageCount,
    category: "업무",
    source: "MANUAL",
    reuseHint: "",
  }))
}
