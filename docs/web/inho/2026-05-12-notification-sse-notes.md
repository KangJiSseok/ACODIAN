# 알림 SSE 연동 작업 정리

- 작성일: 2026-05-12
- 파일명: `2026-05-12-notification-sse-notes.md`
- 대상 파일:
  - `web/src/app/(protected)/notification/_service/notification-stream.service.ts`
  - `web/src/app/(protected)/notification/_hooks/useNotificationStream.ts`
  - `web/src/app/(protected)/notification/_hooks/index.ts`
  - `web/src/app/_common/components/layout/gnb.tsx`
  - `web/tests/notificationSse.test.mjs`

## 목적

알림 목록은 기존 알림 조회 API로 가져오되, 새 알림이 생겼을 때 GNB의 알림 배지와 알림 센터가 자동으로 갱신되도록 SSE 연결을 추가했다.

백엔드에서 제공하는 SSE 엔드포인트는 다음이다.

```text
GET /notifications/stream
```

## 구현 흐름

1. `Gnb`가 렌더링되면 기존처럼 `useNotificationCenter()`로 최근 미읽음 알림을 조회한다.
2. 같은 위치에서 `useNotificationStream()`을 호출해 로그인 사용자 기준 SSE를 연결한다.
3. `useNotificationStream()`은 auth store에서 `accessToken`과 인증 상태를 읽는다.
4. 인증된 상태이고 access token이 있을 때만 `subscribeNotificationStream()`을 실행한다.
5. SSE에서 `event: notification` 메시지를 받으면 `notificationKeys.all` 쿼리를 invalidate한다.
6. React Query가 기존 알림 목록 API를 다시 조회하면서 미읽음 개수와 알림 센터 목록이 갱신된다.
7. 로그아웃, 토큰 변경, 컴포넌트 언마운트 시 기존 SSE 연결은 `AbortController`로 정리한다.

## 주요 결정

### EventSource 대신 fetch stream 사용

브라우저 기본 `EventSource`는 커스텀 `Authorization` 헤더를 붙이기 어렵다.

현재 프론트는 access token을 localStorage가 아니라 메모리 auth store에만 보관하고, API 요청 시 `Authorization: Bearer ...` 헤더를 붙이는 구조다.

따라서 SSE도 같은 인증 방식을 유지하기 위해 `fetch()`로 `text/event-stream` 응답을 직접 읽도록 구현했다.

### 새 알림을 직접 캐시에 넣지 않음

SSE payload를 받아서 알림 목록 cache에 직접 append할 수도 있지만, 이번 작업에서는 그렇게 하지 않았다.

이유는 알림 목록이 페이지네이션, 읽음 상태, 필터 조건을 가질 수 있기 때문이다. 직접 캐시를 수정하면 어떤 목록에 넣어야 하는지 판단이 복잡해진다.

대신 SSE 이벤트를 받으면 알림 query를 invalidate하고, 기존 조회 API가 다시 서버 상태를 가져오게 했다.

## 어려웠던 부분

### 1. 인증 헤더가 필요한 SSE 연결

가장 큰 이슈는 SSE 연결에도 access token을 보내야 한다는 점이었다.

`EventSource`를 쓰면 구현은 간단하지만 `Authorization` 헤더를 붙이기 어렵고, 토큰을 query string에 넣는 방식은 보안상 좋지 않다.

그래서 `fetch()`와 `ReadableStream`을 사용해 직접 스트림을 읽는 방식으로 우회했다.

### 2. SSE 메시지 파싱

SSE는 JSON만 오는 구조가 아니라 다음처럼 여러 줄로 올 수 있다.

```text
event: notification
data: {"notificationId":1}
```

또 네트워크 chunk가 항상 메시지 단위로 끊겨서 오는 것도 아니다.

그래서 수신 buffer를 유지하면서 빈 줄 기준으로 block을 나누고, `event`, `data` 필드를 직접 파싱했다. heartbeat나 주석 라인은 무시하도록 처리했다.

### 3. 연결 생명주기 정리

사용자가 로그아웃하거나 access token이 바뀌면 이전 SSE 연결이 남아 있으면 안 된다.

이를 위해 hook cleanup에서 subscription의 `close()`를 호출하고, 내부에서는 `AbortController.abort()`와 retry timer 정리를 같이 수행했다.

### 4. 재연결 처리

SSE 연결은 서버 timeout, 네트워크 끊김, 브라우저 상태에 따라 종료될 수 있다.

연결이 정상 종료되거나 오류가 발생하면 기본 3초 뒤 다시 연결하도록 했다. 단, 사용자가 로그아웃하거나 cleanup으로 닫은 경우에는 재연결하지 않는다.

### 5. 기존 seed 알림과 실시간 알림의 차이

DB에 이미 들어있는 seed 알림은 알림 목록 API에서 조회되지만, SSE 이벤트를 새로 발생시키지는 않는다.

즉 기존 알림 확인은 `/notifications/me` 조회로 가능하고, SSE 동작 확인은 백엔드에서 새 알림 생성 이벤트가 실제로 발생해야 한다.

원격 API 기준으로 미읽음 알림이 있는 확인용 계정은 다음이 좋다.

```text
u007@ibank.local / password1!
```

이 계정은 확인 당시 미읽음 알림이 2건 있었다.

## 검증

아래 명령으로 SSE 관련 테스트와 웹 lint를 확인했다.

```bash
node --test web/tests/notificationSse.test.mjs
pnpm --filter ./web lint
```

결과:

- `notificationSse.test.mjs` 통과
- `pnpm --filter ./web lint` 통과

## 추후 고려

- 새 알림 수신 시 단순 갱신뿐 아니라 toast 노출이 필요할 수 있다.
- SSE payload를 직접 cache에 반영할지 여부는 알림 목록 필터/페이지네이션 정책이 더 명확해진 뒤 결정하는 편이 안전하다.
- 백엔드에서 이벤트 종류가 늘어나면 `notification` 외 이벤트 처리 분기를 추가할 수 있다.
