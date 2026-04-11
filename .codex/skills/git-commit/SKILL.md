---
name: git-commit
description: 현재 AX-WMS 프로젝트의 커밋 메시지를 Conventional Commit 형식(타입 + 제목 + 본문 + 꼬리말)에 맞춰 작성하도록 돕습니다. 사용자가 변경 사항을 커밋하려고 하거나, 커밋 메시지를 작성/검토/수정해 달라고 하거나, "커밋해줘", "commit message 만들어줘", "이 변경사항 커밋 메시지 어떻게 쓸까?" 같은 요청을 할 때 사용합니다.
---

# Project Git Commit

이 스킬은 현재 프로젝트의 **커밋 메시지 규칙**만 담당한다.
이 프로젝트의 커밋 메시지는 **Conventional Commit** 형식을 따른다.
좋은 커밋 메시지는 변경 이력을 빠르게 훑어볼 수 있게 해주고, 코드 리뷰와 디버깅, 릴리즈 노트 작성에 도움이 된다. 단순한 형식 강제가 아니라 "나중에 이 커밋을 다시 봤을 때 무엇을, 왜 바꿨는지 한눈에 알 수 있는가?"를 기준으로 메시지를 만든다.

## 커밋 메시지 구조

```text
<type>(<scope>): <subject>

<body>

<footer>
```

- `type`: 커밋의 종류 (필수)
- `scope`: 변경이 일어난 범위/모듈 (선택)
- `subject`: 변경 내용을 한 줄로 요약 (필수)
- `body`: 변경의 배경, 이유, 부수 효과 등 상세 설명 (선택)
- `footer`: 필요할 때만 쓰는 추가 정보. 이 프로젝트는 기본적으로 이슈 없이 운영하므로 보통 생략 가능하며, `BREAKING CHANGE` 같은 특별한 경우에만 사용한다.

## 커밋 타입 (type)

| 타입 | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 동작에 영향 없는 포맷 수정 |
| `refactor` | 동작 변화 없는 리팩토링 |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드 설정, 패키지 매니저, `.gitignore` 등 잡일 |
| `design` | 사용자 UI 디자인 변경 (프로젝트 확장 타입) |
| `comment` | 주석 추가/수정 (프로젝트 확장 타입) |
| `rename` | 파일/폴더 이름 변경 또는 이동만 수행 |
| `remove` | 파일 삭제만 수행 |
| `hotfix` | 운영 환경의 치명적 버그를 긴급 수정할 때 |

> `design`, `comment`, `hotfix`는 표준 Conventional Commit에 없는 이 프로젝트 전용 확장 타입이다.

특수 표기:
- `!HOTFIX`: `hotfix` 타입 대신 `fix!` 또는 타입 뒤에 `!`를 붙여 breaking-change 수준의 긴급 수정을 나타낼 때 사용
- `BREAKING CHANGE`: 호환성을 깨는 큰 변경. 꼬리말에 명시하거나 타입 뒤에 `!`를 붙임

## scope 추천
현재 프로젝트는 모노레포 성격이 있으므로 scope는 아래를 우선 검토한다.
- `web`
- `api`
- `ai`
- `infra`
- `docs`
- `shared`

예:
- `feat(api): add auth login endpoint`
- `fix(web): correct dashboard filter reset`
- `docs(schema): sync enum definitions`
- `chore(infra): update docker compose defaults`

## 제목(Subject) 작성 규칙
- **50자 이내** 권장
- **명령형 현재 시제** 사용 (`Add`, `Fix`, `Change`)
- 첫 글자는 **대문자**로 시작 (영문 기준)
- 끝에 **마침표를 붙이지 않음**
- 무엇을, 왜 바꿨는지 알 수 있게 작성

좋은 예:
- `feat(api): add JWT refresh token endpoint`
- `fix(api): prevent invalid worklog status transition`
- `docs(schema): update enum code descriptions`

나쁜 예:
- `update`
- `Fixed bug.`
- `feat: 로그인도 하고 비밀번호 재설정도 같이 구현`

## 본문(Body) 작성 규칙
- 제목과 본문 사이에 **빈 줄 한 줄**을 둔다.
- 한 줄은 **72자 이내** 권장
- 본문에는 *어떻게* 보다 *왜*를 적는다.
- 이전 방식과 비교, 부수 효과, 제약이 있으면 적는다.

## 꼬리말(Footer) 작성 규칙
현재 프로젝트는 **이슈 없이 운영**하는 것을 기본으로 하므로, footer는 대부분 생략한다.
다만 아래처럼 특별한 추가 정보가 필요할 때는 사용할 수 있다.

`BREAKING CHANGE` 예:
```text
BREAKING CHANGE: worklog status API now rejects unknown status codes.
```

## 응답 방식
사용자가 커밋 메시지를 요청하면 아래 순서로 답한다.
1. 변경 범위를 한 문장으로 요약
2. 적절한 `type` / `scope` 제안
3. 제목 한 줄 제안
4. 필요하면 full commit message (body 포함, footer는 특별한 경우에만) 제안

## 프로젝트 특화 주의사항
- 모노레포 작업이면 body에서 영향 영역(`web/api/ai/infra/docs/shared`)을 분명히 적는다.
- 큰 변경을 한 커밋에 몰지 말고, 빠르게 리뷰 가능한 작은 단위 커밋을 선호한다.

## 전체 예시

### 예시 1 — 단순한 기능 추가
```text
feat(ai): add embedding pipeline skeleton
```

### 예시 2 — 본문이 필요한 버그 수정
```text
fix(api): prevent invalid worklog status transition

잘못된 상태 값이 들어와도 서비스 계층에서 그대로 처리되던 문제를 수정합니다.
허용된 상태 집합만 통과하도록 검증 로직을 추가했습니다.
```

### 예시 3 — 문서 수정
```text
docs(schema): align enum definitions with SQL baseline

가이드 문서에 누락되어 있던 enum 저장 코드 설명을 추가해
문서와 SQL 초안의 용어 차이를 줄입니다.
```
