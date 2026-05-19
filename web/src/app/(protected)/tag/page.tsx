import { TagManager } from "./_components/tagManager"

export default function TagPage() {
  return (
    <section className="space-y-6">
      <div>
        <div>
          <h1 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            태그 목록
          </h1>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            업무일지와 파일에서 사용하는 메타 태그를 검색하고 사용 현황을
            확인합니다.
          </p>
        </div>
      </div>

      <TagManager />
    </section>
  )
}
