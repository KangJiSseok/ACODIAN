---
name: code-review-intent
description: 설계의도 문서를 작성. 요구사항, 설계 결정, 트레이드오프, 의도적 제외를 정리한다
---

# Code Review — Design Intent

현재 대화에서 논의된 설계 내용을 기반으로 설계의도 문서를 작성한다.

## When to Use

- `$code-review-auto` 파이프라인의 Step 2로 호출될 때
- 독립적으로 설계의도 문서만 작성하고 싶을 때 (`$code-review-intent`)
- 작업자/리뷰어 간 의도 공유가 필요한 PR을 준비할 때

## Execution Mode

이 skill은 **리더 세션에서 직접 수행하지 않는다.** 반드시 `delegate()`로 analyst subagent에 위임한다:

```
delegate(
  role="analyst",
  tier="STANDARD",
  task="DESIGN INTENT DOCUMENTATION

현재 feature branch의 설계의도 문서를 작성하라.

입력:
- 현재 대화에서 논의된 요구사항, 설계 결정, 트레이드오프
- git diff <base>...HEAD (구현 결과를 통한 설계 추론)
- 필요 시 파이프라인 상태의 docs/{module}/code-convention.yaml, docs/{module}/adr.yaml 참조 (경로는 리더가 전달)

출력:
- .omx/review-artifacts/{branch-name}/design-intent.md 초안
- 모호하거나 구체화가 필요한 논의점 목록 (예: A방식 vs B방식, 예외 케이스 의도 여부)
- 리더 반환 payload:
  - artifact_path
  - status
  - summary (최대 5 bullet)
  - open_questions (최대 5 bullet)

중요:
- 소스코드를 수정하지 마라. 산출물 파일만 생성한다.
- 논의점 없이 넘어가지 마라. 의도가 불분명하면 반드시 사용자에게 질문할 항목으로 정리하라.
"
)
```

## 절차

subagent는 다음을 수행한다:

1. 현재 컨텍스트에서 논의된 요구사항, 설계 결정, 트레이드오프를 분석한다.
2. 아래 구조에 맞춰 `design-intent.md` 초안을 작성한다.
3. 초안을 산출물 경로에 저장한 뒤, 리더에게는 **모호하거나 구체화가 필요한 논의점**과 짧은 summary만 반환한다.
   - 예: "A 방식과 B 방식 중 어느 쪽 의도인지?"
   - 예: "이 예외 케이스를 의도적으로 제외한 건지?"
4. 리더는 논의점을 사용자에게 전달하고 피드백을 수집한다.
5. 피드백을 반영하여 subagent(또는 리더)가 문서를 확정하고 산출물 경로에 저장한다.

## Output Path

```
.omx/review-artifacts/{branch-name}/design-intent.md
```

`{branch-name}`은 `git branch --show-current`로 resolve한다.

## Document Structure

```markdown
# 설계의도

## 작업 개요
(이 작업이 무엇을 해결하는가)

## 핵심 설계 결정
(어떤 선택을 했고, 왜 그 선택이 최적인가)

### 결정 1: ...
- 선택지: A vs B
- 선택: A
- 이유: ...

### 결정 2: ...
- ...

## 의도적으로 제외한 것
(이번 작업 범위에서 의도적으로 빠진 것과 그 이유)

## 주의사항
(이 설계를 구현/리뷰할 때 특히 신경 써야 할 점)
```

## Context Isolation Note

리더가 이 skill을 직접 실행하면 이후 단계(평가기준, PR본문, 리뷰)가 전부 리더 컨텍스트에 쌓여 토큰 낭비와 혼선이 발생한다. 반드시 `delegate()`로 위임하여 subagent의 격리된 컨텍스트 안에서 설계의도 작성만 수행하고, 리더에게는 전문이 아닌 bounded payload만 반환하도록 한다.
