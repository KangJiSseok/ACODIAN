---
name: code-review-criteria
description: 평가기준 문서 자동 생성. ADR 추출 + code-convention 필터링을 거쳐 작업별 기준을 만든다
---

# Code Review — Evaluation Criteria

작업 설계를 분석하고, `docs/{module}/adr.yaml`에서 관련 항목을 추출하여 `docs/{module}/code-convention.yaml`과 병합된 평가기준 문서를 생성한다.

`{module}`은 사용자가 명시하거나 파이프라인 상태에서 전달된 값이다. (예: `api`, `web`, `infra`, `ai`)

## When to Use

- 독립적으로 평가기준만 생성하고 싶을 때 (`$code-review-criteria`)
- 큰 PR을 리뷰하기 전에 "이번 작업에 적용되는 ADR이 무엇인지" 정리가 필요할 때

## Invocation Authority

- 사용자가 `$code-review-criteria`를 명시 호출하면, 이 skill 내부의 2-stage `spawn_agent()` 호출은 이미 승인된 것으로 간주한다.
- 리더는 explicit skill invocation 이후에도 subagent 사용을 재승인 대상으로 취급하지 않는다.

> **파이프라인 실행 중에는** `$code-review-auto`의 Step 3에서 이 skill이 순차적으로 호출된다. 2-stage 실행과 산출물 구조는 이 문서를 따른다.

## Execution Mode (독립 실행 시)

이 skill은 **2-stage `spawn_agent()`**로 동작한다. 리더 세션은 최종 머지 조율만 담당하며, 두 단계 모두 대화 전체를 포크하지 않고 정제된 입력만 넘긴다.

- **Stage 1 (ADR 추출)**: full ADR bleed를 막기 위해 **isolated subagent** (`fork_context=false`)
- **Stage 2 (평가기준 머지)**: Stage 1 결과와 설계 brief를 결합하는 **isolated subagent** (`fork_context=false`)

### Stage 1: ADR 추출 (isolated)

```
spawn_agent(
  role="architect",
  tier="THOROUGH",
  fork_context=false,
  task="ADR EXTRACTION

이번 작업의 설계 내용과 관련된 ADR 항목만 추출하라.

입력:
- 현재 작업의 설계 내용 (리더가 요약 전달)
- {adr_path} 전문 (예: docs/api/adr.yaml). 크로스 모듈이면 모든 모듈의 adr.yaml을 전달.

절차:
1. stacks 필드를 1차 필터로 활용한다.
2. context/decision 내용을 2차 판단 기준으로 사용한다.
3. 관련 없는 항목은 제외한다.
4. 관련 있는 항목은 이 작업에 어떻게 적용되는지 구체적으로 서술한다.

출력 (JSON 또는 markdown):
- 관련 ADR id 목록
- 각 ADR이 이 작업에 어떻게 적용되는지의 구체 설명

중요: adr.yaml 전체를 그대로 옮기지 마라. 추출 항목과 적용 방법만 반환한다.
"
)
```

### Stage 2: 평가기준 머지 (isolated)

Stage 1의 결과를 입력으로 받아 code-convention과 결합한다:

```
spawn_agent(
  role="quality-strategist",
  tier="STANDARD",
  fork_context=false,
  task="CODE QUALITY GUIDE MERGE

다음 입력을 결합하여 code-quality-guide.md를 작성하라.

입력:
- Stage 1에서 추출된 관련 ADR 항목과 적용 방법
- {convention_path}에서 이번 작업의 stacks와 일치하는 항목만 필터링한 결과 (예: docs/api/code-convention.yaml). 크로스 모듈이면 모든 모듈의 code-convention.yaml을 전달.
- 작업 설계 요약 또는 design-intent.md 핵심 요약

절차:
1. 공통 기준 섹션: code-convention에서 필터링된 항목.
2. 작업별 기준 섹션: Stage 1의 ADR 적용 항목.
3. 기준 적용 범위가 모호한 항목은 '논의점' 섹션에 따로 정리한다.

출력:
- .codex/review-artifacts/{branch-name}/code-quality-guide.md 초안
- 논의점 목록 (예: 'ADR-003이 이 작업에 해당하는지?', '이 convention 항목의 적용 강도는?')
- 리더 반환 payload: {artifact_path, status, summary(≤5), open_questions(≤5)}

문서 구조: 이 SKILL.md 'Document Structure' 섹션 참조.
"
)
```

## 절차 (리더 세션 관점)

1. 사용자에게 대상 모듈과 해당 `docs/{module}/adr.yaml`, `docs/{module}/code-convention.yaml` 경로를 확인한다.
2. 작업의 설계 내용과 범위를 한 문단 brief로 요약한다.
3. **Stage 1 `spawn_agent()`**를 호출하여 ADR 추출을 수행한다.
4. Stage 1 결과를 받아 **Stage 2 `spawn_agent()`**에 전달하고 머지를 수행한다.
5. Stage 2가 산출물을 저장하면, 리더는 bounded payload의 summary + 논의점만 사용자에게 제시한다.
6. 사용자 피드백을 반영하여 확정한다.
7. 확정된 문서를 산출물 경로에 저장한다.

## Output Path

```
.codex/review-artifacts/{branch-name}/code-quality-guide.md
```

## Document Structure

```markdown
# 평가기준

## 공통 기준 (Code Convention)
(docs/{module}/code-convention.yaml에서 가져온 항목 — 이번 작업의 stacks에 해당하는 것만)

- `[GEN-001]` 함수는 단일 책임 원칙을 따른다. 50줄을 넘으면 분리를 검토한다.
- ...

## 이 작업에 적용되는 ADR
(Stage 1 subagent가 추출한 관련 ADR 항목 + 구체적 적용 방법)

### ADR-XXX: {제목}
- 결정 요지: ...
- 이 작업에 적용되는 방법: ...
- 위반 시 신호: ...

## 논의점
(기준 적용 범위가 모호한 항목 — 사용자 확인 필요)

- ...
```

## Context Isolation Rationale

ADR 전문(예: 50개 ADR × 평균 800자 = 40KB)을 리더 컨텍스트에 직접 주입하면, 이후 작업에서 ADR 토큰이 계속 쌓여 컨텍스트가 빠르게 오염된다. 따라서 ADR 분석은 Stage 1 **isolated subagent**에 격리하고, 리더에게는 **추출 결과와 짧은 요약만** 전달한다.

반면 code-convention 머지 단계는 이미 정제된 ADR 추출 결과와 설계 brief만 있으면 충분하다. 그래서 Stage 2도 **fork_context=false**로 유지하고, 출력은 계속 bounded payload와 산출물 파일로 제한한다.
