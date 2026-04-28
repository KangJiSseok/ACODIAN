# 로그인 API 연결 및 로컬 검증 정리

- 작성일: 2026-04-27
- 파일명: `2026-04-27-auth-login-local-check.md`
- 대상 파일:
  - `web/src/app/(public)/login/page.tsx`
  - `web/src/app/_common/service/auth.ts`
  - `web/src/app/_common/service/axios.ts`
  - `web/src/proxy.ts`
  - `web/src/app/layout.tsx`
  - `web/.env.example`

## 목적

로그인 화면을 실제 인증 API와 연결하면서 확인한 흐름, 헷갈렸던 지점, 로컬 검증 방법을 문서로 남긴다.

이번 작업의 핵심은 로그인 form에서 `auth.ts`의 `login()`을 호출하고, 성공 시 보호 영역 기본 페이지(`/`)로 이동시키는 것이다.

## 현재 로그인 흐름

로그인 form submit 이후 흐름은 다음과 같다.

1. 사용자가 이메일과 비밀번호를 입력한다.
2. `login({ email, password })`를 호출한다.
3. 프론트가 `POST /auth/login` 요청을 보낸다.
4. 백엔드는 access token을 응답 body가 아니라 `Authorization` 응답 헤더로 내려준다.
5. refresh token은 HttpOnly cookie로 내려온다.
6. 프론트는 `Authorization` 헤더에서 access token 문자열을 추출한다.
7. access token을 auth store에 먼저 저장한다.
8. `GET /users/me`를 호출해 현재 사용자 문맥을 가져온다.
9. access token과 user를 auth store에 함께 저장한다.
10. 로그인 페이지에서 `router.replace("/")`로 기본 보호 페이지로 이동한다.

`auth.ts`가 token 저장과 `/users/me` 조회까지 처리하므로, 로그인 페이지는 성공/실패에 따른 UI 상태와 이동만 담당한다.

## 환경변수 정리

기존에는 API base URL 기본값이 코드에 직접 들어갈 수 있었다.

```ts
const DEFAULT_API_BASE_URL = "http://localhost:8100/api";
```

이 방식은 로컬 주소가 코드에 노출되고, 환경별 API 주소를 코드 수정으로 바꾸게 만들 수 있다.

현재 방향은 `NEXT_PUBLIC_API_BASE_URL`을 필수 환경변수로 보고, 값이 없으면 명확하게 에러를 내는 것이다.

```ts
const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

if (!apiBaseUrl) {
  throw new Error("NEXT_PUBLIC_API_BASE_URL 환경변수가 설정되지 않았습니다.");
}
```

로컬 실행 시 `web/.env.local`에는 다음 값을 둔다.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8100/api
```

저장소에는 팀원이 참고할 수 있도록 `web/.env.example`만 커밋한다.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8100/api
```

주의할 점은 `NEXT_PUBLIC_` prefix가 붙은 값은 브라우저 번들에서 접근 가능한 공개 설정이라는 것이다. API base URL은 secret으로 보지 않지만, 코드에 하드코딩하지 않고 환경별 설정으로 분리하는 목적이 있다.

## 로컬 실행 순서

로컬에서 로그인만 검증할 때는 다음 순서로 실행한다.

1. Postgres와 Redis 실행

```bash
docker compose -f infra/compose.local.yml up -d
```

2. 백엔드 실행

```bash
cd api
./gradlew bootRun
```

API 기본 주소는 다음과 같다.

```text
http://localhost:8100/api
```

3. 프론트 실행

```bash
pnpm install
pnpm --filter ./web dev
```

프론트 기본 주소는 다음과 같다.

```text
http://localhost:3000
```

4. 로그인 페이지 접속

```text
http://localhost:3000/login
```

local profile의 seed 계정 예시는 다음과 같다.

```text
director@ibank.com / password1!
dept@ibank.com / password1!
member@ibank.com / password1!
```

## 로그인 검증 기준

브라우저 개발자도구 Network 탭에서 다음 요청을 확인한다.

- `POST /auth/login`: `200 OK`
- 응답 헤더 `Authorization`: `Bearer ...`
- 응답 헤더 `Set-Cookie`: `refreshToken=...`
- `GET /users/me`: `200 OK`
- 로그인 성공 후 URL이 `/`로 이동

