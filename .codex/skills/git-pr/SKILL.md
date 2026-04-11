---
name: git-pr
description: 현재 AX-WMS 프로젝트의 PR 제목과 본문을 한글 맥락에 맞게 작성하도록 돕습니다. 사용자가 PR 제목/본문을 만들어달라고 하거나, 리뷰어가 이해하기 쉬운 PR 설명을 정리해달라고 할 때 사용합니다.
---

# Project Git PR

이 스킬은 현재 프로젝트의 **PR 제목 / PR 본문 규칙**만 담당한다.
커밋 메시지는 **Conventional Commit** 규칙을 참고해 작성할 수 있지만, PR 제목은 검색성과 가독성을 위해 **짧고 읽기 쉬운 형식**을 우선한다.

또한 현재 프로젝트는 **TBD(Trunk-Based Development)** 방식으로 운영하므로, PR은 모두 **`master` 브랜치로 머지하는 짧은 작업 단위 PR**을 기본으로 한다.
이 프로젝트는 모노레포이므로 scope는 가능하면 `web`, `api`, `ai`, `infra`, `docs`, `shared` 중 하나로 시작한다.

## PR 제목 형식
```text
<type>(<scope>): <subject>
```

## type 예시
- `docs`
- `fix`
- `feat`
- `refactor`
- `chore`

## scope 예시
- `web`
- `api`
- `ai`
- `infra`
- `docs`
- `shared`

## PR 제목 예시
- `docs(api): update response schema guide`
- `fix(api): prevent invalid status transition`
- `feat(web): add importance filter`
- `chore(shared): add project git skills`
- `refactor(api): simplify worklog status flow`

## PR 제목 작성 원칙
- 제목은 한 줄로 짧게 쓴다.
- `type`은 변경 성격을 드러내고, `scope`는 변경 영역을 드러낸다.
- `subject`는 동사 중심으로 핵심만 쓴다.
- PR 제목은 커밋 메시지처럼 엄격한 규약 검사용이 아니라, **리뷰어가 빠르게 이해할 수 있는 검색 친화형 요약**으로 본다.
- 가능하면 한 PR에는 한 가지 주제만 담는다.

## PR 본문 템플릿
이 프로젝트의 PR 본문은 아래 구조를 기본으로 사용한다.

```md
## Type of change
<!-- 해당하는 항목에 [x] 표시해주세요. 여러 항목 중복 선택 가능합니다. -->
- [ ] 🐛 Bug fix — 기존 동작을 정상화하는 변경
- [ ] ✨ New feature — 사용자에게 보이는 새로운 기능
- [ ] ♻️ Refactor — 외부 동작 변화 없는 코드 구조 개선
- [ ] 📝 Documentation — 문서 변경
- [ ] 🎨 Style / Design — 코드 포맷 또는 사용자 UI 디자인 변경
- [ ] ✅ Test — 테스트 코드 추가/수정
- [ ] 🔧 Chore — 빌드, CI, 패키지 매니저 등 잡일

## Motivation

<!-- 작성 배경 -->

## Problem Solving

<!-- 해결 방법 -->

## To Reviewer

<!-- 리뷰어에게 말하고 싶은 것, 유의 깊게 봐주었으면 하는 것, 의문점 등 -->
```

## 본문 작성 원칙
- `Type of change` 에는 해당 항목만 체크한다.
- `Motivation` 에는 왜 이 변경이 필요한지 적는다.
- `Problem Solving` 에는 어떤 방식으로 해결했는지 적는다.
- `To Reviewer` 에는 리뷰어가 중점적으로 볼 부분, 아직 확신이 없는 부분, 확인 요청사항을 적는다.
- diff를 그대로 설명하지 말고, 리뷰어가 알아야 할 판단 포인트를 적는다.
- 문서 작업 PR이면 source of truth가 무엇인지 명시한다.
- TBD 원칙상 PR은 가능한 한 작고 빠르게 머지 가능한 범위로 쪼갠다.
- `master` 를 오래 막는 대형 PR은 지양한다.
- 이슈 번호 섹션은 기본적으로 두지 않는다. 필요하면 추후 별도 규칙을 추가한다.

## 문서/스키마 작업에 특화된 문안
문서나 SQL 정렬 작업 PR에서는 아래 항목을 자주 포함한다.
- 기준안: `docs2/ax-wms-erd-draft.sql`
- 반영 항목: naming / enum / relationship / guide alignment
- 미반영 항목: 아직 rewrite 안 된 guide 구간

## 모노레포 scope 추천
- `web`: Next.js 화면/프론트엔드 작업
- `api`: Spring Boot 비즈니스 API / DB / 인증 / 스키마 작업
- `ai`: FastAPI / 임베딩 / AI 파이프라인 작업
- `infra`: nginx, docker, 배포 설정, 리버스 프록시 등 인프라 작업
- `docs`: 문서 작업
- `shared`: 여러 영역 공통 작업

## 응답 방식
사용자가 PR을 요청하면 아래 순서로 답한다.
1. PR 제목 제안
2. `Type of change` 체크 항목 제안
3. `Motivation` 초안
4. `Problem Solving` 초안
5. `To Reviewer` 초안
6. base 브랜치는 기본적으로 `master` 로 둔다
7. 필요하면 Markdown 형식으로 다시 정리

## 본문 예시 뼈대
```md
## Type of change
- [ ] 🐛 Bug fix
- [ ] ✨ New feature
- [ ] ♻️ Refactor
- [x] 📝 Documentation
- [ ] 🎨 Style / Design
- [ ] ✅ Test
- [ ] 🔧 Chore

## Motivation

- 가이드와 SQL 초안 사이의 용어/관계 불일치가 있어 문서 기준 정리가 필요했습니다.

## Problem Solving

- SQL 기준안을 source of truth로 두고 enum 코드 및 스키마 용어를 문서에 반영했습니다.

## To Reviewer

- SQL 기준안을 문서 source of truth로 두는 판단이 적절한지 중점적으로 확인 부탁드립니다.
- 아직 rewrite되지 않은 guide 구간의 분리 범위가 적절한지도 봐주시면 좋겠습니다.
```
