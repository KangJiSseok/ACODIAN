---
name: code-review-pr-body
description: PR 본문 자동 생성. git diff 기반으로 Summary, Changes, Breaking Changes, Test Plan을 작성
---

# Code Review — PR Body

base branch 대비 git diff와 산출물 문서를 기반으로 PR 본문 초안을 작성한다.

## When to Use

- `$code-review-auto` 파이프라인의 Step 4로 호출될 때
- 독립적으로 PR 본문만 작성하고 싶을 때 (`$code-review-pr-body`)
- `gh pr create`로 PR을 올리기 직전 본문이 필요한 시점

## Execution Mode

이 skill은 **리더 세션에서 직접 수행하지 않는다.** 반드시 `delegate()`로 writer subagent에 **forked context** 위임한다. PR 본문 단계는 git diff뿐 아니라 현재 파이프라인에서 확정된 설계 의도와 사용자 피드백 문맥을 함께 반영해야 하므로, 부모 세션의 관련 문맥을 넘기는 것이 기본값이다:

```
delegate(
  role="writer",
  tier="STANDARD",
  fork_context=true,
  task="PR BODY DRAFTING

현재 feature branch의 PR 본문 초안을 작성하라.

입력:
- git diff <base>...HEAD (변경사항)
- .omx/review-artifacts/{branch-name}/design-intent.md (있으면)
- .omx/review-artifacts/{branch-name}/code-quality-guide.md (있으면)

절차:
1. base branch 대비 git diff를 수집한다.
2. 변경된 파일을 모듈/영역 단위로 그룹화한다.
3. 아래 문서 구조에 맞춰 PR 본문 초안을 작성한다.
4. design-intent.md가 있다면 'Summary'와 'Related' 섹션에 그 내용을 반영한다.
5. PR 설명에서 명확히 해야 할 논의점을 함께 정리한다.
   - 예: 'breaking change 여부 확인 필요'
   - 예: '마이그레이션 스크립트 필요 여부'
   - 예: '관련 이슈 번호 (Linear/Jira/GitHub Issue)'
   - 예: 'Test Plan에 포함할 수동 검증 시나리오 확정'

출력:
- .omx/review-artifacts/{branch-name}/pr-body.md 초안
- 사용자 확인이 필요한 논의점 목록
- 리더 반환 payload:
  - artifact_path
  - status
  - summary (최대 5 bullet)
  - open_questions (최대 5 bullet)

중요:
- 소스코드를 수정하지 마라. 산출물 파일만 생성한다.
- diff에 없는 변경을 추측하여 적지 마라. 실제 변경 내역에 근거하라.
- breaking change가 의심되는 변경(공개 API 시그니처 변경, DB 스키마 변경 등)은 반드시 논의점에 올려라.
"
)
```

## 절차 (리더 세션 관점)

1. base branch를 사용자와 합의한다 (이 프로젝트는 보통 `master`).
2. **writer subagent**를 호출하여 PR 본문 작성을 위임한다.
3. subagent가 artifact를 저장하고 반환한 summary + 논의점을 사용자에게 제시한다.
4. 사용자 피드백을 반영하여 확정한다.
5. 확정된 문서를 산출물 경로에 저장한다.

## Output Path

```
.omx/review-artifacts/{branch-name}/pr-body.md
```

## Document Structure

```markdown
# PR: {제목}

## Summary
(이 PR이 해결하는 문제와 접근 방식 요약, 1-3문장)

## Changes
(변경된 파일/모듈 단위로 무엇이 어떻게 바뀌었는지)

### {모듈/영역}
- ...

## Breaking Changes
(없으면 "없음")

## Migration
(DB 마이그레이션, 환경변수 추가, 캐시 invalidation 등 — 없으면 "없음")

## Test Plan
- [ ] ...

## Related
(관련 이슈, ADR, 설계 문서 등)
```

## Context Isolation Note

PR 본문 작성에 git diff 전체(수천 줄 가능)를 리더가 직접 다루면, 이후 단계(리뷰, 반영)에서 컨텍스트가 빠르게 포화된다. 따라서 Step 4는 **부모 문맥을 포크한 writer subagent**에게 맡기되, 결과 전달은 계속 bounded payload로 제한한다. 즉 입력은 forked context + diff + 기존 artifact를 함께 활용하고, 출력은 **artifact 경로, 짧은 요약, 논의점**만 리더에게 전달한다.
