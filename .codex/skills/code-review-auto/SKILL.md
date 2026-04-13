---
name: code-review-auto
description: 코드리뷰 전체 파이프라인 오케스트레이션 — 설계의도, 평가기준, PR본문, 리뷰, 반영을 순차 실행
---

# Code Review Auto Pipeline

전체 코드리뷰 파이프라인을 오케스트레이션한다. 각 단계는 **`delegate()`로 specialist subagent에 위임**하고, 단계 간 문맥은 artifact로 넘겨 리더 세션의 LLM 컨텍스트 오염을 최소화한다.

## When to Use

이 skill은 다음 상황에서 활성화된다:

- 구현이 완료된 feature branch에서 전체 리뷰 파이프라인을 한 번에 돌리고 싶을 때
- 사용자가 "코드리뷰 파이프라인", "codereview auto", "$code-review-auto" 같이 요청할 때
- PR을 올리기 직전, 설계의도 문서화부터 리뷰 반영까지 일괄 수행할 때

## Do Not Use When

- 단일 skill만 돌리고 싶을 때 → 각 단계 skill을 직접 invoke (`$code-review-intent` 등)
- 설계 중이거나 구현이 끝나지 않은 상태 → 먼저 구현을 완료한다
- 리뷰 결과만 필요할 때 → `$code-review-review`만 실행

## Preconditions

1. 변경 모듈에 해당하는 `docs/{module}/code-convention.yaml`과 `docs/{module}/adr.yaml`이 팀 상황에 맞게 작성되어 있어야 한다. (예: `docs/api/`, `docs/web/`, `docs/infra/`, `docs/ai/`)
2. 구현이 완료된 feature branch에서 실행한다.
3. base branch(기본: `master`)가 명확해야 한다.

## Leader Contract

리더는 다음만 유지한다:

- branch/base/diff stat
- 산출물 경로
- 사용자 확인이 필요한 논의점
- 단계별 완료/실패 상태

각 delegated step은 full artifact를 디스크에 저장하고, 리더에게는 다음 payload만 반환한다:

```json
{
  "artifact_path": ".omx/review-artifacts/{branch-name}/...",
  "status": "drafted|confirmed|completed|failed",
  "summary": ["...", "..."],
  "open_questions": ["...", "..."],
  "escalation_flag": "optional"
}
```

제약:
- `summary`는 최대 5개 bullet
- `open_questions`는 최대 5개 bullet
- full artifact 본문, raw diff, full ADR 본문, 긴 QA 로그는 리더 대화에 싣지 않는다

## Pipeline

### Step 1: 상태 점검

리더 세션이 직접 수행한다 (위임 없음).

1. `git branch --show-current`로 현재 branch를 확인한다.
2. `git diff <base>...HEAD --name-only`로 변경 파일 목록을 수집한다 (`<base>`는 사용자 확인).
3. 사용자에게 다음을 확인한다:
   - branch명
   - base branch (기본: `master`)
   - 변경 파일 수 요약
4. **모듈 자동 감지**: 변경 파일 경로의 최상위 디렉토리를 기준으로 관련 모듈을 감지한다.
   - 예: `api/src/...` → 모듈 `api` → `docs/api/adr.yaml`, `docs/api/code-convention.yaml`
   - 예: `web/src/...` → 모듈 `web` → `docs/web/adr.yaml`, `docs/web/code-convention.yaml`
   - 크로스 모듈 PR(예: `api/` + `web/` 동시 변경): 감지된 모든 모듈의 docs 경로를 파이프라인 상태에 저장하고, 이후 단계에서 각 모듈의 문서를 개별 참조하거나 병합하여 사용한다.
   - `docs/{module}/` 디렉토리가 없는 모듈은 사용자에게 알리고 해당 모듈의 docs 작성을 요청한다.
5. `.omx/review-artifacts/{branch-name}/` 디렉토리를 생성한다.

### Step 2: 설계의도 작성

`$code-review-intent`를 호출한다. 해당 skill은 내부적으로 `delegate(role="writer")`를 **forked context**로 실행한다.

1. subagent가 `design-intent.md`를 저장하고, 리더에게는 artifact 경로 + 짧은 요약 + **모호한 논의점**만 반환한다.
2. 사용자 피드백을 반영하여 확정한다.
3. 산출물을 `.omx/review-artifacts/{branch-name}/design-intent.md`에 저장한다.

### Step 3: 평가기준 수립

