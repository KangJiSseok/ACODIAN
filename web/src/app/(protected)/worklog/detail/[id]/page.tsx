"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import {
  canEditWorklog,
  canTransitionWorklog,
} from "../../_utils/accessControl"
import { useAuth } from "../../_hooks/useAuth"
import { subscribeMockDb } from "../../_mock/worklog.mock"
import { WorklogDetail } from "../../_components/worklogDetail"
import { worklogService } from "../../_service/worklog.service"
import { Button } from "@/components/ui/button"

export default function WorklogDetailPage() {
  const params = useParams<{ id: string }>()
  const { user } = useAuth()
  const [worklog, setWorklog] = useState<
    Awaited<ReturnType<typeof worklogService.getById>>
  >()
  const [isLoading, setIsLoading] = useState(true)
  const [transitionNotice, setTransitionNotice] = useState("")

  useEffect(() => {
    const sync = async () => {
      setIsLoading(true)
      setWorklog(await worklogService.getById(Number(params.id)))
      setIsLoading(false)
    }

    void sync()
    return subscribeMockDb(() => {
      void sync()
    })
  }, [params.id])

  if (isLoading) return <div>업무를 불러오는 중입니다.</div>
  if (!worklog) return <div>업무를 찾을 수 없습니다.</div>

  return (
    <div className="flex flex-col gap-5 lg:gap-6">
      <PageHeader
        title={worklog.title}
        description="업무 상세, 의존성, 파일, AI 상태를 함께 확인합니다."
        actions={
          canEditWorklog(user, worklog) ? (
            <Button asChild variant="outline">
              <Link href={`/worklog/edit/${worklog.id}`}>업무 수정</Link>
            </Button>
          ) : null
        }
      />
      <WorklogDetail
        worklog={worklog}
        canTransition={canTransitionWorklog(user, worklog)}
        transitionNotice={transitionNotice}
        onTransition={async (nextStatus, reason) => {
          const result = await worklogService.transitionStatus(
            worklog.id,
            nextStatus,
            user?.id ?? worklog.authorId,
            reason
          )
          setTransitionNotice(result.warning ?? "")
        }}
      />
    </div>
  )
}
