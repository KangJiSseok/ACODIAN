import { TagMergeList } from "../_components/tagMergeList"

export default function TagMergePage() {
  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
          태그 병합
        </h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          매일 자정 자동 생성된 태그 병합 후보를 확인합니다. 대표 태그와
          합쳐질 태그를 검토한 뒤 후보를 수정하거나 병합할 수 있으며, 병합 후
          업무일지에 연결된 태그는 대표 태그 기준으로 정리됩니다.
        </p>
      </div>

      <TagMergeList />
    </section>
  )
}
