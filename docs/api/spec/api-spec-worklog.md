# Worklog API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
업무일지의 핵심 CRUD, 상태 이력, AI 요약/태그/처리상태 반영 계약을 정의한다. 업무 원본은 `api` 가 소유하고 AI 결과 반영은 원장 경계를 통해 이루어진다.

## 2. 주요 ERD 연관
- `tb_worklog`
- `tb_worklog_status_history`
- `tb_worklog_tag`
- `tb_meta_tag`
- `tb_file`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)
- [../../AX-WMS_기획서_아키텍처가이드.md](../../AX-WMS_기획서_아키텍처가이드.md)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/worklog/list` | `Documented` | 업무일지 목록과 필터링/정렬 결과를 조회한다. |
| `GET` | `/api/worklog/{id}` | `Documented` | 단일 업무일지 상세와 AI/태그/소속 문맥을 조회한다. |
| `POST` | `/api/worklog` | `Documented` | 새 업무일지를 생성하고 초기 AI 파이프라인을 트리거한다. |
| `PUT` | `/api/worklog/{id}` | `Documented` | 업무일지 기본 정보와 재처리 플래그를 수정한다. |
| `DELETE` | `/api/worklog/{id}` | `Documented` | 업무일지를 삭제 또는 soft delete 처리한다. |
| `PATCH` | `/api/worklog/{id}/status` | `Documented` | 업무 상태 전이와 이력 기록을 담당한다. |
| `GET` | `/api/worklog/{id}/history` | `Documented` | 업무 상태 변경 이력을 조회한다. |
| `PATCH` | `/api/worklog/{id}/summary` | `Documented` | AI가 생성한 업무 요약을 원장 DB에 반영한다. |
| `PUT` | `/api/worklog/{id}/tags` | `Documented` | AI가 생성/정규화한 태그 연결을 업무일지에 반영한다. |
| `PATCH` | `/api/worklog/{id}/ai-status` | `Documented` | 업무일지 AI 처리 진행 상태를 반영한다. |

## 5. 엔드포인트 상세

### GET /api/worklog/list
- 목적: 업무일지 목록과 필터링/정렬 결과를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신의 범위 또는 관리 범위의 업무일지를 조회한다. 상위 역할일수록 더 넓은 조직 범위를 가진다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `teamId`, `authorId`, `statusCode`, `importanceCode`, `dueDateFrom`, `dueDateTo`, `keyword`, `tagId` [추론]
- 응답 (`data` 기준)
  - `PageResponse<WorklogSummary>`
  - `items[*]`: `worklogId`, `title`, `authorId`, `teamId`, `statusCode`, `importanceCode`, `dueDate`, `completionDate`, `aiProcessingStatus`, `tagNames` [추론], `fileCount` [추론]
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "teamId": 21,
    "authorId": 101,
    "statusCode": "IN_PROGRESS",
    "importanceCode": "HIGH",
    "keyword": "재고",
    "tagId": 301
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
        "worklogId": 9001,
        "title": "재고 동기화 개선",
        "authorId": 101,
        "teamId": 21,
        "statusCode": "IN_PROGRESS",
        "importanceCode": "HIGH",
        "dueDate": "2026-04-30",
        "completionDate": null,
        "aiProcessingStatus": "COMPLETED",
        "tagNames": [
          "재고",
          "자동화"
        ],
        "fileCount": 2
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
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_worklog_tag`
  - `tb_file`
