"use client"

import { useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { useAuth } from "@/app/_common/hooks/useAuth"
import { isDirectorProfile } from "@/app/_common/utils/organizationAccess.utils"
import { WorklogForm } from "../_components/worklogForm"
import {
  usePredecessorCandidates,
  useWorklogTagInfiniteSearch,
} from "../_hooks/useWorklogList"
import { worklogService } from "../_service/worklog.service"
import { useTeamList } from "../../team/_hooks/useTeamList"
import type { TeamSummary } from "../../team/_types/team.types"
import type {
  WorklogFormDependencyOption,
  WorklogFormTagOption,
  WorklogFormTeamOption,
  WorklogFormValues,
  WorklogOptionPredecessorCandidate,
  WorklogOptionTagItem,
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
  const defaultDate = useMemo(() => getTodayDateString(), [])
  const activeTeams = useMemo(
    () => filterActiveTeams(teamPage?.items ?? []),
    [teamPage?.items]
  )
  const defaultTeamId = useMemo(
    () => resolveInitialTeamId(user, activeTeams),
    [activeTeams, user]
  )
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null)
  const [tagSearchQuery, setTagSearchQuery] = useState("")
  const effectiveTeamId = selectedTeamId ?? defaultTeamId
  const {
    data: predecessorCandidates,
    isError: isPredecessorCandidatesError,
    isLoading: isPredecessorCandidatesLoading,
  } = usePredecessorCandidates({
    teamId: effectiveTeamId ?? 0,
    pageSize: 100,
  })
  const {
    data: tagPages,
    fetchNextPage: fetchNextTagPage,
    hasNextPage: hasNextTagPage,
    isFetchingNextPage: isFetchingNextTagPage,
  } = useWorklogTagInfiniteSearch(tagSearchQuery, {
    enabled: canCreate,
    retry: false,
  })
  const tagOptions = useMemo(
    () => mergeTagOptions(tagPages?.pages.flatMap((page) => page.items) ?? []),
    [tagPages?.pages]
  )
  const formContext = useMemo(
    () =>
      buildCreateFormContext(
        activeTeams,
        predecessorCandidates?.items ?? [],
        tagOptions,
        effectiveTeamId,
        defaultDate
      ),
    [activeTeams, defaultDate, effectiveTeamId, predecessorCandidates?.items, tagOptions]
  )

  if (!canCreate) {
    return <div>업무 등록 권한이 없습니다.</div>
  }

  if (isTeamListLoading || isPredecessorCandidatesLoading) {
    return <div>업무 등록 정보를 불러오는 중입니다.</div>
  }

  if (
    isTeamListError ||
    isPredecessorCandidatesError ||
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
        hasMoreTagCandidates={Boolean(hasNextTagPage)}
        isFetchingMoreTagCandidates={isFetchingNextTagPage}
        onTagSearchKeywordChange={setTagSearchQuery}
        onLoadMoreTagCandidates={() => fetchNextTagPage()}
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
  predecessorCandidates: WorklogOptionPredecessorCandidate[],
  tags: WorklogOptionTagItem[],
  teamId: number | null | undefined,
  defaultDate: string
) {
  if (!teamId) return null

  const teamOptionsSource = buildTeamOptions(teams)

  return {
    initialValues: buildInitialValues(teamId, defaultDate),
    teamOptionsSource,
    dependencyOptionsSource: buildDependencyOptions(predecessorCandidates),
    tagOptionsSource: buildTagOptions(tags),
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

function buildInitialValues(
  teamId: number,
  defaultDate: string
): WorklogFormValues {
  return {
    title: "",
    requestContent: "",
    workContent: "",
    status: "PENDING",
    importance: "NORMAL",
    actualHours: 0,
    instructionDate: defaultDate,
    dueDate: defaultDate,
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

function getTodayDateString() {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, "0")
  const date = String(today.getDate()).padStart(2, "0")

  return `${year}-${month}-${date}`
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
  predecessorCandidates: WorklogOptionPredecessorCandidate[]
): WorklogFormDependencyOption[] {
  return predecessorCandidates.map((candidate) => ({
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
  tags: WorklogOptionTagItem[]
): WorklogFormTagOption[] {
  return tags.map((tag) => ({
    id: tag.tagId,
    name: tag.tagName,
    usageCount: tag.usageCount,
    category: "업무",
    source: "MANUAL",
    reuseHint: "",
  }))
}

function mergeTagOptions(tags: WorklogOptionTagItem[]) {
  return Array.from(
    new Map(tags.map((tag) => [tag.tagId, tag])).values()
  )
}
