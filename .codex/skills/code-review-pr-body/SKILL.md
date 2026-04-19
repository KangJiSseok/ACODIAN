---
name: code-review-pr-body
description: PR 본문 자동 생성. git diff 기반으로 Summary, Changes, Breaking Changes, Test Plan을 작성
---

# Code Review — PR Body

base branch 대비 git diff와 산출물 문서를 기반으로 PR 본문 초안을 작성한다.

## When to Use

- 독립적으로 PR 본문만 작성하고 싶을 때 (`$code-review-pr-body`)
- `gh pr create`로 PR을 올리기 직전 본문이 필요한 시점

## Invocation Authority

- 사용자가 `$code-review-pr-body`를 명시 호출하면, 이 skill 내부의 `spawn_agent()` 호출은 이미 승인된 것으로 간주한다.
- 리더는 explicit skill invocation 이후에도 subagent 사용 승인을 다시 요구하거나 직접 수행으로 임의 강등하지 않는다.

> **파이프라인 실행 중에는** `$code-review-auto`의 Step 4에서 이 skill이 순차적으로 호출된다. agent 실행 세부와 산출물 구조는 이 문서를 따른다.

## Execution Mode (독립 실행 시)

리더는 아래 `spawn_agent()`를 **직접 호출**하여 writer subagent에 위임한다. PR 본문 단계는 대화 전체를 포크하기보다, diff와 확정된 artifact, 그리고 논의점 요약만 넘겨 typed agent가 합성하게 하는 것이 기본값이다:

```
spawn_agent(
  role="writer",
  tier="STANDARD",
  fork_context=false,
  task="PR BODY DRAFTING

현재 feature branch의 PR 본문 초안을 작성하라.

입력:
- git diff <base>...HEAD (변경사항)
- git diff --stat <base>...HEAD
- git diff --name-only <base>...HEAD
- .codex/review-artifacts/{branch-name}/design-intent.md (있으면)
- .codex/review-artifacts/{branch-name}/code-quality-guide.md (있으면)
- 사용자와 확정한 논의점 요약 (있으면)

절차:
1. base branch 대비 git diff를 수집한다.
2. 변경된 파일을 모듈/영역 단위로 그룹화한다.
3. Document Structure에 맞춰 PR 본문 초안을 작성한다.
4. design-intent.md가 있다면 'Summary'와 'Related' 섹션에 그 내용을 반영한다.
5. PR 설명에서 명확히 해야 할 논의점을 함께 정리한다.
   - 예: 'breaking change 여부 확인 필요'
   - 예: '마이그레이션 스크립트 필요 여부'
   - 예: '관련 이슈 번호 (Linear/Jira/GitHub Issue)'
   - 예: 'Test Plan에 포함할 수동 검증 시나리오 확정'

출력:
- .codex/review-artifacts/{branch-name}/pr-body.md 초안
- 사용자 확인이 필요한 논의점 목록
- 리더 반환 payload: {artifact_path, status, summary(≤5), open_questions(≤5)}

문서 구조: 이 SKILL.md 'Document Structure' 섹션 참조.

중요:
- 소스코드를 수정하지 마라. 산출물 파일만 생성한다.
- diff에 없는 변경을 추측하여 적지 마라. 실제 변경 내역에 근거하라.
- breaking change가 의심되는 변경(공개 API 시그니처 변경, DB 스키마 변경 등)은 반드시 논의점에 올려라.
"
)
```

## 절차 (리더 세션 관점)

1. base branch를 사용자와 합의한다 (기본: `dev`).
2. 리더가 위 `spawn_agent()`를 호출한다.
3. subagent가 artifact를 저장하고 반환한 summary + 논의점을 사용자에게 제시한다.
4. 사용자 피드백을 반영하여 확정한다.
5. 확정된 문서를 산출물 경로에 저장한다.

## Output Path

```
.codex/review-artifacts/{branch-name}/pr-body.md
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

PR 본문 작성에 git diff 전체(수천 줄 가능)를 리더가 직접 다루면, 이후 단계(리뷰, 반영)에서 컨텍스트가 빠르게 포화된다. 따라서 `fork_context=true` 대신 **typed writer + diff/stat/name-only + 기존 artifact + 논의점 요약** 조합으로 위임하고, 출력은 **artifact 경로, 짧은 요약, 논의점**만 리더에게 전달한다.