- 근거
  - class mapping ownership: `domain.worklog.controller.WorklogController` / `domain.worklog.service.WorklogService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.repository.WorklogRepository, domain.worklog.repository.jooq.WorklogJooqRepository
  - 메모: 목록 조회는 Service의 readOnly 메서드 + JOOQ 조합 여지를 전제로 둔다.
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ERD `tb_worklog`

### GET /api/worklog/{id}
- 목적: 단일 업무일지 상세와 AI/태그/소속 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자, 소속 팀 리더, 상위 조직 관리자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `worklogId`, `title`, `requestContent`, `workContent`, `statusCode`, `importanceCode`, `actualHours`, `instructionDate`, `dueDate`, `completionDate`, `aiSummary`, `aiSummaryEdited`, `aiProcessingStatus`
  - `author`, `team`, `tags`, `files`, `dependencies` 요약 [추론]
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "title": "재고 동기화 개선",
    "requestContent": "재고 배치 처리 속도 개선 요청",
    "workContent": "동기화 배치 병렬 처리 구조를 적용했다.",
    "statusCode": "IN_PROGRESS",
    "importanceCode": "HIGH",
    "actualHours": 6.5,
    "instructionDate": "2026-04-18",
    "dueDate": "2026-04-30",
    "completionDate": null,
    "aiSummary": "재고 동기화 처리 최적화 작업",
    "aiSummaryEdited": false,
    "aiProcessingStatus": "COMPLETED",
    "author": {
      "userId": 101,
      "userName": "홍길동"
    },
    "team": {
      "teamId": 21,
      "teamName": "물류혁신TF"
    },
    "tags": [
      {
        "tagId": 301,
        "tagName": "재고"
      }
    ],
    "files": [
      {
        "fileId": 7001,
        "originalName": "inventory-plan.pdf"
      }
    ],
    "dependencies": [
      {
        "worklogId": 8990,
        "title": "재고 스키마 정리"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_worklog_tag`
  - `tb_file`
  - `tb_worklog_dependency`
- 근거
  - class mapping ownership: `domain.worklog.controller.WorklogController` / `domain.worklog.service.WorklogService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.entity.WorklogTag, domain.worklog.repository.WorklogRepository, domain.worklog.repository.jooq.WorklogJooqRepository
  - 메모: 상세 조회는 태그/파일/조직 문맥을 함께 조합할 수 있다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog`
  - source: architecture guide

### POST /api/worklog
- 목적: 새 업무일지를 생성하고 초기 AI 파이프라인을 트리거한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신이 속한 팀 범위에서 업무일지를 생성한다. 팀 리더/관리자는 대리 등록 가능성을 가진다. [추론]
- 요청
  - Body: `teamId`, `title`, `requestContent`, `workContent`, `importanceCode`, `instructionDate`, `dueDate`, `actualHours` [추론]
  - 선택적으로 `dependencyIds`, `tagIds` [추론]
- 응답 (`data` 기준)
  - 생성된 업무일지 기준 정보
  - `aiProcessingStatus=PENDING` 초기값 또는 후처리 트리거 결과 [추론]
- 요청 JSON 예시
```json
{
  "body": {
    "teamId": 21,
    "title": "재고 동기화 개선",
    "requestContent": "재고 배치 처리 속도 개선 요청",
    "workContent": "동기화 배치 병렬 처리 구조를 적용한다.",
    "importanceCode": "HIGH",
    "instructionDate": "2026-04-18",
    "dueDate": "2026-04-30",
    "actualHours": 0,
    "dependencyIds": [
      8990
    ],
    "tagIds": [
      301,
      302
    ]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "teamId": 21,
    "title": "재고 동기화 개선",
    "statusCode": "PENDING",
    "aiProcessingStatus": "PENDING"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `TEAM_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_worklog_dependency`
  - `tb_worklog_tag`
- 근거
  - class mapping ownership: `domain.worklog.controller.WorklogController` / `domain.worklog.service.WorklogService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.repository.WorklogRepository
  - 메모: 생성 ownership은 WorklogService가 가진다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog`
  - source: architecture guide

### PUT /api/worklog/{id}
- 목적: 업무일지 기본 정보와 재처리 플래그를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자 또는 관리 권한 사용자가 수정한다. 완료/삭제 상태 등 일부 전이는 추가 검증이 필요하다. [추론]
- 요청
  - Path: `id`
  - Body: `title`, `requestContent`, `workContent`, `importanceCode`, `actualHours`, `instructionDate`, `dueDate`, `completionDate`, `reprocessAi` [추론]
- 응답 (`data` 기준)
  - 수정된 업무일지 요약
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "title": "재고 동기화 개선",
    "requestContent": "재고 배치 처리 속도 개선 요청",
    "workContent": "동기화 배치 병렬 처리 구조와 로그 추적을 추가했다.",
    "importanceCode": "HIGH",
    "actualHours": 8.0,
    "instructionDate": "2026-04-18",
    "dueDate": "2026-05-02",
    "completionDate": null,
    "reprocessAi": true
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "title": "재고 동기화 개선",
    "dueDate": "2026-05-02",
    "aiProcessingStatus": "PENDING"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_INVALID_STATE` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog`
- 근거
  - class mapping ownership: `domain.worklog.controller.WorklogController` / `domain.worklog.service.WorklogService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.repository.WorklogRepository
  - 메모: 수정 시 AI 재생성 정책을 함께 다룰 수 있다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog`

### DELETE /api/worklog/{id}
- 목적: 업무일지를 삭제 또는 soft delete 처리한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자 또는 상위 조직 관리자가 soft delete 를 수행한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 삭제 결과만 반환한다. [추론]
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "isDeleted": true,
    "message": "업무일지가 삭제 처리되었습니다."
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog.is_deleted`
- 근거
  - class mapping ownership: `domain.worklog.controller.WorklogController` / `domain.worklog.service.WorklogService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.repository.WorklogRepository
  - 메모: 실제 삭제 정책은 후속 구현에서 결정하되 ownership은 동일하다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog`

