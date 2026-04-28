# 로그인 redirect query 조회 방식 정리

- 작성일: 2026-04-28
- 파일명: `2026-04-28-login-redirect-search-params.md`
- 대상 파일:
  - `web/src/app/(public)/login/page.tsx`

## 목적

로그인 성공 후 이동할 redirect 경로를 읽는 방식을 `window.location.search`에서 Next.js App Router의 `useSearchParams()`로 변경한 이유와 주의점을 정리한다.

## 배경

보호 라우트에서 인증되지 않은 사용자를 로그인 화면으로 보낼 때 다음처럼 원래 접근하려던 경로를 query string에 담는다.

```text
/login?redirect=/team
```

로그인 화면은 로그인 성공 후 이 `redirect` 값을 읽어 사용자를 원래 가려던 보호 페이지로 돌려보내야 한다.

기존 구현은 다음처럼 브라우저 전역 객체를 직접 읽었다.

```ts
new URLSearchParams(window.location.search).get("redirect");
```

Client Component 안에서는 동작할 수 있지만, App Router에서는 URL query를 읽을 때 `useSearchParams()`를 사용하는 쪽이 더 자연스럽다.

## 변경 사항

`window.location.search` 직접 접근을 제거하고 `useSearchParams()`로 redirect 값을 읽도록 변경했다.

```tsx
const searchParams = useSearchParams();

router.replace(getSafeRedirectPath(searchParams.get("redirect")));
```

redirect 값 검증은 유지했다.

```ts
function getSafeRedirectPath(redirectPath: string | null): string {
  if (!redirectPath) {
    return "/";
  }

  if (!redirectPath.startsWith("/") || redirectPath.startsWith("//")) {
    return "/";
  }

  return redirectPath;
}
```

이 검증은 외부 URL로 이동하는 open redirect 가능성을 줄이기 위한 것이다.

## Suspense가 필요한 이유

처음에는 `LoginPage` 컴포넌트 안에서 바로 `useSearchParams()`를 호출했다.

하지만 `next build`에서 다음 에러가 발생했다.

```text
useSearchParams() should be wrapped in a suspense boundary at page "/login".
```

Next.js App Router에서 정적 prerender 대상 페이지가 `useSearchParams()`를 사용하면, 해당 client-side query 읽기 구간을 Suspense boundary 안에 두어야 한다.

그래서 로그인 페이지를 다음처럼 분리했다.

```tsx
export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const searchParams = useSearchParams();
  // ...
}
```

`LoginPage`는 Suspense boundary만 담당하고, 실제 form 로직은 `LoginForm`이 담당한다.

## 검증

수정 후 다음 명령을 실행했다.

```bash
pnpm lint:web
pnpm build:web
```

결과:

- `pnpm lint:web` 통과
- `pnpm build:web` 통과

## 정리

이번 수정의 핵심은 다음 세 가지다.

1. App Router URL query 조회는 `useSearchParams()`로 정리했다.
2. redirect 값은 내부 경로만 허용하도록 검증한다.
3. `useSearchParams()`를 사용하는 로그인 form은 Suspense boundary 안에 둔다.

이후 로그인 redirect 관련 로직을 수정할 때는 `window.location.search` 직접 접근보다 `useSearchParams()`를 우선 사용한다.
