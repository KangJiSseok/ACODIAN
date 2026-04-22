import { users } from "../_mock/worklog.mock"
import type { Worklog, WorklogStatus } from "../_types/worklog.types"
import { AiSummaryCard } from "./aiSummaryCard"
import { DependencyGraph } from "./dependencyGraph"
import { FileAttachment } from "./fileAttachment"
import { ImportanceBadge } from "./importanceBadge"
import { StatusBadge } from "./statusBadge"
import { StatusHistory } from "./statusHistory"
import { StatusTransition } from "./statusTransition"
import { TagList } from "./tagList"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { formatDate, formatDateTime, formatHours } from "../_utils/worklogFormat"

export function WorklogDetail({
  worklog,
  canTransition = false,
  onTransition = async () => {},
  transitionNotice,
}: {
  worklog: Worklog | null
  canTransition?: boolean
  onTransition?: (nextStatus: WorklogStatus, reason: string) => Promise<void>
  transitionNotice?: string
}) {
  if (!worklog) {
    return (
      <Card className="rounded-[28px]">
        <CardContent className="p-8 text-sm text-muted-foreground">
          선택된 업무가 없습니다.
        </CardContent>
      </Card>
    )
  }

  const author = users.find((user) => user.id === worklog.authorId)

  return (
    <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
      <div className="space-y-6">
        <Card>
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between gap-3">
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <StatusBadge status={worklog.status} />
                  <ImportanceBadge importance={worklog.importance} />
                </div>
                <div className="space-y-2">
                  <CardTitle className="text-2xl tracking-[-0.04em]">
                    {worklog.title}
                  </CardTitle>
                  <p className="max-w-4xl text-sm leading-7 text-muted-foreground">
                    {worklog.requestContent || "상위 요청/지시 내용이 아직 입력되지 않았습니다."}
                  </p>
                </div>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4 border-t border-border/70 pt-4">
            <section>
              <p className="text-sm font-medium">업무 내용</p>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">
                {worklog.workContent}
              </p>
            </section>
            <section>
              <p className="text-sm font-medium">메타 태그</p>
              <div className="mt-2">
                <TagList tagIds={worklog.tagIds} />
              </div>
            </section>
          </CardContent>
        </Card>

        <AiSummaryCard worklog={worklog} />

        <Card>
          <CardHeader>
            <CardTitle>첨부 파일</CardTitle>
          </CardHeader>
          <CardContent>
            <FileAttachment fileIds={worklog.fileIds} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>선행 업무</CardTitle>
          </CardHeader>
          <CardContent>
            <DependencyGraph dependencyIds={worklog.dependencyIds} />
          </CardContent>
        </Card>
      </div>

      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>작성자 및 일정</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {author ? (
              <div className="flex items-center gap-3 rounded-xl bg-muted/40 p-4">
                <Avatar className="size-11">
                  <AvatarImage src={author.profileImage} alt={author.name} />
                  <AvatarFallback>{author.name.slice(0, 1)}</AvatarFallback>
                </Avatar>
                <div>
                  <p className="font-medium">{author.name}</p>
                  <p className="text-sm text-muted-foreground">{author.title}</p>
                </div>
              </div>
            ) : null}
            <div className="grid gap-3">
              <InfoRow label="지시일" value={formatDate(worklog.instructionDate)} />
              <InfoRow label="마감일" value={formatDate(worklog.dueDate)} />
              <InfoRow label="실제 업무시간" value={formatHours(worklog.actualHours)} />
              <InfoRow
                label="완료일"
                value={worklog.completionDate ? formatDate(worklog.completionDate) : "-"}
              />
              <InfoRow
                label="AI 수동 편집"
                value={worklog.aiSummaryEdited ? "사용자 수정 완료" : "자동 생성 유지"}
              />
              <InfoRow label="생성일" value={formatDateTime(worklog.createdAt)} />
              <InfoRow label="수정일" value={formatDateTime(worklog.updatedAt)} />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>상태 변경</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <StatusTransition
              worklog={worklog}
              canTransition={canTransition}
              onTransition={onTransition}
            />
            {transitionNotice ? (
              <div className="rounded-xl border border-warning/40 bg-warning/10 px-4 py-3 text-sm text-[color:var(--warning)]">
                {transitionNotice}
              </div>
            ) : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>상태 이력</CardTitle>
          </CardHeader>
          <CardContent>
            <StatusHistory worklog={worklog} />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default WorklogDetail

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-lg bg-muted/40 px-4 py-3 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium">{value}</span>
    </div>
  )
}