로그인 실패 시에는 `/auth/login`과 `/users/me` 중 어떤 요청이 실패했는지 먼저 확인한다.

- `/auth/login` 실패: 계정 정보, 백엔드 실행 여부, CORS, API base URL을 확인한다.
- `/users/me` 실패: access token 저장, 요청 Authorization 헤더, 백엔드 인증 필터를 확인한다.

## 헷갈렸던 지점

### `/login`에 들어갔는데 대시보드로 이동하는 경우

`web/src/proxy.ts`에는 로그인된 사용자가 `/login`에 접근하면 `/`로 보내는 로직이 있었다.

```ts
if (pathname === "/login" && refreshToken) {
  return NextResponse.redirect(new URL("/", request.url));
}
```

브라우저에 `refreshToken` cookie가 남아 있으면 로그인 화면을 다시 보려 해도 바로 대시보드로 이동할 수 있다.

로그인 UI를 수동 검증하는 동안에는 이 분기를 잠시 끄거나, 브라우저의 localhost cookie를 삭제하고 다시 확인한다.

현재 보호 라우트 차단 자체는 `ENABLE_ROUTE_PROTECTION = false`로 비활성화되어 있다. 따라서 `/`에 직접 들어가면 대시보드가 보이는 것은 정상이다.

### `/logout` URL로 로그아웃하려는 경우

현재 프론트에는 `/logout` 페이지가 없다.

로그아웃은 URL 이동이 아니라 버튼 클릭 액션으로 처리해야 한다.

```ts
await logout();
router.replace("/login");
```

프론트의 `logout()`은 `POST /auth/logout`을 호출하고, 성공/실패와 관계없이 auth store를 초기화한다.

따라서 다음 작업에서는 사이드바 하단에 로그아웃 버튼을 붙이고, 클릭 시 `logout()` 호출 후 `/login`으로 이동시키는 방식이 맞다.

### Next.js smooth scroll 경고

로그인 후 페이지 이동 시 개발자도구에 다음 경고가 나올 수 있다.

```text
Detected `scroll-behavior: smooth` on the `<html>` element.
```

이 경고는 로그인 실패가 아니다. 전역 CSS에서 `html { scroll-behavior: smooth; }`를 사용하고 있는데, Next.js가 라우트 전환 중 스크롤을 제어하면서 의도한 설정인지 알려달라는 의미다.

유지하려면 root layout의 `<html>`에 다음 속성을 추가한다.

```tsx
<html lang="ko" data-scroll-behavior="smooth">
```

smooth scroll이 필요 없다면 전역 CSS의 `scroll-behavior: smooth`를 제거한다.

## 이번 변경에 포함된 작업

- 로그인 placeholder UI를 실제 form으로 변경
- 로그인 submit 시 `login()` 서비스 호출
- 로그인 요청 중 버튼 disabled 처리
- 로그인 실패 메시지 표시
- 로그인 성공 시 `/`로 이동
- API base URL 하드코딩 제거
- `web/.env.example` 추가
- smooth scroll 경고 대응을 위한 root html 속성 추가
- 로그인 수동 검증을 위해 `/login` 자동 리다이렉트 임시 비활성화

## 아직 남은 작업

- 사이드바 하단 로그아웃 버튼 연결
- 앱 초기 진입 시 `refreshSession()`으로 세션 복구
- 보호 라우트 접근 제어 재활성화
- 로그인한 사용자가 `/login` 접근 시 `/`로 이동하는 proxy 정책 복구
- 로그아웃 후 refresh cookie 삭제와 `/login` 이동 검증

## 커밋 전 확인할 점

`web/next-env.d.ts`는 Next.js가 자동으로 갱신하는 파일이다.

현재 변경 내용이 실제 작업 의도와 관련 없다면 커밋에서 제외하는 것이 좋다.

```diff
- import "./.next/types/routes.d.ts";
+ import "./.next/dev/types/routes.d.ts";
```

이 파일은 주석에도 "This file should not be edited"라고 되어 있으므로, 커밋 전 포함 여부를 확인한다.

## 정리

이번 작업은 인증 API 서비스가 준비된 상태에서 로그인 화면을 실제 동작으로 연결한 단계다.

아직 전체 인증 흐름이 완성된 것은 아니며, 로그인 이후 새로고침 복구와 보호 라우트 정책은 후속 작업으로 남아 있다.
