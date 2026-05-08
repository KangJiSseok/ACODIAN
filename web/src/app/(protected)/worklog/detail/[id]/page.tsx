"use client"

import Link from "next/link"
import { useParams } from "next/navigation"
import { useState } from "react"
import { PencilLine } from "lucide-react"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { useAuth } from "@/app/_common/hooks/useAuth"
import { WorklogDetail } from "../../_components/worklogDetail"
import { useWorklogDetail } from "../../_hooks/useWorklogList"
import { Button } from "@/components/ui/button"
import type { Worklog, WorklogStatus } from "../../_types/worklog.types"

export default function WorklogDetailPage() {
  const params = useParams<{ id: string }>()
  const { user } = useAuth()
  const worklogId = Number(params.id)
  const { data: worklog, isError, isLoading } = useWorklogDetail(worklogId)
  const [displayWorklog, setDisplayWorklog] = useState<Worklog | null>(null)
  const [transitionNotice, setTransitionNotice] = useState<{
    worklogId: number
    message: string
  } | null>(null)

  if (isLoading) return <div>업무를 불러오는 중입니다.</div>
  const selectedWorklog =
    displayWorklog?.id === worklog?.id ? displayWorklog : worklog
  if (isError || !selectedWorklog) return <div>업무를 찾을 수 없습니다.</div>

  const handleTransition = async (nextStatus: WorklogStatus, reason: string) => {
    const changedAt = new Date().toISOString()
    const statusCode = nextStatus === "DONE" ? "COMPLETED" : nextStatus

    setDisplayWorklog((current) => {
      const target = current?.id === selectedWorklog.id ? current : selectedWorklog

      return {
        ...target,
        status: nextStatus,
        statusCode,
        completionDate: nextStatus === "DONE" ? changedAt.slice(0, 10) : undefined,
        updatedAt: changedAt,
        statusHistory: [
          {
            id: Date.now(),
            previousStatus: target.status,
            newStatus: nextStatus,
            changedBy: user?.userId ?? target.authorId,
            changedByName: user?.userName,
            reason: reason.trim() || "상태 변경",
            changedAt,
          },
          ...target.statusHistory,
        ],
      }
    })
    setTransitionNotice({
      worklogId: selectedWorklog.id,
      message: "상태 변경 API 연동 전까지 현재 상세 화면에서만 변경 상태를 반영합니다.",
    })
  }
  const notice =
    transitionNotice?.worklogId === selectedWorklog.id
      ? transitionNotice.message
      : undefined

  return (
    <div className="flex flex-col gap-5 lg:gap-6">
      <PageHeader
        title={selectedWorklog.title}
        description="업무 상세, 의존성, 파일, AI 상태를 함께 확인합니다."
        actions={
          canEditSelectedWorklog(user?.userId, selectedWorklog) ? (
            <Button
              asChild
              variant="outline"
              className="h-12 min-w-32 px-6 text-sm font-semibold"
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
        transitionNotice={notice}
      />
    </div>
  )
}

function canEditSelectedWorklog(userId: number | undefined, worklog: Worklog) {
  return userId === worklog.authorId
}

function canTransitionSelectedWorklog(
  user: ReturnType<typeof useAuth>["user"],
  worklog: Worklog
) {
  if (!user) return false
  if (user.userId === worklog.authorId) return true
  return user.teams.some((team) => team.teamId === worklog.teamId && team.isLeader)
}
