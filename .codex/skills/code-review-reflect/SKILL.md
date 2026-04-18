---
name: code-review-reflect
description: 리뷰 코멘트 수용/거부 판단 후 코드 수정 + QA. 파이프라인에서 유일하게 소스코드를 수정하는 단계
---

# Code Review — Reflect Review

`review-comments.md`의 각 코멘트를 검토하고, 수용/거부를 판단하여 코드를 수정한 뒤 QA를 수행한다.

## When to Use

- 리뷰 코멘트 반영만 별도로 수행하고 싶을 때 (`$code-review-reflect`)
- **`review-comments.md`가 사전에 작성되어 있어야 한다.**

## Invocation Authority

- 사용자가 `$code-review-reflect`를 명시 호출하면, 이 skill 내부의 `spawn_agent()` 호출은 이미 승인된 것으로 간주한다.
- 리더는 explicit skill invocation 이후에도 subagent 사용 승인을 다시 요구하거나, 보수적 해석만으로 직접 수정 단계로 바꾸지 않는다.

> **파이프라인 실행 중에는** `$code-review-auto`의 Step 6에서 이 skill이 순차적으로 호출된다. agent 실행 세부와 판정 형식은 이 문서를 따른다.

## Execution Mode (독립 실행 시)

리더는 아래 `spawn_agent()`를 **직접 호출**하여 executor subagent에 위임한다. 이 skill은 **유일하게 소스코드를 수정**하는 단계다:

```
spawn_agent(
  role="executor",
  tier="THOROUGH",
  fork_context=false,
  task="REVIEW REFLECTION + QA

리뷰 코멘트를 검토하고 코드를 수정한 뒤 QA를 수행하라.

입력:
- .codex/review-artifacts/{branch-name}/review-comments.md
- .codex/review-artifacts/{branch-name}/design-intent.md (의도 검증용)
- .codex/review-artifacts/{branch-name}/code-quality-guide.md (기준 재확인용)
- 사용자 결정 요약 또는 `review-decisions.md` (있으면)
- 현재 작업 트리 (수정 대상)

절차:
1. review-comments.md를 읽고 각 코멘트의 수용/거부를 판단한다.
2. 우선순위별 처리 정책:
   - [p1] 기본적으로 ACCEPT. REJECT 시 명확한 근거 필요 (예: design-intent와 충돌).
   - [p2] 판단과 근거를 사용자에게 제시하고 확인.
   - [p3] 판단을 제시하되 사용자 재량에 맡긴다.
   - [p4] 일괄 수용 또는 일괄 무시 (사용자 선호 확인).
3. 사용자 확인 후 코드를 수정한다.
4. QA 수행:
   - Spring Boot 프로젝트 → './gradlew build' 또는 './gradlew test'
   - 빌드/테스트 실패 시 수정 루프(수정 → QA → 재확인) 반복.
5. 각 코멘트 하단에 판정을 추가하여 review-comments.md를 업데이트한다.
6. 최종 변경사항(수정 파일 목록, QA 결과, 추가 발견 사항)을 요약하여 리더에게 보고한다.

판정 형식:
- **판정: ACCEPT** — 수정 완료. {수정 내용 요약}
- **판정: REJECT** — {거부 근거}
- **판정: DEFER** — {보류 이유, 후속 이슈 트래킹 필요}

중요:
- 사용자 확인 없이 [p1] 코멘트를 자동 수정해도 된다. 단, REJECT 결정만은 사용자에게 보고한다.
- [p2] 이상은 반드시 사용자 확인 후 진행한다.
- 빌드 실패를 숨기지 마라. 실패 로그와 함께 보고한다.
- 리더 반환 payload: {artifact_path, status, summary(≤5), open_questions(사용자 확인 필요 항목만 ≤5), escalation_flag(QA 실패/추가 판단 필요 시)}
- 리더에게는 전체 대화 로그가 아닌 **수정 요약**만 반환한다.
"
)
```

## 절차 (리더 세션 관점)

1. `.codex/review-artifacts/{branch-name}/review-comments.md` 존재 확인.
2. 리더가 위 `spawn_agent()`를 호출한다.
3. subagent가 사용자 확인을 요청하면 리더가 중계하고, 필요 시 사용자 결정을 bullet 요약 또는 `review-decisions.md`로 정리해 재전달한다.
4. subagent가 bounded payload 범위의 최종 수정 요약을 반환하면 사용자에게 보고한다.

## Output

리뷰 반영 결과는 `review-comments.md`에 판정을 추가하여 in-place 업데이트한다.

```
.codex/review-artifacts/{branch-name}/review-comments.md
```

## 판정 형식 예시

```markdown
### [p1] src/main/java/com/example/payment/PaymentService.java:42
- 근거: GEN-001 함수 단일 책임 (SB-005 트랜잭션 경계)
- 내용: ...
- 제안: ...
- side effect: ...
- 제안 이유: ...
- **판정: ACCEPT** — 수정 완료. `extractPaymentData()` 메서드로 분리하고 @Transactional(readOnly=true) 추가.
```

```markdown
### [p2] src/main/java/com/example/auth/AuthController.java:15
- 근거: ADR-001 (응답 자동 래핑)
- 내용: ...
- 제안: ...
- **판정: REJECT** — 현재 컨트롤러는 외부 webhook 응답을 그대로 전달하는 케이스라 ADR-001 적용 대상에서 제외. design-intent.md '의도적으로 제외한 것' 섹션 참조.
```

## QA 명령어 (이 프로젝트 기준)

Spring Boot:
```bash
./gradlew build       # 컴파일 + 테스트
./gradlew test        # 테스트만
./gradlew check       # 정적 분석 + 테스트
```

다른 모듈이 추가되면 해당 모듈의 build 명령어도 함께 실행한다.

## Context Isolation Rationale

이 단계는 **유일하게 소스코드를 수정**하는 단계이므로, 리더 세션에서 직접 수정하면 다른 작업과 변경사항이 섞일 위험이 있다. `spawn_agent(role="executor", fork_context=false)`로 위임하면:

- 수정 작업이 격리된 컨텍스트에서 수행된다.
- 리더는 **수정 요약**만 받아 사용자에게 보고한다.
- subagent가 QA 실패 루프를 돌면서 발생하는 모든 토큰이 리더에 쌓이지 않는다.
- QA 실패 시에도 full log를 리더에 싣지 않고, 실패 상태와 최소 로그 발췌만 전달한다.
- 사용자 판단이 필요한 p2/p3 코멘트도 대화 전체 포크 대신 별도 요약 입력으로 재주입할 수 있다.

장시간 병렬 작업이 필요하면 `omx team N:executor "reflect-review on <branch-name>"`로 확장 가능. 각 워커에 isolated git worktree가 부여되어 격리 수준이 한 단계 강화된다.
