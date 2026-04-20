# Web 작업 내용 정리

## 개요

이번 작업에서는 기존의 빈 파일 스캐폴드 상태였던 `web` 폴더를 실제로 실행 가능한 Next.js 앱 구조로 전환하고, `shadcn/ui` 초기 설정과 시맨틱 토큰 연결까지 완료했다.

목표는 다음 두 가지였다.

- `web`을 실제로 실행, 빌드, 린트할 수 있는 상태로 만든다.
- 와이어프레임 기준 시각 토큰을 유지하면서 `shadcn/ui`를 붙일 수 있는 기반을 만든다.

---

## 주요 변경 사항

### 1. 루트 워크스페이스 설정 추가

루트에서 `web` 앱을 워크스페이스처럼 다룰 수 있도록 다음 파일을 추가했다.

- `package.json`
- `pnpm-workspace.yaml`
- `pnpm-lock.yaml`

적용 내용:

- `pnpm --filter ./web` 기반 실행 스크립트 추가
- `pnpm@10.32.0` 기준으로 루트 패키지 매니저 버전 고정
- `web` 폴더를 pnpm workspace 대상으로 등록

---

### 2. `web` 앱을 실행 가능한 Next.js 프로젝트로 전환

기존에는 폴더와 빈 파일만 존재했기 때문에 `pnpm`, `next`, `shadcn`이 정상 동작할 수 없었다. 이를 해결하기 위해 아래 파일들을 실제 내용이 있는 상태로 채웠다.

#### 설정 파일

- `web/package.json`
- `web/tsconfig.json`
- `web/next.config.ts`
- `web/postcss.config.mjs`
- `web/eslint.config.mjs`
- `web/next-env.d.ts`
- `web/.env.local`

적용 내용:

- Next.js 16 App Router 기준 기본 실행 스크립트 구성
- TypeScript alias `@/* -> ./src/*` 설정
- Tailwind v4 + PostCSS 구성
- ESLint 기본 설정 추가
- 공개 환경 변수 placeholder 추가
  - `NEXT_PUBLIC_API_BASE_URL`
  - `NEXT_PUBLIC_AI_BASE_URL`

---

### 3. App Router 전역/보호 레이아웃 연결

실제 라우팅이 동작하도록 전역 레이아웃과 보호 레이아웃을 구성했다.

#### 핵심 파일

- `web/src/app/layout.tsx`
- `web/src/app/(protected)/layout.tsx`
- `web/src/middleware.ts`

적용 내용:

- 전역 레이아웃에서 글로벌 CSS와 폰트 로드
- `(protected)` route group에서 `GNB + Sidebar + main` 구조 적용
- `/login`과 보호 라우트(`/`, `/department`, `/team`, `/user`, `/worklog`, `/search`, `/file`, `/notification`, `/tag`)에 대한 기본 인증 분기 추가

참고:

- 현재는 `accessToken` 쿠키 존재 여부만 보는 최소 보호 정책이다.
- Next.js 16 기준 `middleware.ts`는 deprecated 경고가 있으므로 추후 `proxy.ts` 전환 검토가 필요하다.

---

### 4. 주요 페이지 placeholder 연결

예약 파일이 비어 있으면 App Router가 깨지기 때문에, 실제 페이지가 렌더링되도록 주요 `page.tsx`들을 placeholder 화면으로 채웠다.

#### 대표 파일

- `web/src/app/(public)/login/page.tsx`
- `web/src/app/(protected)/page.tsx`
- `web/src/app/(protected)/department/page.tsx`
- `web/src/app/(protected)/team/page.tsx`
- `web/src/app/(protected)/user/page.tsx`
- `web/src/app/(protected)/worklog/page.tsx`
- `web/src/app/(protected)/search/page.tsx`
- `web/src/app/(protected)/file/page.tsx`
- `web/src/app/(protected)/notification/page.tsx`
- `web/src/app/(protected)/tag/page.tsx`

적용 내용:

- 각 라우트별 기본 타이틀과 설명 추가
- 보호 레이아웃이 실제로 렌더링되는지 확인 가능한 화면 구성
- 대시보드에서 주요 도메인으로 이동 가능한 링크 배치

---

### 5. 공통 레이아웃 컴포넌트 추가

공통 페이지 구조를 재사용하기 위해 기본 레이아웃 컴포넌트를 작성했다.

#### 추가/수정 파일

- `web/src/app/_common/components/layout/gnb.tsx`
- `web/src/app/_common/components/layout/sidebar.tsx`
- `web/src/app/_common/components/layout/pageHeader.tsx`
- `web/src/app/_common/components/layout/scaffoldPage.tsx`

적용 내용:

- protected 화면 공통 상단 바 구성
- 좌측 전역 메뉴 골격 구성
- 페이지 타이틀/설명 공통 헤더 구성
- placeholder 페이지용 공통 래퍼 구성

