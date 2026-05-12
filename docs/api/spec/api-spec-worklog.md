# Worklog API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
업무일지의 핵심 CRUD, 상태 이력, AI 요약/태그/처리상태 반영 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/worklogs/*` 를 사용한다.

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
| `GET` | `/api/worklogs` | `Documented` | 업무일지 목록과 필터링/정렬 결과를 조회한다. |
| `GET` | `/api/worklogs/{id}` | `Documented` | 단일 업무일지 상세와 AI/태그/소속 문맥을 조회한다. |
| `POST` | `/api/worklogs` | `Documented` | 새 업무일지를 생성하고 초기 AI 파이프라인을 트리거한다. |
| `PUT` | `/api/worklogs/{id}` | `Documented` | 업무일지 기본 정보를 수정한다. |
| `DELETE` | `/api/worklogs/{id}` | `Documented` | 업무일지를 삭제 또는 soft delete 처리한다. |
| `PATCH` | `/api/worklogs/{id}/status` | `Documented` | 업무 상태를 전이한다. |
| `GET` | `/api/worklogs/{id}/history` | `Documented` | 업무 상태 변경 이력을 조회한다. |
| `PATCH` | `/api/worklogs/{id}/summary` | `Documented` | AI 요약을 반영한다. |
| `PUT` | `/api/worklogs/{id}/tags` | `Documented` | 태그 연결을 반영한다. |
| `PATCH` | `/api/worklogs/{id}/ai-status` | `Documented` | AI 처리 상태를 반영한다. |

## 5. 엔드포인트 상세

### GET /api/worklogs
- 목적: 업무일지 목록과 필터링/정렬 결과를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신의 범위 또는 관리 범위의 업무일지를 조회한다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `teamId`, `authorId`, `statusCode`, `importanceCode`, `dueDateFrom`, `dueDateTo`, `keyword`
  - Query: `tagId[]` 또는 반복 query 형태의 `tagId` 리스트 지원
- 응답 (`data` 기준)
  - `PageResponse<WorklogSummary>`
  - `items[*]`: `worklogId`, `title`, `authorId`, `authorName`, `teamId`, `teamName`, `statusCode`, `importanceCode`, `dueDate`, `completionDate`, `aiSummary`, `aiProcessingStatus`, `tagNames`, `fileCount`
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
    "tagId": [301, 302]
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
        "authorName": "홍길동",
        "teamId": 21,
        "teamName": "물류혁신TF",
        "statusCode": "IN_PROGRESS",
        "importanceCode": "HIGH",
        "dueDate": "2026-04-30",
        "completionDate": null,
        "aiSummary": "재고 동기화 처리 최적화 작업",
        "aiProcessingStatus": "COMPLETED",
        "tagNames": ["재고", "자동화"],
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
- ERD 연관
  - `tb_worklog`
  - `tb_worklog_tag`
  - `tb_file`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007

### GET /api/worklogs/{id}
- 목적: 단일 업무일지 상세와 AI/태그/소속 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자, 소속 팀 리더, 상위 조직 관리자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `worklogId`, `title`, `requestContent`, `workContent`, `statusCode`, `importanceCode`, `actualHours`, `instructionDate`, `dueDate`, `completionDate`, `aiSummary`, `aiSummaryEdited`, `aiProcessingStatus`
  - `author`: `userId`, `userName`, `titleName`, `departmentName`
  - `team`: `teamId`, `teamName`
  - `tags[*]`: `tagId`, `tagName`
  - `files[*]`: `fileId`, `originalName`, `aiSummary`, `aiProcessingStatus`
  - `dependencies[*]`: `worklogId`, `title`
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
      "userName": "홍길동",
      "titleName": "팀장",
      "departmentName": "물류본부"
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
        "originalName": "inventory-plan.pdf",
        "aiSummary": "재고 계획 문서 요약",
        "aiProcessingStatus": "COMPLETED"
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
- 근거
  - source: checklist
  - source: class mapping

### POST /api/worklogs
- 목적: 새 업무일지를 생성하고 초기 AI 파이프라인을 트리거한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신이 속한 팀 범위에서 생성한다. [추론]
- 요청
  - 실제 전송은 `multipart/form-data` 또는 파일 업로드를 포함하는 요청으로 처리할 수 있다.
  - Body: `teamId`, `title`, `requestContent`, `workContent`, `importanceCode`, `instructionDate`, `dueDate`, `actualHours`, `dependencyIds`, `files[]`
  - 생성 요청에는 `tagIds` 를 두지 않는다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "note": "실제 요청은 파일 업로드를 포함할 수 있다.",
  "body": {
    "teamId": 21,
    "title": "재고 동기화 개선",
    "requestContent": "재고 배치 처리 속도 개선 요청",
    "workContent": "동기화 배치 병렬 처리 구조를 적용한다.",
    "importanceCode": "HIGH",
    "instructionDate": "2026-04-18",
    "dueDate": "2026-04-30",
    "actualHours": 0,
    "dependencyIds": [8990],
    "files": ["inventory-plan.pdf"]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `TEAM_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_file`
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/worklogs/{id}
- 목적: 업무일지 기본 정보를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자 또는 관리 권한 사용자가 수정한다. [추론]
- 요청
  - Path: `id`
  - 실제 전송은 파일 업로드를 포함할 수 있다.
  - Body: `title`, `requestContent`, `workContent`, `importanceCode`, `actualHours`, `instructionDate`, `dueDate`, `completionDate`, `files[]`, `reprocessAi` [추론]
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
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
    "files": ["inventory-plan-v2.pdf"],
    "reprocessAi": true
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_INVALID_STATE` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_file`
- 근거
  - source: checklist
  - source: class mapping

### DELETE /api/worklogs/{id}
- 목적: 업무일지를 삭제 또는 soft delete 처리한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자 또는 상위 관리자만 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
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
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `WORKLOG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog`
- 근거
  - source: checklist
  - source: class mapping

### PATCH /api/worklogs/{id}/status
- 목적: 업무 상태를 전이한다.
- 상태: `Documented`
- 권한/접근 주체: 작성자 또는 관리 권한 사용자가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `statusCode`, `reason` [추론]
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "statusCode": "DONE",
    "reason": "검수 완료"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_INVALID_STATUS_TRANSITION` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
- ERD 연관
  - `tb_worklog.status_code`
  - `tb_worklog_status_history`
- 근거
  - source: checklist
  - source: class mapping

### GET /api/worklogs/{id}/history
- 목적: 업무 상태 변경 이력을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 업무 상세를 볼 수 있는 사용자만 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `items[*]`: `historyId`, `fromStatusCode`, `toStatusCode`, `changedAt`, `reason`, `userId`, `userName`, `titleName`, `departmentName`
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
    "items": [
      {
        "historyId": 1,
        "fromStatusCode": "PENDING",
        "toStatusCode": "IN_PROGRESS",
        "changedAt": "2026-04-19T09:00:00Z",
        "reason": "작업 시작",
        "userId": 101,
        "userName": "홍길동",
        "titleName": "팀장",
        "departmentName": "물류본부"
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
  - `tb_user`
- 근거
  - source: checklist
  - source: class mapping

### PATCH /api/worklogs/{id}/summary
- 목적: AI 요약을 반영한다.
- 상태: `Documented`
- 권한/접근 주체: 내부 AI callback 또는 승인된 내부 운영 주체가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `aiSummary`, `aiSummaryEdited` [추론]
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "aiSummary": "재고 동기화 처리 최적화 작업",
    "aiSummaryEdited": false
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
- ERD 연관
  - `tb_worklog.ai_summary`
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/worklogs/{id}/tags
- 목적: 태그 연결을 반영한다.
- 상태: `Documented`
- 권한/접근 주체: 내부 AI callback 또는 승인된 내부 운영 주체가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `tagIds[]`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "tagIds": [301, 302]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `TAG_NOT_FOUND` [추론]
- ERD 연관
  - `tb_worklog_tag`
  - `tb_meta_tag`
- 근거
  - source: checklist
  - source: class mapping

### PATCH /api/worklogs/{id}/ai-status
- 목적: AI 처리 상태를 반영한다.
- 상태: `Documented`
- 권한/접근 주체: 내부 AI callback 또는 승인된 내부 운영 주체가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `aiProcessingStatus`, `failureReason` [추론]
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 9001
  },
  "body": {
    "aiProcessingStatus": "COMPLETED",
    "failureReason": null
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
- ERD 연관
  - `tb_worklog.ai_processing_status`
- 근거
  - source: checklist
  - source: class mapping

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/worklogs/*` 를 canonical 로 사용한다.
- non-GET endpoint 는 common 의 closed-set 규칙에 따라 모두 empty 응답으로 정리했다.