`$code-review-criteria`를 호출한다. 내부적으로 **ADR 추출은 격리 subagent**, **코드 컨벤션 머지는 forked context subagent**로 나누어 동작한다.

Step 1에서 감지된 모듈 경로(예: `docs/api/adr.yaml`)를 함께 전달한다.

1. subagent가 감지된 모듈의 `docs/{module}/adr.yaml`을 분석하여 이번 작업과 관련된 항목만 추출한다. full ADR 본문은 리더에 전달하지 않는다.
2. 감지된 모듈의 `docs/{module}/code-convention.yaml`에서 stacks 기반 필터링으로 공통 기준을 결합한다. 크로스 모듈 PR이면 각 모듈 문서를 순서대로 처리한다.
3. `code-quality-guide.md`를 저장하고, 리더에게는 artifact 경로 + 짧은 요약 + **기준 적용 범위가 모호한 논의점**만 반환한다.
4. 사용자 피드백을 반영하여 확정한다.
5. 산출물을 `.omx/review-artifacts/{branch-name}/code-quality-guide.md`에 저장한다.

### Step 4: PR 본문 생성

`$code-review-pr-body`를 호출한다. 내부적으로 `delegate(role="writer")`를 **forked context**로 실행한다.

1. subagent가 git diff와 기존 artifact 기반으로 PR 본문 초안을 생성한다.
2. 리더에게는 artifact 경로 + 짧은 요약 + **PR 설명에서 명확히 해야 할 논의점**(breaking change 여부, 마이그레이션 필요성, 관련 이슈 번호 등)만 제시한다.
3. 사용자 피드백을 반영하여 확정한다.
4. 산출물을 `.omx/review-artifacts/{branch-name}/pr-body.md`에 저장한다.

### Step 5: 코드리뷰 실행

`$code-review-review`를 호출한다. 내부적으로 `delegate(role="code-reviewer", tier="THOROUGH")`로 격리 실행된다.

**사용자 확인 없이 자동으로 진행한다.**

1. subagent는 다음을 입력받는다:
   - `.omx/review-artifacts/{branch-name}/code-quality-guide.md`
   - `.omx/review-artifacts/{branch-name}/design-intent.md`
   - `.omx/review-artifacts/{branch-name}/pr-body.md`
   - `git diff <base>...HEAD`
2. 근거 기반 리뷰 코멘트를 작성한다.
3. 결과를 `.omx/review-artifacts/{branch-name}/review-comments.md`에 저장하고, 리더에게는 severity 요약 + artifact 경로만 반환한다.

### Step 6: 리뷰 반영

`$code-review-reflect`를 호출한다. 내부적으로 `delegate(role="executor")`로 격리 실행되며, 이 단계에서만 소스코드 수정이 허용된다.

1. subagent가 각 리뷰 코멘트의 수용/거부 판단을 사용자에게 제시한다.
2. 사용자 확인 후 코드를 수정한다.
3. QA를 수행한다 (빌드, 테스트). 이 프로젝트는 Spring Boot이므로 `./gradlew build` 또는 `./gradlew test`.
4. 버그 발견 시 수정 루프를 반복한다.
5. 핵심 변경사항을 리더 세션에 보고한다 (고정 payload 범위 안의 요약만 — 전체 대화 로그와 긴 QA 로그를 끌어오지 않는다).

### 완료

파이프라인이 완료되면 `.omx/review-artifacts/{branch-name}/`의 전체 파일 목록과 최종 상태를 사용자에게 보고한다.

## Context Isolation Strategy

리더 세션은 파이프라인 **진행 상태와 사용자 논의점**만 유지한다. 실제 작업은 모두 subagent에 위임되므로:

- 리더 컨텍스트 ≈ 파이프라인 상태 + 사용자 결정 로그 + artifact 경로
- subagent 컨텍스트 ≈ 각 단계에 필요한 최소 입력 (diff, 참조 문서, 이전 산출물)

이렇게 하면 Step 6에서 리더가 Step 2의 토큰을 그대로 들고 다니는 일이 없어 컨텍스트 오염이 방지된다.

만약 작업이 매우 크거나 영속 상태가 필요하면 `omx team` 런타임으로 확장할 수 있다:

```bash
omx team 2:executor "code-review-auto pipeline on <branch-name>"
```

이 경우 각 워커는 자동으로 isolated git worktree를 받으므로 파이프라인의 격리 수준이 한 단계 더 강화된다. 다만 기본값은 여전히 delegate-first artifact pipeline이다.
