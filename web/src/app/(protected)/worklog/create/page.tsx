"use client"

import { useRouter } from "next/navigation"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { WorklogForm } from "../_components/worklogForm"
import { worklogService } from "../_service/worklog.service"

export default function WorklogCreatePage() {
  const router = useRouter()

  return (
    <div className="flex flex-col gap-5 lg:gap-6">
      <PageHeader title="업무 등록" />
      <WorklogForm
        submitLabel="업무 생성"
        currentWorklogId={undefined}
        onSubmit={async (values) => {
          const created = await worklogService.create(values)
          router.push(`/worklog/detail/${created.id}`)
        }}
      />
    </div>
  )
}
