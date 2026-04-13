---
name: git-branch-naming
description: 현재 AX-WMS 모노레포 프로젝트의 브랜치 네이밍과 기본 브랜치 전략을 한글로 안내합니다. 사용자가 브랜치 이름을 정하려고 하거나, 어떤 브랜치를 파야 하는지 묻거나, 작업 종류와 앱(web/api/ai/infra/docs)에 맞는 branch prefix를 추천해달라고 할 때 사용합니다.
---

# Project Branch Naming

이 스킬은 현재 프로젝트의 **브랜치 전략과 네이밍 규칙**만 담당한다.
좋은 브랜치 이름은 작업 대상 앱과 변경 성격이 한눈에 드러나야 한다.
현재 프로젝트는 **GitFlow 경량 전략**으로 운영한다.

## 브랜치 구조

| 브랜치 | 역할 | 비고 |
|--------|------|------|
| `master` | 프로덕션 배포 브랜치 | 직접 커밋 금지. `dev` 또는 `hotfix/*`에서만 머지 |
| `dev` | 개발 통합 브랜치 | 모든 작업 브랜치의 base. 기능이 모이는 곳 |
| `feat/*` | 기능 개발 | `dev`에서 생성 → `dev`로 PR |
| `fix/*` | 버그 수정 | `dev`에서 생성 → `dev`로 PR |
| `refactor/*` | 리팩토링 | `dev`에서 생성 → `dev`로 PR |
| `chore/*` | 빌드/설정 잡일 | `dev`에서 생성 → `dev`로 PR |
| `docs/*` | 문서 작업 | `dev`에서 생성 → `dev`로 PR |
| `hotfix/*` | 운영 긴급 수정 | `master`에서 생성 → `master`로 PR 후 `dev`에도 반영 |

## 기본 전략
- 일상 작업 브랜치의 base: **`dev`**
- `dev` → `master`: 릴리즈 시 PR로 머지
- `hotfix/*`: `master`에서 생성, 완료 후 `master` + `dev` 양쪽에 반영
- 작업 브랜치는 머지 후 바로 삭제

## 권장 형식
```text
<type>/<area>/<topic>
```

예:
- `fix/api/worklog-status`
- `feat/web/login-flow`
- `feat/ai/embedding-pipeline`
- `chore/infra/routing-cleanup`
- `docs/schema/sql-guide-sync`

## 네이밍 원칙
- 모두 소문자 사용
- 공백 금지
- `/` 로 카테고리 구분
- 너무 길지 않게 작성
- 한 브랜치는 한 주제만 담기
- topic은 1~3단어 이내 권장
- 첫 토큰은 **작업 유형(type)** 으로 시작
- 두 번째 토큰은 **영역(area)** 으로 둔다
- area는 `web`, `api`, `ai`, `infra`, `docs`, `agent` 중 하나를 사용
- 브랜치 이름은 짧고 일회성으로 유지한다

## type 권장값
- `feat`
- `fix`
- `chore`
- `refactor`
- `docs`
- `hotfix`

## 예시
- `docs/schema/sql-guide-sync`
- `docs/guide/enum-alignment`
- `feat/web/login-flow`
- `fix/web/dashboard-filter`
- `feat/api/auth-login`
- `fix/api/worklog-status`
- `feat/ai/embedding-pipeline`
- `chore/infra/docker-compose`
- `refactor/agent/git-convention-skill`

## 추천 규칙
사용자가 브랜치를 물으면 아래 우선순위로 제안한다.
1. 먼저 작업 유형을 분류한다 (`feat/fix/chore/refactor/docs/hotfix`)
2. 그다음 어느 앱/영역을 바꾸는지 분류한다 (`web/api/ai/infra/docs/agent`)
3. 가장 짧고 명확한 topic을 뽑는다
4. 브랜치명 1순위 + 대안 1~2개 제시한다
5. `hotfix/*` 가 아니면 `dev` 에서, `hotfix/*` 이면 `master` 에서 브랜치를 만드는 명령까지 함께 제안한다

## 응답 예시 스타일
- 추천: `fix/api/worklog-status`
- 이유: 작업 유형과 영역, 주제가 모두 드러나며 `dev` 기반 단기 브랜치로 운영하기 좋기 때문
- 대안: `fix/api/status-transition`, `refactor/api/worklog-status`
- base 브랜치: `dev` (hotfix 계열이면 `master`)
