---
name: code-review-docs
description: docs/{module}/code-convention.yaml과 docs/{module}/adr.yaml 항목 추가/수정/삭제. 두 문서 간 충돌과 연쇄 수정을 검사
---

# Code Review — Docs Update

`docs/{module}/code-convention.yaml`과 `docs/{module}/adr.yaml`에 항목을 추가/수정/삭제한다. 두 문서 사이의 **충돌**과 **연쇄 수정** 필요성을 함께 검사한다.

`{module}`은 대상 모듈을 의미한다. (예: `api`, `web`, `infra`, `ai`)

## When to Use

- 새 ADR을 채택했을 때 (`$code-review-docs adr --module api`)
- 새 코딩 컨벤션 규칙을 정했을 때 (`$code-review-docs convention --module api`)
- 기존 결정이 폐기/대체되었을 때
- 인자 없이 호출하면 두 문서 모두를 업데이트한다 (`$code-review-docs --module api`)

## Execution Mode

이 skill은 **충돌 분석이 핵심**이므로 격리된 컨텍스트에서 수행한다. 리더는 `spawn_agent()`를 **직접 호출**하여 executor subagent에 위임한다:

```
spawn_agent(
  role="executor",
  tier="THOROUGH",
  fork_context=false,
  task="DOCS UPDATE — convention/adr

docs/{module}/code-convention.yaml 또는 docs/{module}/adr.yaml에 항목을 추가/수정/삭제한다.
{module}은 리더가 전달한다. (예: api, web, infra, ai)

입력:
- 현재 작업의 의사결정 요약 (리더가 전달)
- {module} (리더가 전달)
- docs/{module}/code-convention.yaml 전문
- docs/{module}/adr.yaml 전문
- 업데이트 대상: convention | adr | both

절차 (반드시 두 문서 모두 먼저 읽는다):
1. docs/{module}/code-convention.yaml과 docs/{module}/adr.yaml을 모두 읽는다.
2. 현재 작업의 의사결정과 기존 항목을 대조하여 다음을 판단한다:
   - 충돌 항목: 새 결정이 기존 항목과 모순되는가?
     예) 새 ADR이 '에러를 중앙에서 처리'로 결정 → convention에 '에러는 발생 지점에서 처리'가 남아있으면 충돌
   - 연쇄 수정: 한 문서의 변경이 다른 문서 항목에 영향을 미치는가?
     예) ADR에서 새 기술 결정 → 그 결정과 반대되는 convention 규칙도 수정/삭제 필요
   - 폐기 대상: 이번 변경으로 더 이상 유효하지 않은 항목이 있는가?
3. 분석 결과를 사용자에게 '변경 제안 목록'으로 제시한다.
   - MUST: 충돌/모순으로 반드시 수정해야 하는 항목
   - RECOMMENDED: 일관성/명확성을 위해 권장되는 수정
4. 사용자 확정 후 항목을 추가/수정/삭제한다.

스키마:

# docs/{module}/code-convention.yaml
- id: {카테고리}-{번호}    # GEN, SB, NEXT, NEST 등
  rule: ...                 # 명확하고 실행 가능한 규칙
  stacks: [...]             # 적용 대상 스택. 전체 적용이면 [all]

# docs/{module}/adr.yaml
- id: ADR-{번호}
  title: ...
  status: adopted | deprecated | superseded
  date: YYYY-MM-DD
  stacks: [...]
  context: |
    (구체적인 상황 — 가이드라인 참조)
  decision: |
    ...
  consequence: ...

context 작성 가이드라인:
- context만 읽고도 '왜 이 결정이 필요했는가'를 구체적으로 연상할 수 있어야 한다.
- 나쁜 예: '에러 처리가 산발적이라 비일관적'
- 좋은 예: '각 서비스 레이어에서 에러를 자체적으로 catch하여 임의 형식으로 반환했다. 어떤 엔드포인트는 { message: \"...\" }, 다른 곳은 { error: \"...\", code: 500 }을 반환해 프론트엔드에서 엔드포인트별로 에러 형식을 확인해야 했다.'
- context가 불충분하면 사용자에게 구체화 방안 논의를 요청하라.

출력:
- 변경 제안 목록 (MUST/RECOMMENDED 분류)
- 사용자 확정 후 두 yaml 파일에 직접 반영
- 변경 요약을 리더에게 보고
"
)
```

## 절차 (리더 세션 관점)

1. 사용자에게 대상 모듈(`api` | `web` | `infra` | `ai` 등), 업데이트 대상(`convention` | `adr` | `both`)과 현재 작업의 의사결정 요약을 확인한다.
2. 리더가 위 `spawn_agent()`를 호출하여 docs 업데이트를 위임한다.
3. subagent가 변경 제안 목록(MUST/RECOMMENDED)을 반환하면 사용자에게 전달한다.
4. 사용자 확정을 받아 subagent에 반영을 지시한다 (재 `spawn_agent()` 또는 리더 직접 수정).
5. 변경 요약을 사용자에게 보고한다.

## Update 대상 명령

```
$code-review-docs convention --module api   # api의 code-convention만
$code-review-docs adr --module api          # api의 adr만
$code-review-docs --module api              # api 두 문서 모두 (충돌/연쇄 수정 분석)
```

`--module`을 생략하면 리더가 사용자에게 모듈을 확인한다.

## 분석 우선 원칙

> 어떤 문서를 업데이트하든, **해당 모듈의 두 문서를 모두 먼저 읽고 분석한다.** 한쪽 문서만 수정하면 충돌과 연쇄 수정을 놓친다.

이 원칙은 위 `spawn_agent()` task에 명시되어 있으며, subagent가 반드시 따라야 한다.

## Context Isolation Rationale

두 yaml 파일을 동시에 컨텍스트에 끌어와 충돌을 분석하면 토큰 비용이 크다. `spawn_agent(role="executor")`에 위임하면 분석과 파일 수정이 격리된 컨텍스트에서 일어나고, 리더에게는 **변경 제안 목록과 최종 변경 요약**만 전달된다.

이 skill은 다른 code-review-* skill과 달리 산출물을 `.codex/review-artifacts/`에 저장하지 않는다. 직접 `docs/{module}/*.yaml`을 수정한다는 점에 주의하라.
