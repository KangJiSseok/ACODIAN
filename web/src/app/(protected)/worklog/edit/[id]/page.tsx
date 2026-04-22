"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { files, subscribeMockDb, worklogs } from "../../_mock/worklog.mock"
import { WorklogForm } from "../../_components/worklogForm"
import { worklogService } from "../../_service/worklog.service"

export default function WorklogEditPage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const [worklog, setWorklog] = useState(
    () => worklogs.find((item) => item.id === Number(params.id))
  )
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const sync = () => {
      setIsLoading(true)
      setWorklog(worklogs.find((item) => item.id === Number(params.id)))
      setIsLoading(false)
    }

    sync()
    return subscribeMockDb(sync)
  }, [params.id])

  if (isLoading) return <div>업무를 불러오는 중입니다.</div>
  if (!worklog) return <div>업무를 찾을 수 없습니다.</div>

  return (
    <div className="flex flex-col gap-5 lg:gap-6">
      <PageHeader title={`${worklog.title} 수정`} />
      <WorklogForm
        initialValues={{
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
          attachmentNames: files
            .filter((file) => worklog.fileIds.includes(file.id) && !file.isDeleted)
            .map((file) => file.originalName),
          tagIds: worklog.tagIds,
          aiSummary: worklog.aiSummary,
          aiSummaryEdited: worklog.aiSummaryEdited,
          aiRegenerateRequested: false,
        }}
        currentWorklogId={worklog.id}
        submitLabel="수정 저장"
        onSubmit={async (values) => {
          await worklogService.update(worklog.id, values)
          router.push(`/worklog/detail/${worklog.id}`)
        }}
      />
    </div>
  )
}
