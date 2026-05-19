# 업무일지 조회 페이지 폰트 로딩 최적화 기록

- 작성일: 2026-05-20
- 대상 화면: 업무일지 조회 페이지(`/worklog`)
- 관련 파일:
  - `web/src/app/layout.tsx`
  - `web/src/app/globals.css`
  - `web/src/app/_assets/fonts/PretendardVariable.woff2`

## 배경

테스트 서버에서 업무일지 조회 페이지 Lighthouse 측정 중 LCP가 높게 관측되었다.
이후 LCP breakdown을 확인했을 때 LCP element는 업무일지 카드의 제목 텍스트였다.

```html
<p class="text-[18px] font-semibold tracking-[-0.03em] text-foreground">
  AI 평가셋 재구성 기준 보정 작업 - 수정 테스트 123
</p>
```

TTFB는 약 20ms, element render delay는 약 600ms 수준으로 확인되어 서버 HTML 응답 자체가 병목은 아니었다.
다만 Lighthouse diagnostics에서 외부 폰트 CSS가 render blocking request로 잡혔다.

확인된 render blocking 항목:

- Google Fonts: `IBM Plex Mono`
- JSDelivr CDN: `Pretendard Variable`
- 앱 CSS chunk

특히 Pretendard는 `cdn.jsdelivr.net`에서 CSS를 먼저 받은 뒤 약 2MB의 `PretendardVariable.woff2`를 내려받는 구조였다.
초기 화면 텍스트 렌더링 전에 외부 CDN 연결과 CSS 로딩이 끼어 있어 FCP/LCP에 불필요한 지연을 줄 수 있었다.

## 기존 구조

기존에는 `globals.css` 최상단에서 외부 폰트 CSS를 직접 import했다.

```css
@import url("https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/variable/pretendardvariable.css");
@import url("https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600&display=swap");
```

이 방식의 문제는 다음과 같다.

- CSS `@import`가 초기 렌더링 경로에 포함된다.
- 외부 CDN DNS/TLS/응답 시간이 초기 렌더에 영향을 준다.
- Pretendard 폰트 캐시 정책을 애플리케이션에서 직접 통제하기 어렵다.
- 업무일지 조회 화면에서 핵심이 아닌 mono font까지 전역으로 로드한다.

## 수정 내용

### 1. Pretendard를 로컬 폰트로 전환

Pretendard variable font 파일을 프로젝트 내부에 추가했다.

```text
web/src/app/_assets/fonts/PretendardVariable.woff2
```

`layout.tsx`에서는 `next/font/local`을 사용해 로컬 폰트로 등록했다.

```tsx
import localFont from "next/font/local";

const pretendard = localFont({
  src: "./_assets/fonts/PretendardVariable.woff2",
  variable: "--font-pretendard",
  weight: "45 920",
  display: "swap",
  preload: true,
  fallback: ["Apple SD Gothic Neo", "Malgun Gothic", "Segoe UI", "sans-serif"],
});
```

그리고 root `<html>`에 font variable class를 연결했다.

```tsx
<html
  lang="ko"
  data-scroll-behavior="smooth"
  className={cn("h-full", "font-sans", pretendard.variable)}
>
```

### 2. 외부 폰트 `@import` 제거

`globals.css`에서 Google Fonts와 JSDelivr import를 제거했다.

제거 대상:

```css
@import url("https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/variable/pretendardvariable.css");
@import url("https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600&display=swap");
```

### 3. 전역 폰트 변수 정리

Pretendard는 `next/font/local`이 만든 CSS variable을 사용하도록 변경했다.

```css
--font-display-family:
  var(--font-pretendard), "Apple SD Gothic Neo", "Malgun Gothic", "Segoe UI",
  sans-serif;
```

전역 mono font는 외부 Google Fonts를 쓰지 않고 시스템 mono stack으로 대체했다.

```css
--font-mono-family:
  ui-monospace, "SFMono-Regular", "SF Mono", Consolas, "Liberation Mono",
  Menlo, monospace;
```

## 기대 효과

이 작업은 LCP 13초 전체를 직접 해결하는 변경은 아니다.
Lighthouse가 표시한 render blocking request 중 외부 폰트 CSS 경로를 제거하는 목적의 개선이다.

기대 효과:

- `fonts.googleapis.com` render blocking request 제거
- `cdn.jsdelivr.net` Pretendard CSS render blocking request 제거
- 외부 CDN 연결 비용 제거
- Next.js 정적 asset 캐시 정책 활용
- `font-display: swap`으로 폰트 다운로드 전에도 fallback font로 텍스트 렌더 가능
- 업무일지 조회 페이지의 LCP 텍스트가 외부 폰트 CSS 응답을 기다리는 상황 완화

## 확인 방법

배포 후 업무일지 조회 페이지에서 Lighthouse를 다시 실행한다.

확인할 항목:

- `Render blocking requests`에 `fonts.googleapis.com`이 사라졌는지
- `Render blocking requests`에 `cdn.jsdelivr.net` Pretendard CSS가 사라졌는지
- Network 탭에서 Pretendard가 `/_next/static/media/...woff2` 형태로 제공되는지
- LCP element가 여전히 업무일지 카드 제목인지
- LCP breakdown에서 `Render delay`와 전체 LCP가 얼마나 변했는지

## 검증

로컬에서 다음 명령을 실행해 기본 검증을 완료했다.

```bash
pnpm --filter ./web lint
pnpm --filter ./web build
```

검증 결과:

- web lint 통과
- web production build 통과
- `web/next-env.d.ts` 변경 없음
- favicon 관련 파일 변경 없음

## 후속 검토

폰트 로딩 개선 이후에도 업무일지 조회 페이지 LCP가 높다면 다음 항목을 이어서 확인한다.

- `/api/worklogs` 응답 시간
- `/api/worklogs/filter-options` 응답 시간
- 인증 복구 흐름의 `auth/refresh -> users/me -> worklogs` 직렬 지연
- 업무일지 카드 렌더링 시 style/layout 비용
- 목록 skeleton 또는 초기 레이아웃 안정화 여부
