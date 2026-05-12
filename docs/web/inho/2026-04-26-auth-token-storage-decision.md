# accessToken 저장 방식 결정 정리

- 작성일: 2026-04-26
- 파일명: `2026-04-26-auth-token-storage-decision.md`
- 대상 파일:
  - `web/src/app/_common/store/auth.store.ts`
  - `web/src/app/_common/hooks/useAuth.ts`

## 목적

프론트 인증 store를 구성하면서 accessToken을 어디에 저장할지 결정한 배경을 문서로 남긴다.

현재 결정은 accessToken을 `localStorage`나 `sessionStorage`에 저장하지 않고, Zustand auth store의 메모리 상태에만 유지하는 것이다.

## 배경

백엔드 인증 문서 기준으로 로그인 응답은 다음 흐름을 가진다.

- accessToken은 응답 헤더의 `Authorization`으로 전달된다.
- refreshToken은 HttpOnly cookie로 관리된다.
- 현재 사용자 정보는 `/api/users/me` 응답을 기준으로 가져온다.

프론트는 로그인 이후 accessToken을 API 요청에 사용해야 하지만, 브라우저 저장소에 오래 남길수록 토큰 탈취 위험이 커진다.

## 결정 사항

accessToken은 현재 탭의 메모리 상태에만 저장한다.

구현 기준:

- `auth.store.ts`의 `accessToken` state로만 보관한다.
- `localStorage`에 accessToken을 저장하지 않는다.
- `sessionStorage`에도 accessToken을 저장하지 않는다.
- 새로고침 이후 필요한 인증 복구는 추후 refresh API와 `/api/users/me` 호출 흐름에서 처리한다.

## localStorage에 저장하지 않는 이유

`localStorage`는 브라우저에 값이 지속적으로 남고, 같은 origin에서 실행되는 JavaScript가 접근할 수 있다.

만약 XSS 취약점이 생기면 공격자가 주입한 스크립트가 `localStorage`에 접근해 accessToken을 읽어갈 수 있다.

accessToken은 API 호출 권한을 가진 값이므로 탈취되면 사용자 권한으로 요청을 보낼 수 있다.

따라서 accessToken을 브라우저 저장소에 장기 보관하는 방식은 현재 단계에서 피하는 것이 맞다고 판단했다.

## 메모리 저장 방식의 장점

메모리 저장 방식은 브라우저 새로고침이나 탭 종료 시 accessToken이 사라진다.

이 특성 때문에 토큰이 브라우저 저장소에 오래 남지 않고, XSS가 발생했을 때 탈취 가능한 지속 저장 위치를 줄일 수 있다.

또한 auth store가 accessToken 보관 책임을 명확히 가지므로, 이후 인증 API 연동 시 토큰 갱신 흐름을 한 곳에서 정리하기 쉽다.

## 트레이드오프

새로고침 시 메모리에 있던 accessToken은 사라진다.

따라서 사용자가 새로고침하면 프론트는 인증 상태를 즉시 확정할 수 없고, refreshToken cookie를 이용한 accessToken 재발급 또는 `/api/users/me` 재조회 흐름이 필요하다.

이 과정에서 초기 화면 로딩 시간이 조금 늘어날 수 있다.

다만 accessToken을 장기 저장소에 두지 않는 보안 이점이 더 크다고 판단했다.

## 현재 구현 범위

현재 브랜치에서는 인증 API 호출까지 구현하지 않고 store 골격만 잡았다.

반영된 내용:

- `AuthStatus`로 인증 상태 흐름 정의
- `accessToken` 메모리 상태 정의
- `/api/users/me` 기준의 `AuthUser` 타입 정의
- `setAuth`, `resetAuth` 등 상태 변경 액션 정의
- 컴포넌트용 `useAuth` hook 추가

의도적으로 제외한 내용:

- 로그인 API 호출
- refresh API 호출
- 요청 interceptor 연결
- 새로고침 시 인증 복구 로직
- accessToken의 localStorage/sessionStorage 저장

## 후속 구현 시 확인할 점

refresh API가 확정되면 앱 초기 진입 시 인증 복구 흐름을 정해야 한다.

예상 흐름:

1. 앱 초기 진입 시 auth 상태를 `loading`으로 둔다.
2. refreshToken cookie 기반으로 accessToken 재발급을 시도한다.
3. 재발급에 성공하면 `/api/users/me`로 사용자 문맥을 가져온다.
4. accessToken과 사용자 정보를 `setAuth`로 한 번에 반영한다.
5. 실패하면 `unauthenticated` 상태로 전환한다.

이 흐름은 백엔드 refresh API 명세가 확정된 뒤 구현한다.

## 정리

이번 결정의 핵심은 accessToken을 편의성 때문에 브라우저 저장소에 남기지 않는 것이다.

새로고침 시 인증 복구 과정이 필요해지는 비용은 있지만, XSS 상황에서 토큰이 장기 저장소에 노출되는 위험을 줄이는 방향이 현재 프로젝트에 더 적합하다고 판단했다.