### PATCH /api/worklog/{id}/status
- 목적: 업무 상태 전이와 이력 기록을 담당한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자, 팀 리더, 상위 조직 관리자가 상태 전이를 수행한다. 상태 전이는 Service 레이어에서 소유권과 비즈니스 규칙을 함께 검증한다.
- 요청
  - Path: `id`
  - Body: `newStatusCode`, `reason` [추론]
- 응답 (`data` 기준)
  - 변경 후 `statusCode`, `changedAt`, `changedBy` [추론]
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "newStatusCode": "COMPLETED",
    "reason": "QA 확인 완료"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "statusCode": "COMPLETED",
    "changedAt": "2026-04-21T03:00:00Z",
    "changedBy": 101
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_INVALID_STATUS_TRANSITION` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog.status_code`
  - `tb_worklog_status_history`
- 근거
  - class mapping ownership: `domain.worklog.controller.WorklogController` / `domain.worklog.service.WorklogStatusService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.entity.WorklogStatusHistory, domain.worklog.repository.WorklogStatusHistoryRepository, domain.worklog.repository.jooq.WorklogStatusHistoryJooqRepository
  - 메모: 상태 전이 규칙은 WorklogStatusPolicy와 함께 읽는다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ERD `tb_worklog_status_history`

### GET /api/worklog/{id}/history
- 목적: 업무 상태 변경 이력을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 업무 상세 조회 권한이 있는 사용자만 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `history[*]`: `historyId`, `previousStatusCode`, `newStatusCode`, `reason`, `changedAt`, `changedBy`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "history": [
      {
        "historyId": 1,
        "previousStatusCode": "PENDING",
        "newStatusCode": "IN_PROGRESS",
        "reason": "작업 시작",
        "changedAt": "2026-04-19T09:00:00Z",
        "changedBy": 101
      },
      {
        "historyId": 2,
        "previousStatusCode": "IN_PROGRESS",
        "newStatusCode": "COMPLETED",
        "reason": "QA 확인 완료",
        "changedAt": "2026-04-21T03:00:00Z",
        "changedBy": 101
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog_status_history`
- 근거
  - class mapping ownership: `domain.worklog.controller.WorklogController` / `domain.worklog.service.WorklogStatusService`
  - 관련 entity/context: domain.worklog.entity.WorklogStatusHistory, domain.worklog.repository.WorklogStatusHistoryRepository, domain.worklog.repository.jooq.WorklogStatusHistoryJooqRepository
  - 메모: 이력 조회 ownership은 status 서비스에 둔다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog_status_history`

### PATCH /api/worklog/{id}/summary
- 목적: AI가 생성한 업무 요약을 원장 DB에 반영한다.
- 상태: `Documented`
- 권한/접근 주체: 내부 AI 후처리 또는 관리자 보정 경로가 호출하는 반영 API로 본다. 외부 사용자 직접 호출은 제한될 수 있다. [추론]
- 요청
  - Path: `id`
  - Body: `aiSummary`, `aiSummaryEdited` [추론]
- 응답 (`data` 기준)
  - 반영된 `aiSummary`, `aiSummaryEdited`, `updatedAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "aiSummary": "재고 동기화 배치 최적화 및 로그 보강 작업",
    "aiSummaryEdited": false
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "aiSummary": "재고 동기화 배치 최적화 및 로그 보강 작업",
    "aiSummaryEdited": false,
    "updatedAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_AI_UPDATE_FORBIDDEN` [추론]
