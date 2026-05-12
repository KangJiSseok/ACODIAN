# File API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
첨부파일 업로드, 목록/상세, 다운로드, 삭제, AI 요약·처리상태 반영 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/files/*` 를 사용한다.

## 2. 주요 ERD 연관
- `tb_file`
- `tb_worklog`
- `tb_file_embedding`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)
- [../../AX-WMS_기획서_아키텍처가이드.md](../../AX-WMS_기획서_아키텍처가이드.md)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `POST` | `/api/files/upload` | `Documented` | 파일 업로드와 메타데이터 저장을 담당한다. |
| `GET` | `/api/files` | `Inferred-required` | 파일 목록/필터/AI 요약 검색을 지원한다. |
| `GET` | `/api/files/{id}` | `Proposed-risk-closure` | 파일 상세 메타데이터와 AI 상태를 조회한다. |
| `GET` | `/api/files/{id}/download` | `Documented` | 원본 파일 다운로드 또는 presigned URL 발급을 담당한다. |
| `DELETE` | `/api/files/{id}` | `Documented` | 파일 soft delete 와 후처리 정리를 시작한다. |
| `PUT` | `/api/files/{id}/summary` | `Inferred-required` | AI가 생성한 파일 요약을 반영한다. |
| `PATCH` | `/api/files/{id}/ai-status` | `Inferred-required` | 파일 AI 처리 진행 상태를 반영한다. |

## 5. 엔드포인트 상세

### POST /api/files/upload
- 목적: 파일 업로드와 메타데이터 저장을 담당한다.
- 상태: `Documented`
- 권한/접근 주체: 업무일지 접근 권한이 있는 인증 사용자만 업로드할 수 있다. [추론]
- 요청
  - Multipart form-data: `file`, `worklogId`
  - 선택 Body: `description` [추론]
- 응답 (`data` 기준)
  - `fileId`, `worklogId`, `worklogTitle`, `originalName`, `fileExtension`, `fileSizeBytes`, `aiProcessingStatus`, `createdAt`
- 요청 JSON 예시
```json
{
  "note": "실제 요청은 multipart/form-data 이며, 아래는 메타데이터 JSON 예시다.",
  "body": {
    "worklogId": 9001,
    "originalName": "inventory-plan.pdf",
    "fileExtension": "pdf",
    "fileSizeBytes": 245760
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "fileId": 7001,
    "worklogId": 9001,
    "worklogTitle": "재고 동기화 개선",
    "originalName": "inventory-plan.pdf",
    "fileExtension": "pdf",
    "fileSizeBytes": 245760,
    "aiProcessingStatus": "PENDING",
    "createdAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `WORKLOG_NOT_FOUND` [추론]
  - 대표 오류: `FILE_EXTENSION_NOT_ALLOWED` [추론]
- ERD 연관
  - `tb_file`
  - `tb_worklog`
- 근거
  - source: checklist
  - source: class mapping

### GET /api/files
- 목적: 파일 목록/필터/AI 요약 검색을 지원한다.
- 상태: `Inferred-required`
- 권한/접근 주체: 업무일지/조직 범위 조회 권한이 있는 사용자만 호출한다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `worklogId`, `uploadedByUserId`, `aiProcessingStatus`, `keyword`, `fileExtension`
- 응답 (`data` 기준)
  - `PageResponse<FileSummary>`
  - `items[*]`: `fileId`, `worklogId`, `worklogTitle`, `originalName`, `fileExtension`, `fileSizeBytes`, `uploadedByUserId`, `uploadedByUserName`, `aiSummary`, `aiProcessingStatus`, `createdAt`
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "worklogId": 9001,
    "uploadedByUserId": 101,
    "aiProcessingStatus": "COMPLETED",
    "keyword": "inventory",
    "fileExtension": "pdf"
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
        "fileId": 7001,
        "worklogId": 9001,
        "worklogTitle": "재고 동기화 개선",
        "originalName": "inventory-plan.pdf",
        "fileExtension": "pdf",
        "fileSizeBytes": 245760,
        "uploadedByUserId": 101,
        "uploadedByUserName": "홍길동",
        "aiSummary": "재고 계획 문서 요약",
        "aiProcessingStatus": "COMPLETED",
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
  - 성공: `200 OK` [추론]
  - 대표 오류: `FILE_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_file`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007

### GET /api/files/{id}
- 목적: 파일 상세 메타데이터와 AI 상태를 조회한다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체: 파일이 속한 업무일지 접근 권한이 있는 사용자만 조회한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `fileId`, `worklogId`, `worklogTitle`, `originalName`, `fileExtension`, `fileSizeBytes`, `uploadedByUserId`, `uploadedByUserName`, `aiSummary`, `aiProcessingStatus`, `createdAt`, `updatedAt`
  - `downloadUrl` [추론]
- 요청 JSON 예시
```json
{
  "path": {
    "id": 7001
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "fileId": 7001,
    "worklogId": 9001,
    "worklogTitle": "재고 동기화 개선",
    "originalName": "inventory-plan.pdf",
    "fileExtension": "pdf",
    "fileSizeBytes": 245760,
    "uploadedByUserId": 101,
    "uploadedByUserName": "홍길동",
    "aiSummary": "재고 계획 문서 요약",
    "aiProcessingStatus": "COMPLETED",
    "createdAt": "2026-04-21T03:00:00Z",
    "updatedAt": "2026-04-21T03:10:00Z",
    "downloadUrl": "/api/files/7001/download"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `FILE_NOT_FOUND` [추론]
  - 대표 오류: `FILE_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_file`
- 근거
  - source: checklist
  - source: class mapping

### GET /api/files/{id}/download
- 목적: 원본 파일 다운로드 또는 presigned URL 발급을 담당한다.
- 상태: `Documented`
- 권한/접근 주체: 파일 접근 권한이 있는 사용자만 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 파일 바이너리 스트림 또는 presigned URL 발급 결과 [추론]
  - streaming 응답일 경우 공통 envelope 예외가 될 수 있다.
- 요청 JSON 예시
```json
{
  "path": {
    "id": 7001
  }
}
```
- 응답 JSON 예시
```json
{
  "download": {
    "fileName": "inventory-plan.pdf",
    "contentType": "application/pdf",
    "stream": "<binary-stream-or-presigned-url>"
  }
}
```
- 상태/에러
  - 성공: `200 OK` 또는 `302 Found` [추론]
  - 대표 오류: `FILE_NOT_FOUND` [추론]
  - 대표 오류: `FILE_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_file.stored_path`
- 근거
  - source: checklist
  - source: class mapping

### DELETE /api/files/{id}
- 목적: 파일 soft delete 와 후처리 정리를 시작한다.
- 상태: `Documented`
- 권한/접근 주체: 업로드 사용자, 업무 소유자, 상위 관리자 범위에서 삭제를 수행한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 7001
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
  - 대표 오류: `FILE_NOT_FOUND` [추론]
  - 대표 오류: `FILE_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_file`
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/files/{id}/summary
- 목적: AI가 생성한 파일 요약을 반영한다.
- 상태: `Inferred-required`
- 권한/접근 주체: 내부 AI callback 또는 승인된 내부 운영 주체가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `aiSummary`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 7001
  },
  "body": {
    "aiSummary": "재고 계획 문서 요약"
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
  - 대표 오류: `FILE_NOT_FOUND` [추론]
- ERD 연관
  - `tb_file.ai_summary`
- 근거
  - source: checklist
  - source: class mapping

### PATCH /api/files/{id}/ai-status
- 목적: 파일 AI 처리 진행 상태를 반영한다.
- 상태: `Inferred-required`
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
    "id": 7001
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
  - 대표 오류: `FILE_NOT_FOUND` [추론]
- ERD 연관
  - `tb_file.ai_processing_status`
- 근거
  - source: checklist
  - source: class mapping

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/files/*` 를 canonical 로 사용한다.

