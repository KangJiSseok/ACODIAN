"use client"

import { useMemo, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { useQueryClient } from "@tanstack/react-query"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { WorklogForm } from "../../_components/worklogForm"
import {
  worklogKeys,
  useWorklogDetail,
  useWorklogOptions,
} from "../../_hooks/useWorklogList"
import { worklogService } from "../../_service/worklog.service"
import { useTeamList } from "../../../team/_hooks/useTeamList"
import type { TeamSummary } from "../../../team/_types/team.types"
import type {
  Worklog,
  WorklogFormDependencyOption,
  WorklogFormTagOption,
  WorklogFormTeamOption,
  WorklogFormValues,
  WorklogOptionsApiResponse,
  WorklogStatus,
} from "../../_types/worklog.types"

const statusCodeMap: Record<string, WorklogStatus> = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "DONE",
  DONE: "DONE",
  ON_HOLD: "ON_HOLD",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
}

export default function WorklogEditPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const params = useParams<{ id: string }>()
  const worklogId = Number(params.id)
  const {
    data: worklog,
    isError: isWorklogError,
    isLoading: isWorklogLoading,
  } = useWorklogDetail(worklogId)
  const {
    data: teamPage,
    isLoading: isTeamListLoading,
  } = useTeamList({ pageSize: 100 })
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null)
  const effectiveTeamId = selectedTeamId ?? worklog?.teamId
  const {
    data: worklogOptions,
    isLoading: isWorklogOptionsLoading,
  } = useWorklogOptions(effectiveTeamId)

  const formContext = useMemo(() => {
    if (!worklog || !worklogOptions) return null

    const teamOptionsSource = buildTeamOptions(worklog, teamPage?.items ?? [])
    const tagOptionsSource = buildTagOptions(worklogOptions)
    const dependencyOptionsSource = buildDependencyOptions(worklog, worklogOptions)
    const tagIds = resolveTagIds(worklog.tagNames ?? [], tagOptionsSource)

    return {
      initialValues: toWorklogFormValues(worklog, tagIds),
      teamOptionsSource,
      dependencyOptionsSource,
      tagOptionsSource,
    }
  }, [teamPage?.items, worklog, worklogOptions])

  if (isWorklogLoading || isTeamListLoading || isWorklogOptionsLoading) {
    return <div>업무를 불러오는 중입니다.</div>
  }

  if (isWorklogError || !worklog || !formContext) {
    return <div>업무를 찾을 수 없습니다.</div>
  }

  return (
    <div className="flex flex-col gap-5 lg:gap-6">
      <PageHeader title={`${worklog.title} 수정`} />
      <WorklogForm
        key={worklog.id}
        initialValues={formContext.initialValues}
        currentWorklogId={worklog.id}
        submitLabel="수정 저장"
        onTeamIdChange={setSelectedTeamId}
        teamOptionsSource={formContext.teamOptionsSource}
        dependencyOptionsSource={formContext.dependencyOptionsSource}
        tagOptionsSource={formContext.tagOptionsSource}
        onSubmit={async (values) => {
          await worklogService.update(worklog.id, values)
          await Promise.all([
            queryClient.invalidateQueries({
              queryKey: worklogKeys.detail(worklog.id),
            }),
            queryClient.invalidateQueries({ queryKey: worklogKeys.all }),
          ])
          router.push(`/worklog/detail/${worklog.id}`)
        }}
      />
    </div>
  )
}

function toWorklogFormValues(
  worklog: Worklog,
  tagIds: number[]
): WorklogFormValues {
  return {
    title: worklog.title,
    requestContent: worklog.requestContent,
    workContent: worklog.workContent,
    status: worklog.status,
    importance: worklog.importance,
    actualHours: worklog.actualHours,
    instructionDate: worklog.instructionDate,
    dueDate: worklog.dueDate,
    teamId: worklog.teamId,
    dependencyIds: worklog.dependencyIds,
    attachmentNames:
      worklog.fileItems?.map((file) => file.originalName).filter(Boolean) ?? [],
    attachmentFiles: [],
    attachmentFileItems: worklog.fileItems ?? [],
    removeFileIds: [],
    tagIds,
    aiSummary: worklog.aiSummary,
    aiSummaryEdited: worklog.aiSummaryEdited,
  }
}

function buildTeamOptions(
  worklog: Worklog,
  teams: TeamSummary[]
): WorklogFormTeamOption[] {
  const options = teams.map((team) => ({
    id: team.teamId,
    name: team.teamName,
  }))

  return ensureOption(options, {
    id: worklog.teamId,
    name: worklog.teamName ?? `팀 ${worklog.teamId}`,
  })
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

function buildDependencyOptions(
  worklog: Worklog,
  worklogOptions: WorklogOptionsApiResponse
): WorklogFormDependencyOption[] {
  const options: WorklogFormDependencyOption[] =
    worklogOptions.predecessorCandidates.map((candidate) => ({
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

  return (
    worklog.dependOnWorklogs?.reduce((acc, dependency) => ensureOption(acc, {
      id: dependency.worklogId,
      title: dependency.title,
      status: statusCodeMap[dependency.statusCode] ?? "PENDING",
      isDeleted: false,
      dependencyIds: [],
    }), options) ?? options
  )
}

function resolveTagIds(
  tagNames: string[],
  tagOptions: WorklogFormTagOption[]
): number[] {
  return tagNames
    .map((tagName) => tagOptions.find((tag) => tag.name === tagName)?.id)
    .filter((tagId): tagId is number => typeof tagId === "number")
}

function ensureOption<T extends { id: number }>(options: T[], current: T): T[] {
  if (options.some((option) => option.id === current.id)) return options
  return [current, ...options]
}
