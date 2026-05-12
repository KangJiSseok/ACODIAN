import { tags } from "../../worklog/_mock/worklog.mock"
import type { TagItem } from "../_types/tag.types"

const displayTags: TagItem[] = [
  ...tags,
  {
    id: 5,
    name: "요구사항정의",
    usageCount: 9,
    category: "업무",
    source: "MANUAL",
    mergeState: "REVIEW",
    reuseHint: "기획 문서/프로젝트 착수 태그와 통합 검토 중입니다.",
  },
  {
    id: 6,
    name: "솔루션개발사업부",
    usageCount: 17,
    category: "부서",
    source: "AI",
    mergeState: "ACTIVE",
    reuseHint: "부서 컨텍스트 추론 시 자동 부여됩니다.",
  },
  {
    id: 7,
    name: "문서자동화",
    usageCount: 7,
    category: "업무",
    source: "AI",
    mergeState: "MERGE_CANDIDATE",
    reuseHint: "업무자동화 태그와 유사해 병합 후보로 표시됩니다.",
    mergeTargetId: 4,
  },
]

export const tagService = {
  list(): TagItem[] {
    return [...displayTags]
  },
}
