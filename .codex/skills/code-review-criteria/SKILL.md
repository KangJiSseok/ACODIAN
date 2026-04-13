---
name: code-review-criteria
description: 평가기준 문서 자동 생성. ADR 추출 + code-convention 필터링을 거쳐 작업별 기준을 만든다
---

# Code Review — Evaluation Criteria

작업 설계를 분석하고, `docs/{module}/adr.yaml`에서 관련 항목을 추출하여 `docs/{module}/code-convention.yaml`과 병합된 평가기준 문서를 생성한다.

`{module}`은 `$code-review-auto` Step 1의 모듈 자동 감지 결과 또는 사용자가 명시한 값이다. (예: `api`, `web`, `infra`, `ai`)

## When to Use

- `$code-review-auto` 파이프라인의 Step 3로 호출될 때
- 독립적으로 평가기준만 생성하고 싶을 때 (`$code-review-criteria`)
- 큰 PR을 리뷰하기 전에 "이번 작업에 적용되는 ADR이 무엇인지" 정리가 필요할 때

## Execution Mode

이 skill은 **2-stage delegation**으로 동작한다. 리더 세션은 최종 머지만 담당한다.

- **Stage 1 (ADR 추출)**: full ADR bleed를 막기 위해 **격리된 subagent**로 실행한다.
- **Stage 2 (평가기준 머지)**: 코드 컨벤션과 작업 설계 요약을 현재 파이프라인 문맥과 함께 다뤄야 하므로 **forked context subagent**로 실행한다.

### Stage 1: ADR 추출 (격리된 subagent)

```
delegate(
  role="architect",
  tier="THOROUGH",
  task="ADR EXTRACTION

이번 작업의 설계 내용과 관련된 ADR 항목만 추출하라.

입력:
- 현재 작업의 설계 내용 (리더가 요약 전달)
- {adr_path} 전문  ← 파이프라인 상태에서 전달된 경로 (예: docs/api/adr.yaml)
  크로스 모듈 PR이면 각 모듈의 adr.yaml을 모두 전달한다.

절차:
1. stacks 필드를 1차 필터로 활용한다.
2. context/decision 내용을 2차 판단 기준으로 사용한다.
3. 관련 없는 항목은 제외한다.
4. 관련 있는 항목은 이 작업에 어떻게 적용되는지 구체적으로 서술한다.

출력 (JSON 또는 markdown):
- 관련 ADR id 목록
- 각 ADR이 이 작업에 어떻게 적용되는지의 구체 설명

중요: adr.yaml 전체를 그대로 옮기지 마라. 추출한 항목과 적용 방법만 반환한다.
"
)
```

### Stage 2: 평가기준 머지 (forked context subagent)

Stage 1의 결과를 입력으로 받아 code-convention과 결합한다:

```
delegate(
  role="quality-strategist",
  tier="STANDARD",
  fork_context=true,
  task="CODE QUALITY GUIDE MERGE

다음 입력을 결합하여 code-quality-guide.md를 작성하라.

입력:
- Stage 1에서 추출된 관련 ADR 항목과 적용 방법
- {convention_path}에서 이번 작업의 stacks와 일치하는 항목만 필터링한 결과  ← 파이프라인 상태에서 전달된 경로 (예: docs/api/code-convention.yaml)
  크로스 모듈 PR이면 각 모듈의 code-convention.yaml을 모두 전달한다.
- 작업 설계 요약

절차:
1. 공통 기준 섹션: code-convention에서 필터링된 항목.
2. 작업별 기준 섹션: Stage 1의 ADR 적용 항목.
3. 기준 적용 범위가 모호한 항목은 '논의점' 섹션에 따로 정리한다.

출력:
- .omx/review-artifacts/{branch-name}/code-quality-guide.md 초안
- 논의점 목록 (예: 'ADR-003이 이 작업에 해당하는지?', '이 convention 항목의 적용 강도는?')
- 리더 반환 payload:
  - artifact_path
  - status
  - summary (최대 5 bullet)
  - open_questions (최대 5 bullet)
"
)
```

## 절차 (리더 세션 관점)

1. 파이프라인 상태에서 감지된 모듈 경로(`{adr_path}`, `{convention_path}`)를 확인한다. 독립 실행 시 사용자에게 모듈을 확인한다.
2. 현재 컨텍스트에서 작업의 설계 내용과 범위를 한 문단으로 요약한다.
3. **Stage 1 subagent**를 호출하여 ADR 추출을 위임한다.
4. Stage 1 결과를 받아 **Stage 2 subagent**에 전달하고 머지를 위임한다.
5. Stage 2가 산출물을 저장하면, 리더는 bounded payload의 summary + 논의점만 사용자에게 제시한다.
6. 사용자 피드백을 반영하여 확정한다.
7. 확정된 문서를 산출물 경로에 저장한다.

## Output Path

```
.omx/review-artifacts/{branch-name}/code-quality-guide.md
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

ADR 전문(예: 50개 ADR × 평균 800자 = 40KB)을 리더 컨텍스트에 직접 주입하면, 이후 단계(PR 본문, 리뷰)에서 ADR 토큰이 계속 쌓여 컨텍스트가 빠르게 오염된다. 따라서 ADR 분석은 Stage 1 subagent에 격리하고, 리더에게는 **추출 결과와 짧은 요약만** 전달한다.

반면 code-convention 머지 단계는 이미 정제된 ADR 추출 결과와 현재 파이프라인의 설계 요약, 사용자 피드백 문맥을 함께 다루는 편이 정확도가 높다. 그래서 Stage 2는 **forked context subagent**로 실행하되, 출력은 계속 bounded payload와 산출물 파일로 제한한다.
