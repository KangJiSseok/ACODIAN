"use client"

import { useMemo } from "react"
import { useRouter } from "next/navigation"
import PageHeader from "@/app/_common/components/layout/pageHeader"
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
  const {
    data: teamPage,
    isError: isTeamListError,
    isLoading: isTeamListLoading,
  } = useTeamList({ pageSize: 100 })
  const {
    data: worklogOptions,
    isError: isWorklogOptionsError,
    isLoading: isWorklogOptionsLoading,
  } = useWorklogOptions()
  const formContext = useMemo(
    () => buildCreateFormContext(teamPage?.items ?? [], worklogOptions),
    [teamPage?.items, worklogOptions]
  )

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
  worklogOptions: WorklogOptionsApiResponse | undefined
) {
  if (!worklogOptions) return null

  const teamOptionsSource = buildTeamOptions(teams)

  return {
    initialValues: buildInitialValues(teamOptionsSource[0]?.id ?? 0),
    teamOptionsSource,
    dependencyOptionsSource: buildDependencyOptions(worklogOptions),
    tagOptionsSource: buildTagOptions(worklogOptions),
  }
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
    tagIds: [],
  }
}

function buildTeamOptions(teams: TeamSummary[]): WorklogFormTeamOption[] {
  return teams.map((team) => ({
    id: team.teamId,
    name: team.teamName,
  }))
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
