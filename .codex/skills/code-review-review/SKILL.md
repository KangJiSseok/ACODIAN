---
name: code-review-review
description: 평가기준/설계의도/PR본문/diff를 입력으로 근거 기반 코드리뷰를 수행. p1~p4 우선순위 분류
---

# Code Review — Review Execution

평가기준, 설계의도, PR 본문, git diff를 입력받아 **근거 기반** 코드리뷰를 수행한다.

## When to Use

- `$code-review-auto` 파이프라인의 Step 5로 호출될 때
- 독립적으로 코드리뷰만 실행하고 싶을 때 (`$code-review-review`)
- 사전에 `design-intent.md`와 `code-quality-guide.md`가 준비된 feature branch

## Execution Mode

이 skill은 **사용자 확인 없이 자동 진행된다.** 반드시 `delegate()`로 code-reviewer subagent에 위임한다. 토큰 비용이 가장 큰 단계이므로 격리가 필수다:

```
delegate(
  role="code-reviewer",
  tier="THOROUGH",
  task="EVIDENCE-BASED CODE REVIEW

이 feature branch의 코드리뷰를 수행하라.

입력 (모두 필수):
- .omx/review-artifacts/{branch-name}/code-quality-guide.md
- .omx/review-artifacts/{branch-name}/design-intent.md
- .omx/review-artifacts/{branch-name}/pr-body.md
- git diff <base>...HEAD (변경 코드 전체)

리뷰 원칙:
1. 의도를 깊이 이해한 뒤 비판적으로 검증한다.
   - design-intent.md와 pr-body.md로 작성자의 의도를 먼저 파악한다.
   - 그 의도가 코드에 정확히 반영되었는지 비판적 관점에서 검증한다.
2. 모든 코멘트는 code-quality-guide.md의 기준에 근거해야 한다.
   - 근거 없는 취향 리뷰 금지.
   - 각 코멘트에 어떤 ADR/convention 항목이 근거인지 명시한다.
3. 의도적 결정을 존중한다.
   - design-intent.md에 명시된 결정에 대해 '왜 이렇게 했나요?'라고 묻지 않는다.
   - 단, 의도와 실제 구현이 불일치하면 반드시 지적한다.
4. 우선순위 분류:
   - [p1] 반드시 수정. 버그, 기준 위반, 의도-구현 불일치 등.
   - [p2] 강력 권장. 유지보수성/가독성에 유의미한 영향.
   - [p3] 권장. 개선하면 좋지만 동작에는 문제 없음.
   - [p4] 사소한 개선. 네이밍/포맷팅 등.
5. 변경 제안 시 side effect를 반드시 함께 설명한다.
   - 리뷰는 무조건 반영해야 하는 것이 아니다. 구현자가 트레이드오프를 판단할 수 있도록 충분한 정보를 제공한다.
6. 파일 경로와 라인 번호를 명시한다.

출력:
- .omx/review-artifacts/{branch-name}/review-comments.md 파일 생성
- 아래 'Document Structure' 섹션의 형식을 따른다
- 리더 반환 payload:
  - artifact_path
  - status
  - summary (severity count + 최대 5 bullet)
  - open_questions (원칙적으로 비움. 꼭 필요한 경우만 최대 5 bullet)

중요:
- 소스코드를 수정하지 마라. 리뷰 코멘트 파일만 생성한다.
- 사용자에게 중간 확인을 받지 마라. 끝까지 작성하고 결과만 보고한다.
"
)
```

## 절차 (리더 세션 관점)

1. 사전 산출물(`code-quality-guide.md`, `design-intent.md`, `pr-body.md`)이 모두 존재하는지 확인한다.
2. 누락된 산출물이 있으면 사용자에게 알리고 해당 단계 skill을 먼저 실행하도록 안내한다.
3. **code-reviewer subagent**에 리뷰를 위임한다 (사용자 확인 없음).
4. subagent가 반환한 severity 요약과 artifact 경로만 사용자에게 보고한다.

## Output Path

```
.omx/review-artifacts/{branch-name}/review-comments.md
```

## Document Structure

```markdown
# Code Review

## Summary
(전체 코드 품질에 대한 간략한 총평)

## Comments

### [p1] {파일경로}:{라인}
- 근거: {code-quality-guide.md의 어떤 기준 — ADR-XXX 또는 GEN-XXX/SB-XXX}
- 내용: ...
- 제안: ...
- side effect: (이 변경을 적용할 때 발생할 수 있는 부수 효과. 없으면 "없음")
- 제안 이유: (side effect에도 불구하고 이 방향을 제안하는 이유)

### [p2] {파일경로}:{라인}
- 근거: ...
- 내용: ...
- 제안: ...
- side effect: ...
- 제안 이유: ...

### [p3] {파일경로}:{라인}
- 근거: ...
- 내용: ...
- 제안: ...
- side effect: ...

### [p4] {파일경로}:{라인}
- 내용: ...
- 제안: ...
```

## Context Isolation Rationale

리뷰 단계는 파이프라인에서 **가장 큰 컨텍스트**(평가기준 + 설계의도 + PR본문 + diff 전체)를 동시에 다룬다. 리더 세션에서 직접 수행하면 이후 단계(반영, 후속 작업)에서 컨텍스트 한계에 부딪힌다.

`tier="THOROUGH"`로 위임하여 code-reviewer subagent의 격리된 컨텍스트에서 리뷰를 완수하고, 리더에게는 **severity 요약과 산출물 경로**만 반환하도록 한다. full comment body는 `review-comments.md`에만 남긴다.
