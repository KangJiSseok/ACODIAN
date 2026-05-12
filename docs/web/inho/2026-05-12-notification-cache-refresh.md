# 알림 캐시 갱신 이슈 정리

- 작성일: 2026-05-12
- 파일명: `2026-05-12-notification-cache-refresh.md`
- 대상 파일:
  - `web/src/app/(protected)/notification/_hooks/useNotificationList.ts`
  - `web/tests/notificationCacheRefresh.test.mjs`

## 문제

알림 탭이나 GNB 알림센터에 진입했을 때 실제 서버에는 알림이 있어도 화면에는 바로 보이지 않고, 사용자가 새로고침을 눌러야 보일 수 있는 문제가 있었다.

기존 알림 목록 query key는 요청 파라미터만 기준으로 구성되어 있었다.

```ts
["notifications", "list", params]
```

이 구조에서는 로그인 사용자가 바뀌어도 같은 파라미터를 쓰면 이전 알림 캐시가 재사용될 여지가 있었다. 특히 이전 조회 결과가 빈 목록이면 새 사용자도 잠깐 빈 목록처럼 보일 수 있었다.

## 수정

알림 query key에 현재 로그인 사용자의 `userId`를 포함했다.

```ts
["notifications", "user", userId, "list", params]
```

또한 알림 목록 query를 다음 조건으로 조정했다.

- `userId`가 확인된 뒤에만 알림 API 호출
- `staleTime: 0`으로 알림 데이터를 즉시 stale 처리
- `refetchOnMount: "always"`로 알림 탭이나 알림센터가 다시 마운트될 때 최신 목록 재조회

이렇게 하면 로그인 직후나 사용자 전환 이후에도 이전 사용자의 알림 캐시가 섞이지 않고, 화면 진입 시 서버 기준 최신 알림을 다시 확인한다.

## 알림센터 정책

현재 GNB 알림센터는 전체 알림이 아니라 최근 미읽음 알림만 조회한다.

```ts
{
  isRead: false,
  page: 1,
  pageSize: 3,
}
```

따라서 이미 읽은 알림만 있는 계정은 알림 탭의 전체 목록에는 표시될 수 있지만, 알림센터에는 표시되지 않는다. 알림센터를 "최근 알림"으로 확장하려면 목록 조회와 미읽음 카운트 조회를 분리하는 방식이 더 자연스럽다.

## SSE와의 관계

SSE는 알림 목록의 원본 데이터가 아니라 새 알림 발생 신호에 가깝다.

프론트는 SSE를 받으면 알림 query를 invalidate해서 기존 `GET /notifications/me` 조회 API로 다시 목록을 가져온다. 따라서 모든 알림은 조회 API에서 확인 가능해야 하고, 실시간 반영이 필요한 알림은 SSE 이벤트도 함께 발행되는 구조가 좋다.

현재 마감 임박/지연 알림처럼 매일 스케줄러로 생성되는 배치성 알림은 로그인 또는 알림 탭 진입 시 조회 API로 가져오면 충분하다. 추후 AI 업무 완료처럼 즉시성이 중요한 알림은 SSE 이벤트 발행이 필요하다.

## 검증

```bash
node --test web/tests/notificationCacheRefresh.test.mjs web/tests/notificationSse.test.mjs web/tests/notificationFilterAccess.test.mjs
pnpm --filter ./web lint
```

검증 결과:

- 알림 캐시 갱신 테스트 통과
- 기존 알림 SSE 테스트 통과
- 기존 알림 필터 접근 테스트 통과
- web lint 통과
