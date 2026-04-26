# CLAUDE.md — AX-WMS 프로젝트 Claude 지침

## 기본 응답 규칙

**항상 한국어로 답변한다.** 코드, 파일 경로, 기술 식별자(변수명, 클래스명 등)는 원문 그대로 유지하되,
설명과 커뮤니케이션은 반드시 한국어로 작성한다.

---

## 프로젝트 개요

- **프로젝트**: AX-WMS (`S14P31S209`)
- **구조**: 모노레포 — `web` / `api` / `ai` / `nginx` 4개 영역으로 구성

| 영역 | 기술 | 역할 |
|---|---|---|
| `web` | Next.js | 사용자 UI, 화면 조합, 사용자 상호작용 진입점 |
| `api` | Spring Boot | 비즈니스 규칙, 인증/인가, 조직/업무/파일/알림 기준 시스템 |
| `ai` | FastAPI | AI 파이프라인 — 요약/태그/청킹/임베딩/시맨틱 검색 |
| `nginx` | Nginx | 외부 진입 게이트웨이, 라우팅, 보안 경계 |

전체 아키텍처 기준 문서: [`docs/AX-WMS_기획서_아키텍처가이드.md`](docs/AX-WMS_기획서_아키텍처가이드.md)

---

## 영역별 참조 문서

작업하는 영역에 따라 아래 문서를 **반드시 먼저 읽고** 규칙을 따른다.

### `api` — Spring Boot

| 문서 | 경로 |
|---|---|
| ADR (설계 결정 기록) | [`docs/api/adr.yaml`](docs/api/adr.yaml) |
| 코드 컨벤션 | [`docs/api/code-convention.yaml`](docs/api/code-convention.yaml) |

### `web` — Next.js

| 문서 | 경로 |
|---|---|
| 표준 가이드 | `docs/AX-WMS_NextJS_표준가이드.docx` |
| ADR | `docs/web/adr.yaml` _(추가 예정)_ |
| 코드 컨벤션 | `docs/web/code-convention.yaml` _(추가 예정)_ |

### `ai` — FastAPI

| 문서 | 경로 |
|---|---|
| 표준 가이드 | `docs/AX-WMS_FastAPI_표준가이드.docx` |
| ADR | `docs/ai/adr.yaml` _(추가 예정)_ |
| 코드 컨벤션 | `docs/ai/code-convention.yaml` _(추가 예정)_ |

### `nginx` (infra)

| 문서 | 경로 |
|---|---|
| 표준 가이드 | `docs/AX-WMS_Nginx_표준가이드.docx` |
| ADR | `docs/infra/adr.yaml` _(추가 예정)_ |
| 코드 컨벤션 | `docs/infra/code-convention.yaml` _(추가 예정)_ |

---

## 코드 작업 시 주의사항

- 요청한 범위 밖의 리팩토링이나 불필요한 변경을 하지 않는다.
- 보안 취약점(SQL Injection, XSS, CSRF 등)이 생기지 않도록 주의한다.
- UI/프론트엔드 변경 후에는 개발 서버에서 직접 동작을 확인한 뒤 완료로 보고한다.

---

## Git 커밋 규칙

- **사용자의 명시적 동의 없이 절대 커밋하지 않는다.** (`git commit`, `git commit --amend` 포함)
- "커밋해줘", "commit" 등 사용자가 분명히 지시한 경우에만 커밋을 실행한다.
- 작업 완료 보고는 커밋 실행 권한과 별개다. 변경 내용을 요약해 보고하되, 커밋 여부는 사용자가 결정한다.
- `git push`, `git reset --hard`, 브랜치 삭제 등 파괴적/공유 상태 변경 작업도 동일하게 명시적 동의가 필요하다.
