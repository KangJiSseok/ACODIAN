# Notification API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자 개인 알림 목록, unread count, 일괄/단건 읽음 처리 계약을 정의한다. 알림은 `tb_notification` 단일 테이블을 기준으로 한다.

## 2. 주요 ERD 연관
- `tb_notification`
- `tb_user`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/notification/list` | `Documented` | 사용자 개인 알림 목록을 조회한다. |
| `GET` | `/api/notification/unread-count` | `Documented` | 읽지 않은 알림 수를 경량 조회한다. |
| `PATCH` | `/api/notification/read-all` | `Documented` | 본인 알림을 일괄 읽음 처리한다. |
| `PATCH` | `/api/notification/{id}/read` | `Inferred-required` | 개별 알림 클릭 시 단건 읽음 처리를 수행한다. |

## 5. 엔드포인트 상세

### GET /api/notification/list
- 목적: 사용자 개인 알림 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신의 알림만 조회한다.
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `isRead`, `notificationType` [추론]
- 응답 (`data` 기준)
  - `PageResponse<NotificationSummary>`
  - `items[*]`: `notificationId`, `notificationType`, `title`, `content`, `referenceType`, `referenceId`, `isRead`, `readAt`, `createdAt`
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "isRead": false,
    "notificationType": "WORKLOG"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "notificationId": 801,
        "notificationType": "WORKLOG",
        "title": "업무 상태 변경",
        "content": "재고 동기화 개선 업무가 완료되었습니다.",
        "referenceType": "WORKLOG",
        "referenceId": 9001,
        "isRead": false,
        "readAt": null,
        "createdAt": "2026-04-21T03:00:00Z"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "totalCount": 1,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true,
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_notification`
- 근거
  - class mapping ownership: `domain.notification.controller.NotificationController` / `domain.notification.service.NotificationService`
  - 관련 entity/context: domain.notification.entity.Notification, domain.notification.repository.NotificationRepository, domain.notification.repository.jooq.NotificationJooqRepository
  - 메모: 알림 조회 ownership은 NotificationService의 readOnly 메서드가 가진다.
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ERD `tb_notification`

### GET /api/notification/unread-count
- 목적: 읽지 않은 알림 수를 경량 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신의 읽지 않은 알림 수를 조회한다.
- 요청
  - 추가 파라미터 없음
- 응답 (`data` 기준)
  - `unreadCount`
- 요청 JSON 예시
```json
{}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "unreadCount": 3
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
- ERD 연관
  - `tb_notification.is_read`
- 근거
  - class mapping ownership: `domain.notification.controller.NotificationController` / `domain.notification.service.NotificationService`
  - 관련 entity/context: domain.notification.entity.Notification, domain.notification.repository.NotificationRepository, domain.notification.repository.jooq.NotificationJooqRepository
  - 메모: GNB count 용도라 단건 카운트에 집중한다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_notification`

### PATCH /api/notification/read-all
- 목적: 본인 알림을 일괄 읽음 처리한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신의 알림만 일괄 읽음 처리한다.
- 요청
  - 선택 Body: `before`(특정 시점 이전만 처리) [추론]
- 응답 (`data` 기준)
  - 처리 건수 `updatedCount` [추론]
- 요청 JSON 예시
```json
{
  "body": {
    "before": "2026-04-21T03:00:00Z"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "updatedCount": 3
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
- ERD 연관
  - `tb_notification.is_read`
  - `tb_notification.read_at`
- 근거
  - class mapping ownership: `domain.notification.controller.NotificationController` / `domain.notification.service.NotificationService`
  - 관련 entity/context: domain.notification.entity.Notification, domain.notification.repository.NotificationRepository
  - 메모: 쓰기 ownership은 NotificationService에 둔다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_notification`

### PATCH /api/notification/{id}/read
- 목적: 개별 알림 클릭 시 단건 읽음 처리를 수행한다.
- 상태: `Inferred-required`
- 권한/접근 주체: 인증된 사용자가 자신의 단건 알림만 읽음 처리한다.
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 변경된 `isRead=true`, `readAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 801
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "notificationId": 801,
    "isRead": true,
    "readAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `NOTIFICATION_NOT_FOUND` [추론]
  - 대표 오류: `NOTIFICATION_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_notification`
- 근거
  - class mapping ownership: `domain.notification.controller.NotificationController` / `domain.notification.service.NotificationService`
  - 관련 entity/context: domain.notification.entity.Notification, domain.notification.repository.NotificationRepository
  - 메모: UI 흐름 기반의 필수 추론 API다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_notification`

## 6. 추론 메모
- 알림 목록은 사용자 본인 범위만 허용하는 self-service API 로 본다. [추론]
