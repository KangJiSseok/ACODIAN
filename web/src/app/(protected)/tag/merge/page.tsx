import { TagMergeList } from "../_components/tagMergeList"

export default function TagMergePage() {
  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
          태그 병합
        </h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          백엔드가 추천한 태그 병합 후보를 병합될 태그명과 합쳐질 태그들로
          확인합니다. 태그 병합 후보는 매일 새벽 스케줄링을 통해 자동으로
          생성됩니다.
        </p>
      </div>

      <TagMergeList />
    </section>
  )
}