---

### 6. `shadcn/ui` 초기화 완료

실행 가능한 상태를 만든 뒤 `shadcn init`을 정상 완료했다.

#### 생성 파일

- `web/components.json`
- `web/src/components/ui/button.tsx`
- `web/src/lib/utils.ts`

적용 내용:

- `radix` + `nova` preset 기준 초기 설정
- `components`, `ui`, `utils`, `lib` alias 기준 연결
- 추후 `button`, `input`, `card` 등 공통 UI를 같은 기준으로 추가할 수 있는 상태 확보

---

### 7. 전역 스타일과 시맨틱 토큰 정리

`shadcn` 기본 구조는 유지하되, 실제 화면 톤은 와이어프레임 기준으로 맞추기 위해 글로벌 스타일을 정리했다.

#### 핵심 파일

- `web/src/app/globals.css`
- `docs/web/design-tokens.md`

적용 내용:

- `shadcn/tailwind.css`와 앱 시맨틱 토큰 연결
- 라이트/다크 모드 토큰 정의
- workspace shell, panel, sidebar 계열 공통 클래스 추가
- 와이어프레임에서 사용 중인 색상/질감 기준 유지

대표 토큰:

- `background`
- `foreground`
- `primary`
- `secondary`
- `muted`
- `border`
- `input`
- `sidebar`
- `warning`
- `success`
- `workspace-shell-base`
- `workspace-shell-gradient`

---

### 8. 한국어 주석 추가

실행 흐름과 의도가 바로 보이도록 핵심 파일들에 한국어 주석을 추가했다.

#### 주석 추가 파일

- `web/src/middleware.ts`
- `web/src/app/layout.tsx`
- `web/src/app/(protected)/layout.tsx`
- `web/src/app/(protected)/page.tsx`
- `web/src/app/(public)/login/page.tsx`
- `web/src/app/_common/components/layout/gnb.tsx`
- `web/src/app/_common/components/layout/pageHeader.tsx`
- `web/src/app/_common/components/layout/sidebar.tsx`
- `web/src/app/_common/components/layout/scaffoldPage.tsx`
- `web/src/app/globals.css`

주석 방향:

- 왜 이 구조로 나눴는지
- protected/public 레이아웃 분리 이유
- placeholder 공통화 목적
- 시맨틱 토큰과 `shadcn` 연결 의도

---

## 검증 결과

다음 항목을 확인했다.

- `pnpm install` 성공
- `pnpm build:web` 성공
- `pnpm lint:web` 성공
- dev 서버에서 `/login` 경로 `200` 응답 확인

검증 시 확인한 점:

- `/`, `/department`, `/team`, `/user`, `/worklog`, `/search`, `/file`, `/notification`, `/tag` 라우트가 빌드 대상에 정상 포함됨
- `shadcn init`이 실패하지 않고 `components.json`과 기본 UI 파일을 생성함

---

## 현재 상태

현재 `web`은 다음 상태까지 완료되었다.

- 실행 가능한 Next.js 앱
- 라우트 구조가 실제로 동작하는 상태
- protected/public 레이아웃 분리 완료
- `shadcn/ui` 초기 세팅 완료
- 시맨틱 토큰 연결 완료
- 공통 레이아웃 뼈대 구성 완료

즉, 다음 단계부터는 실제 UI 컴포넌트 추가와 화면 구현 작업으로 바로 넘어갈 수 있다.

---

## 아직 남아 있는 부분

이번 작업에서는 기반 세팅과 placeholder 연결까지만 진행했다. 아래 파일들은 아직 빈 파일 상태로 남아 있다.

- 각 도메인의 `_components`
- 각 도메인의 `_hooks`
- 각 도메인의 `_service`
- 각 도메인의 `_types`
- `_common` 하위의 일부 공용 모듈

이는 의도된 상태이며, 이후 화면 구현 또는 API 연동 단계에서 채워 나가면 된다.

---

## 다음 추천 작업

다음 순서로 진행하는 것이 자연스럽다.

1. `shadcn/ui` 공통 컴포넌트 추가
   - `input`
   - `card`
   - `label`
   - `dialog`
   - `badge`
   - `table`

2. 인증 화면 UI 구체화
   - `LoginForm`
   - `SignupPage`
   - `SignupForm`

3. 공통 디자인 패턴 정리
   - `PageHeader`
   - `DataTable`
   - `EmptyState`
   - `ConfirmDialog`

4. 도메인별 실제 화면 구현 시작
   - 조직관리
   - 사용자관리
   - 업무일지
   - 검색

---

## 비고

- `middleware.ts`는 현재 동작하지만 Next.js 16에서는 deprecated 경고가 있다.
- 추후 `proxy.ts` 전환 여부를 별도 작업으로 정리하는 것이 좋다.
- 이번 작업은 아직 커밋되지 않은 작업 트리 기준 정리이다.
