---
name: git-branch-naming
description: 현재 AX-WMS 모노레포 프로젝트의 브랜치 네이밍과 기본 브랜치 전략을 한글로 안내합니다. 사용자가 브랜치 이름을 정하려고 하거나, 어떤 브랜치를 파야 하는지 묻거나, 작업 종류와 앱(web/api/ai/infra/docs)에 맞는 branch prefix를 추천해달라고 할 때 사용합니다.
---

# Project Branch Naming

이 스킬은 현재 프로젝트의 **브랜치 전략과 네이밍 규칙**만 담당한다.
좋은 브랜치 이름은 작업 대상 앱과 변경 성격이 한눈에 드러나야 한다.
현재 프로젝트는 **TBD(Trunk-Based Development)** 방식으로 운영하며, `master` 브랜치를 trunk로 사용한다.
즉, 장수 `develop/dev` 브랜치를 두지 않고, **`master`에서 짧은 작업 브랜치를 생성해서 빠르게 머지/삭제**하는 흐름을 기본으로 한다.

## 기본 전략
- trunk 브랜치: `master`
- 작업 브랜치는 모두 `master` 에서 생성
- 작업 브랜치는 짧게 유지하고, 머지 후 바로 삭제
- 장수 통합 브랜치(`dev`, `develop`)는 두지 않음

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
- area는 `web`, `api`, `ai`, `infra`, `docs`, `shared` 중 하나를 사용
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
- `refactor/shared/git-convention-skill`

## 추천 규칙
사용자가 브랜치를 물으면 아래 우선순위로 제안한다.
1. 먼저 작업 유형을 분류한다 (`feat/fix/chore/refactor/docs/hotfix`)
2. 그다음 어느 앱/영역을 바꾸는지 분류한다 (`web/api/ai/infra/docs/shared`)
3. 가장 짧고 명확한 topic을 뽑는다
4. 브랜치명 1순위 + 대안 1~2개 제시한다
5. 필요하면 `master` 에서 브랜치를 만드는 명령까지 함께 제안한다

## 응답 예시 스타일
- 추천: `fix/api/worklog-status`
- 이유: TBD 방식에서 작업 유형과 영역, 주제가 모두 드러나며 짧은 브랜치로 운영하기 좋기 때문
- 대안: `fix/api/status-transition`, `refactor/api/worklog-status`