- ERD 연관
  - `tb_worklog.ai_summary`
  - `tb_worklog.ai_summary_edited`
- 근거
  - class mapping ownership: `domain.worklog.controller.InternalWorklogAiCallbackController` / `domain.worklog.service.InternalWorklogAiCallbackService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.repository.WorklogRepository
  - 메모: 현행 코드 기준 internal callback controller base path는 /api/internal/worklog 이지만, endpoint/status 표기는 api-design 상세 본문 계약(/api/{domain}/{id}/{action})을 우선 따른다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog`
  - source: architecture guide

### PUT /api/worklog/{id}/tags
- 목적: AI가 생성/정규화한 태그 연결을 업무일지에 반영한다.
- 상태: `Documented`
- 권한/접근 주체: AI 후처리 파이프라인 또는 관리자 보정 경로가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `tags[]` (`tagId` 또는 `tagName`, `isAiGenerated`) [추론]
- 응답 (`data` 기준)
  - 반영 후 태그 세트와 정규화 결과 [추론]
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "tags": [
      {
        "tagId": 301,
        "tagName": "재고",
        "isAiGenerated": true
      },
      {
        "tagId": 302,
        "tagName": "자동화",
        "isAiGenerated": true
      }
    ]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "tags": [
      {
        "tagId": 301,
        "tagName": "재고"
      },
      {
        "tagId": 302,
        "tagName": "자동화"
      }
    ],
    "updatedAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `TAG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_AI_UPDATE_FORBIDDEN` [추론]
- ERD 연관
  - `tb_worklog_tag`
  - `tb_meta_tag`
- 근거
  - class mapping ownership: `domain.worklog.controller.InternalWorklogAiCallbackController` / `domain.worklog.service.InternalWorklogAiCallbackService`
  - 관련 entity/context: domain.worklog.entity.WorklogTag, domain.worklog.repository.WorklogTagRepository, domain.worklog.repository.jooq.WorklogTagJooqRepository
  - 메모: 현행 코드 기준 internal callback controller base path는 /api/internal/worklog 이지만, endpoint/status 표기는 api-design 상세 본문 계약(/api/{domain}/{id}/{action})을 우선 따른다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog_tag`
  - source: ERD `tb_meta_tag`

### PATCH /api/worklog/{id}/ai-status
- 목적: 업무일지 AI 처리 진행 상태를 반영한다.
- 상태: `Documented`
- 권한/접근 주체: 내부 AI 처리 상태 반영용 API로 본다. 일반 사용자 직접 호출은 허용하지 않는다. [추론]
- 요청
  - Path: `id`
  - Body: `aiProcessingStatus`, `message` [추론]
- 응답 (`data` 기준)
  - 변경된 `aiProcessingStatus`, `updatedAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "aiProcessingStatus": "COMPLETED",
    "message": "요약과 태그 생성 완료"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "worklogId": 9001,
    "aiProcessingStatus": "COMPLETED",
    "updatedAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_AI_STATUS_INVALID` [추론]
- ERD 연관
  - `tb_worklog.ai_processing_status`
- 근거
  - class mapping ownership: `domain.worklog.controller.InternalWorklogAiCallbackController` / `domain.worklog.service.InternalWorklogAiCallbackService`
  - 관련 entity/context: domain.worklog.entity.Worklog, domain.worklog.repository.WorklogRepository
  - 메모: 현행 코드 기준 internal callback controller base path는 /api/internal/worklog 이지만, endpoint/status 표기는 api-design 상세 본문 계약(/api/{domain}/{id}/{action})을 우선 따른다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_worklog`

## 6. 추론 메모
- AI callback 계열(`summary`, `tags`, `ai-status`)은 내부 AI 처리 결과를 원장에 반영하는 계약으로 기술했다. [추론]
- 업무 삭제는 soft delete(`is_deleted`) 중심으로 서술했다. [추론]
