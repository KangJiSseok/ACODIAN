"use client"

import Image from "next/image"
import Link from "next/link"
import { useParams } from "next/navigation"
import { useState } from "react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { PencilLine } from "lucide-react"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { useAuth } from "@/app/_common/hooks/useAuth"
import { ApiClientError, getApiErrorMessage } from "@/app/_common/service/api-client"
import { dashboardKeys } from "../../../_dashboard/_hooks/useDirectorDashboard"
import { WorklogDetail } from "../../_components/worklogDetail"
import { worklogKeys, useWorklogDetail } from "../../_hooks/useWorklogList"
import { worklogService } from "../../_service/worklog.service"
import { Button } from "@/components/ui/button"
import type { Worklog, WorklogStatus } from "../../_types/worklog.types"

export default function WorklogDetailPage() {
  const params = useParams<{ id: string }>()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const worklogId = Number(params.id)
  const { data: worklog, error, isError, isLoading } = useWorklogDetail(worklogId)
  const [transitionNotice, setTransitionNotice] = useState<string>()
  const [transitionErrorMessage, setTransitionErrorMessage] = useState<string>()
  const [aiSummaryRetryErrorMessage, setAiSummaryRetryErrorMessage] = useState<string>()
  const transitionMutation = useMutation({
    mutationFn: ({ nextStatus, reason }: { nextStatus: WorklogStatus; reason: string }) =>
      worklogService.transitionStatus(worklogId, nextStatus, reason),
  })
  const aiSummaryRetryMutation = useMutation({
    mutationFn: () => worklogService.retryAiSummary(worklogId),
  })

  const handleTransition = async (nextStatus: WorklogStatus, reason: string) => {
    setTransitionNotice(undefined)
    setTransitionErrorMessage(undefined)

    try {
      await transitionMutation.mutateAsync({ nextStatus, reason })
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: worklogKeys.detail(worklogId),
        }),
        queryClient.invalidateQueries({ queryKey: worklogKeys.lists() }),
        queryClient.invalidateQueries({ queryKey: worklogKeys.searches() }),
        queryClient.invalidateQueries({ queryKey: dashboardKeys.all }),
      ])
      setTransitionNotice("업무 상태를 변경했습니다.")
    } catch (error) {
      setTransitionErrorMessage(
        getApiErrorMessage(error, "업무 상태를 변경하지 못했습니다.")
      )
      throw error
    }
  }

  const handleRetryAiSummary = async () => {
    setAiSummaryRetryErrorMessage(undefined)

    try {
      await aiSummaryRetryMutation.mutateAsync()
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: worklogKeys.detail(worklogId),
        }),
        queryClient.invalidateQueries({ queryKey: worklogKeys.lists() }),
        queryClient.invalidateQueries({ queryKey: worklogKeys.searches() }),
      ])
    } catch (error) {
      setAiSummaryRetryErrorMessage(
        getApiErrorMessage(error, "AI 요약을 다시 요청하지 못했습니다.")
      )
    }
  }

  if (isLoading) return <div>업무를 불러오는 중입니다.</div>
  const selectedWorklog = worklog
  if (isWorklogForbidden(error)) return <WorklogForbiddenState />
  if (isError || !selectedWorklog) return <div>업무를 찾을 수 없습니다.</div>

  return (
    <div className="flex flex-col gap-5 lg:gap-6">
      <PageHeader
        title={selectedWorklog.title}
        description="업무 상세, 의존성, 파일, AI 상태를 함께 확인합니다."
        actions={
          canEditSelectedWorklog(user?.userId, selectedWorklog) ? (
            <Button
              asChild
              variant="secondary"
              className="h-10 min-w-32 px-5 text-sm font-semibold"
            >
              <Link href={`/worklog/edit/${selectedWorklog.id}`}>
                <PencilLine className="size-4" />
                업무 수정
              </Link>
            </Button>
          ) : null
        }
      />
      <WorklogDetail
        worklog={selectedWorklog}
        canTransition={canTransitionSelectedWorklog(user, selectedWorklog)}
        onTransition={handleTransition}
        isTransitionPending={transitionMutation.isPending}
        transitionNotice={transitionNotice}
        transitionErrorMessage={transitionErrorMessage}
        transitionDisabledMessage="업무 상태는 작성자 본인만 변경할 수 있습니다."
        canRetryAiSummary={canEditSelectedWorklog(user?.userId, selectedWorklog)}
        isRetryingAiSummary={aiSummaryRetryMutation.isPending}
        aiSummaryRetryErrorMessage={aiSummaryRetryErrorMessage}
        onRetryAiSummary={handleRetryAiSummary}
      />
    </div>
  )
}

function WorklogForbiddenState() {
  return (
    <div className="flex min-h-[420px] flex-col items-center justify-center gap-5 text-center">
      <Image
        src="/videos/403error.webp"
        alt=""
        width={240}
        height={135}
        unoptimized
        className="h-auto w-60 object-contain"
      />
      <p className="text-lg font-semibold text-foreground">업무를 찾을 수 없습니다.</p>
    </div>
  )
}

function isWorklogForbidden(error: unknown) {
  return error instanceof ApiClientError && error.status === 403
}

function canEditSelectedWorklog(userId: number | undefined, worklog: Worklog) {
  return userId === worklog.authorId
}

function canTransitionSelectedWorklog(
  user: ReturnType<typeof useAuth>["user"],
  worklog: Worklog
) {
  return Boolean(user && user.userId === worklog.authorId)
}
