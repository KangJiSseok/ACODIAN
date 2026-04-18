---
name: code-review-auto
description: 코드리뷰 전체 파이프라인 오케스트레이션 — 설계의도, 평가기준, PR본문, 리뷰, 반영을 순차 실행
---

# Code Review Auto Pipeline

전체 코드리뷰 파이프라인을 **순차적으로** 오케스트레이션한다. 이 skill은 직접 산출물을 작성하는 문서가 아니라, 어떤 순서로 어떤 하위 skill을 실행할지와 단계 간 handoff 규칙만 정의한다.

## When to Use

- 구현이 완료된 feature branch에서 리뷰 준비를 한 번에 끝내고 싶을 때
- 사용자가 "코드리뷰 파이프라인", "codereview auto", "$code-review-auto" 같이 요청할 때
- PR 직전에 설계의도 문서화부터 리뷰 반영까지 일괄 수행할 때

## Invocation Authority

- 사용자가 `$code-review-auto`를 명시 호출하면, 리더는 이 파이프라인을 끝까지 진행할 권한이 있다고 본다.
- 각 step의 구체적인 agent 실행 방식은 해당 하위 skill 문서가 authoritative하다.
- 리더는 하위 skill의 실행 세부를 이 문서에 중복 정의하지 않는다.

## Principles

- 병렬 실행하지 않는다. 항상 **한 step이 끝난 뒤 다음 step**으로 간다.
- 단계 간 문맥은 대화 전체가 아니라 **artifact와 짧은 요약**으로 넘긴다.
- 리더는 상태, 경로, 논의점, 완료/실패만 관리한다.
- 산출물 본문은 해당 step의 하위 skill이 작성한다.
- 어떤 step이 실패하면 그 즉시 파이프라인을 중단하고 실패 step만 보고한다.

## Preconditions

1. 변경 모듈에 해당하는 `docs/{module}/code-convention.yaml`과 `docs/{module}/adr.yaml`이 준비되어 있어야 한다.
2. 구현이 완료된 feature branch에서 실행한다.
3. base branch(기본: `dev`)가 명확해야 한다.

## Leader Contract

리더는 다음만 유지한다:

- branch/base/diff stat
- 산출물 경로
- 사용자 확인이 필요한 논의점
- 단계별 완료/실패 상태

리더가 하지 말아야 할 일:

- 하위 skill이 작성해야 할 artifact 본문을 직접 쓰지 않는다.
- 여러 step을 동시에 띄우지 않는다.
- 긴 diff, ADR 전문, 전체 로그를 대화에 싣지 않는다.

## Pipeline

### Step 1: 상태 점검

리더가 직접 수행한다.

1. `git branch --show-current`로 현재 branch를 확인한다.
2. `git diff <base>...HEAD --name-only`와 `git diff --stat <base>...HEAD`로 변경 범위를 확인한다.
3. 변경 파일의 최상위 디렉토리 기준으로 대상 모듈을 감지한다.
4. `.codex/review-artifacts/{branch-name}/` 디렉토리를 준비한다.
5. 이후 step에서 사용할 brief를 짧게 정리한다.

### Step 2: 설계의도 작성

- 하위 skill: `$code-review-intent`
- 산출물: `.codex/review-artifacts/{branch-name}/design-intent.md`
- 입력 handoff:
  - branch/base
  - 작업 brief
  - 변경 모듈
  - 관련 docs 경로

완료 후:

- summary와 open questions만 확인한다.
- 사용자 논의가 필요하면 여기서 처리한 뒤 다음 step으로 간다.

### Step 3: 평가기준 수립

- 하위 skill: `$code-review-criteria`
- 산출물: `.codex/review-artifacts/{branch-name}/code-quality-guide.md`
- 입력 handoff:
  - branch/base
  - Step 2 결과
  - 관련 `docs/{module}/adr.yaml`
  - 관련 `docs/{module}/code-convention.yaml`

완료 후:

- summary와 open questions만 확인한다.
- 기준 적용 범위 논의가 끝난 뒤 다음 step으로 간다.

### Step 4: PR 본문 생성

- 하위 skill: `$code-review-pr-body`
- 산출물: `.codex/review-artifacts/{branch-name}/pr-body.md`
- 입력 handoff:
  - branch/base
  - Step 2, Step 3 산출물
  - 확정된 논의점 요약

완료 후:

- summary와 open questions만 확인한다.
- breaking change, migration, issue link 같은 확인 포인트를 정리한 뒤 다음 step으로 간다.

### Step 5: 코드리뷰 실행

- 하위 skill: `$code-review-review`
- 산출물: `.codex/review-artifacts/{branch-name}/review-comments.md`
- 입력 handoff:
  - Step 2, Step 3, Step 4 산출물
  - base 대비 diff

완료 후:

- severity 요약만 확인한다.
- artifact가 없으면 실패로 보고하고 중단한다.

### Step 6: 리뷰 반영

- 하위 skill: `$code-review-reflect`
- 입력 handoff:
  - `review-comments.md`
  - `design-intent.md`
  - `code-quality-guide.md`
  - 사용자 결정 요약

완료 후:

- 수정 요약과 QA 결과만 확인한다.
- 필요하면 사용자 판단 항목을 정리해 다시 전달한다.

## Failure Rule

- 어떤 step이든 artifact 미생성, bounded payload 미반환, 입력 전제 미충족이면 해당 step을 failed로 기록하고 즉시 중단한다.
- 실패 시 사용자에게는 실패 step, 실패 이유 요약, 현재까지 생성된 artifact 목록만 보고한다.
- 실패한 step을 리더가 직접 대필하지 않는다.

## Output

파이프라인 완료 시 다음만 보고한다:

- `.codex/review-artifacts/{branch-name}/`의 생성 파일 목록
- 각 step의 완료 상태
- 남은 open question 또는 리스크

## Rationale

`code-review-auto`는 오케스트레이터여야지, 각 하위 skill의 실행 방식을 중복 서술하는 문서가 되어선 안 된다. 실행 세부는 하위 skill에 두고, 이 문서는 **순서, handoff, 중단 조건**만 관리해야 유지보수가 쉽다.
