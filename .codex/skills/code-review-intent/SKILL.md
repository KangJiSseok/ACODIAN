---
name: code-review-intent
description: 설계의도 문서를 작성. 요구사항, 설계 결정, 트레이드오프, 의도적 제외를 정리한다
---

# Code Review — Design Intent

리더가 정리한 작업 brief와 git diff를 기반으로 설계의도 문서를 작성한다.

## When to Use

- 독립적으로 설계의도 문서만 작성하고 싶을 때 (`$code-review-intent`)
- 작업자/리뷰어 간 의도 공유가 필요한 PR을 준비할 때

## Invocation Authority

- 사용자가 `$code-review-intent`를 명시 호출하면, 이 skill 내부의 `spawn_agent()` 호출은 이미 승인된 것으로 간주한다.
- 리더는 explicit skill invocation 이후에도 subagent 사용 승인을 다시 요구하거나, 보수적 해석만으로 직접 수행으로 강등하지 않는다.

> **파이프라인 실행 중에는** `$code-review-auto`의 Step 2에서 이 skill이 순차적으로 호출된다. agent 실행 세부와 산출물 구조는 이 문서를 따른다.

## Execution Mode (독립 실행 시)

리더는 아래 `spawn_agent()`를 **직접 호출**하여 writer subagent에 위임한다. 설계의도 단계는 대화 전체를 포크하기보다, 요구사항/설계 결정/트레이드오프를 짧은 brief로 정리해 typed agent에 전달하는 것을 기본값으로 한다:

```
spawn_agent(
  role="writer",
  tier="STANDARD",
  fork_context=false,
  task="DESIGN INTENT DOCUMENTATION

현재 feature branch의 설계의도 문서를 작성하라.

입력:
- 작업 brief (리더가 5~10줄로 정리한 요구사항, 설계 결정, 트레이드오프)
- git diff <base>...HEAD (구현 결과를 통한 설계 추론)
- 필요 시 docs/{module}/code-convention.yaml, docs/{module}/adr.yaml 참조 (경로는 리더가 전달)

출력:
- .codex/review-artifacts/{branch-name}/design-intent.md 초안
- 모호하거나 구체화가 필요한 논의점 목록 (예: A방식 vs B방식, 예외 케이스 의도 여부)
- 리더 반환 payload: {artifact_path, status, summary(≤5), open_questions(≤5)}

문서 구조: 이 SKILL.md 'Document Structure' 섹션 참조.

중요:
- 소스코드를 수정하지 마라. 산출물 파일만 생성한다.
- 논의점 없이 넘어가지 마라. 의도가 불분명하면 반드시 사용자에게 질문할 항목으로 정리하라.
"
)
```

## 절차 (리더 세션 관점)

1. 리더가 branch/base/변경 모듈과 작업 brief를 먼저 정리한 뒤, 위 `spawn_agent()`를 호출한다.
2. subagent가 `design-intent.md` 초안을 산출물 경로에 저장한 뒤, 리더에게는 **모호하거나 구체화가 필요한 논의점**과 짧은 summary만 반환한다.
   - 예: "A 방식과 B 방식 중 어느 쪽 의도인지?"
   - 예: "이 예외 케이스를 의도적으로 제외한 건지?"
3. 리더는 논의점을 사용자에게 전달하고 피드백을 수집한다.
4. 피드백을 반영하여 subagent를 재호출해 문서를 확정하고 산출물 경로에 저장한다.

## Output Path

```
.codex/review-artifacts/{branch-name}/design-intent.md
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

리더가 이 작업을 직접 수행하면 이후 작업(평가기준, PR본문, 리뷰)이 전부 리더 컨텍스트에 쌓여 토큰 낭비와 혼선이 발생한다. 따라서 설계의도 작성은 `fork_context=true` 대신 **typed writer + structured brief + diff** 조합으로 위임하고, 결과 전달은 bounded payload로 제한한다. 입력은 구조화하고, 출력은 artifact 경로와 논의점 중심으로 좁게 유지한다.
