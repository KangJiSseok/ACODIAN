"use client"

import { useMemo } from "react"
import { useParams, useRouter } from "next/navigation"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { WorklogForm } from "../../_components/worklogForm"
import {
  useWorklogDetail,
  useWorklogFilterOptions,
} from "../../_hooks/useWorklogList"
import { worklogService } from "../../_service/worklog.service"
import type {
  Worklog,
  WorklogFilterOptions,
  WorklogFormDependencyOption,
  WorklogFormTagOption,
  WorklogFormTeamOption,
  WorklogFormUserOption,
  WorklogFormValues,
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
  const params = useParams<{ id: string }>()
  const worklogId = Number(params.id)
  const {
    data: worklog,
    isError: isWorklogError,
    isLoading: isWorklogLoading,
  } = useWorklogDetail(worklogId)
  const {
    data: filterOptions,
    isLoading: isFilterOptionsLoading,
  } = useWorklogFilterOptions()

  const formContext = useMemo(() => {
    if (!worklog) return null

    const teamOptionsSource = buildTeamOptions(worklog, filterOptions)
    const authorOptionsSource = buildAuthorOptions(worklog, filterOptions)
    const tagOptionsSource = buildTagOptions(filterOptions)
    const dependencyOptionsSource = buildDependencyOptions(worklog)
    const tagIds = resolveTagIds(worklog.tagNames ?? [], tagOptionsSource)

    return {
      initialValues: toWorklogFormValues(worklog, tagIds),
      teamOptionsSource,
      authorOptionsSource,
      dependencyOptionsSource,
      tagOptionsSource,
    }
  }, [filterOptions, worklog])

  if (isWorklogLoading || isFilterOptionsLoading) {
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
        teamOptionsSource={formContext.teamOptionsSource}
        authorOptionsSource={formContext.authorOptionsSource}
        dependencyOptionsSource={formContext.dependencyOptionsSource}
        tagOptionsSource={formContext.tagOptionsSource}
        onSubmit={async (values) => {
          await worklogService.update(worklog.id, values)
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
    authorId: worklog.authorId,
    dependencyIds: worklog.dependencyIds,
    attachmentNames:
      worklog.fileItems?.map((file) => file.originalName).filter(Boolean) ?? [],
    tagIds,
    aiSummary: worklog.aiSummary,
    aiSummaryEdited: worklog.aiSummaryEdited,
  }
}

function buildTeamOptions(
  worklog: Worklog,
  filterOptions: WorklogFilterOptions | undefined
): WorklogFormTeamOption[] {
  const options =
    filterOptions?.teams.map((team) => ({
      id: team.teamId,
      name: team.teamName,
    })) ?? []

  return ensureOption(options, {
    id: worklog.teamId,
    name: worklog.teamName ?? `팀 ${worklog.teamId}`,
  })
}

function buildAuthorOptions(
  worklog: Worklog,
  filterOptions: WorklogFilterOptions | undefined
): WorklogFormUserOption[] {
  const indexed = new Map<number, WorklogFormUserOption>()

  filterOptions?.teams.forEach((team) => {
    team.members.forEach((member) => {
      indexed.set(member.userId, {
        id: member.userId,
        name: member.userName,
      })
    })
  })

  return ensureOption(Array.from(indexed.values()), {
    id: worklog.authorId,
    name: worklog.authorName ?? `사용자 ${worklog.authorId}`,
  })
}

function buildTagOptions(
  filterOptions: WorklogFilterOptions | undefined
): WorklogFormTagOption[] {
  return (
    filterOptions?.tags.map((tag) => ({
      id: tag.tagId,
      name: tag.tagName,
      usageCount: 0,
      category: "업무",
      source: "MANUAL",
      reuseHint: "",
    })) ?? []
  )
}

function buildDependencyOptions(worklog: Worklog): WorklogFormDependencyOption[] {
  return (
    worklog.dependOnWorklogs?.map((dependency) => ({
      id: dependency.worklogId,
      title: dependency.title,
      status: statusCodeMap[dependency.statusCode] ?? "PENDING",
      isDeleted: false,
      dependencyIds: [],
    })) ?? []
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
