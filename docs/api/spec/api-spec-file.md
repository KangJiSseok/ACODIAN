# File API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
첨부파일 업로드, 목록/상세, 다운로드, 삭제, AI 요약·처리상태 반영 계약을 정의한다. 파일 메타데이터는 `tb_file`, 원본은 Object Storage 로 분리한다.

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
| `POST` | `/api/file/upload` | `Documented` | 파일 업로드와 메타데이터 저장을 담당한다. |
| `GET` | `/api/file/list` | `Inferred-required` | 파일 목록/필터/AI 요약 검색을 지원하는 파일 관리 조회 API다. |
| `GET` | `/api/file/{id}` | `Proposed-risk-closure` | 파일 상세 메타데이터와 AI 상태를 안정적으로 조회하는 운영 보강 API다. |
| `GET` | `/api/file/{id}/download` | `Documented` | 원본 파일 다운로드 또는 presigned URL 발급을 담당한다. |
| `DELETE` | `/api/file/{id}` | `Documented` | 파일 soft delete와 후처리 정리를 시작한다. |
| `PUT` | `/api/file/{id}/summary` | `Inferred-required` | AI가 생성한 파일 요약을 파일 메타데이터에 반영한다. |
| `PATCH` | `/api/file/{id}/ai-status` | `Inferred-required` | 파일 AI 처리 진행 상태를 반영한다. |

## 5. 엔드포인트 상세

### POST /api/file/upload
- 목적: 파일 업로드와 메타데이터 저장을 담당한다.
- 상태: `Documented`
- 권한/접근 주체: 업무일지 접근 권한이 있는 인증 사용자만 업로드할 수 있다. [추론]
- 요청
  - Multipart form-data: `file`, `worklogId`
  - 선택적으로 `description` 또는 업로드 메모 [추론]
- 응답 (`data` 기준)
  - `fileId`, `worklogId`, `originalName`, `fileExtension`, `fileSizeBytes`, `aiProcessingStatus`, `createdAt`
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
  - 대표 오류: `FILE_SIZE_LIMIT_EXCEEDED` [추론]
  - 대표 오류: `FILE_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_file`
  - `tb_worklog`
- 근거
  - class mapping ownership: `domain.file.controller.FileController` / `domain.file.service.FileService`
  - 관련 entity/context: domain.file.entity.File, domain.file.repository.FileRepository, domain.file.external.ObjectStoragePort
  - 메모: 저장소 추상화는 ObjectStoragePort / S3ObjectStorageAdapter로 분리한다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_file`
  - source: architecture guide

### GET /api/file/list
- 목적: 파일 목록/필터/AI 요약 검색을 지원하는 파일 관리 조회 API다.
- 상태: `Inferred-required`
- 권한/접근 주체: 업무일지/조직 범위에 대한 조회 권한이 있는 사용자만 호출한다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `worklogId`, `uploadedBy`, `aiProcessingStatus`, `keyword`, `fileExtension` [추론]
- 응답 (`data` 기준)
  - `PageResponse<FileSummary>`
  - `items[*]`: `fileId`, `worklogId`, `originalName`, `fileExtension`, `fileSizeBytes`, `uploadedBy`, `aiSummary` [추론], `aiProcessingStatus`, `createdAt`
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "worklogId": 9001,
    "uploadedBy": 101,
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
        "originalName": "inventory-plan.pdf",
        "fileExtension": "pdf",
        "fileSizeBytes": 245760,
        "uploadedBy": 101,
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
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_file`
- 근거
  - class mapping ownership: `domain.file.controller.FileController` / `domain.file.service.FileService`
  - 관련 entity/context: domain.file.entity.File, domain.file.repository.FileRepository, domain.file.repository.jooq.FileJooqRepository
  - 메모: api-design 기준 필수 추론 API이며 목록 화면 요구를 따른다.
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ERD `tb_file`

### GET /api/file/{id}
- 목적: 파일 상세 메타데이터와 AI 상태를 안정적으로 조회하는 운영 보강 API다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체: 파일이 속한 업무일지 접근 권한이 있는 사용자만 상세 조회한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `fileId`, `worklogId`, `originalName`, `fileExtension`, `fileSizeBytes`, `uploadedBy`, `aiSummary`, `aiProcessingStatus`, `createdAt`, `updatedAt`
  - `downloadUrl` 또는 다운로드 엔드포인트 링크 [추론]
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
    "originalName": "inventory-plan.pdf",
    "fileExtension": "pdf",
    "fileSizeBytes": 245760,
    "uploadedBy": 101,
    "aiSummary": "재고 계획 문서 요약",
    "aiProcessingStatus": "COMPLETED",
    "createdAt": "2026-04-21T03:00:00Z",
    "updatedAt": "2026-04-21T03:10:00Z",
    "downloadUrl": "/api/file/7001/download"
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
  - class mapping ownership: `domain.file.controller.FileController` / `domain.file.service.FileService`
  - 관련 entity/context: domain.file.entity.File, domain.file.repository.FileRepository, domain.file.repository.jooq.FileJooqRepository
  - 메모: 목록/다운로드와 별도로 상세 UX 완결성을 위해 제안된 API다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_file`

### GET /api/file/{id}/download
- 목적: 원본 파일 다운로드 또는 presigned URL 발급을 담당한다.
- 상태: `Documented`
- 권한/접근 주체: 파일 접근 권한이 있는 사용자만 원본 다운로드를 수행한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 파일 바이너리 스트림 또는 presigned URL 발급 결과 [추론]
  - 이 엔드포인트는 `Resource`/스트리밍 반환 시 공통 응답 봉투 제외 대상이 될 수 있다.
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
  - class mapping ownership: `domain.file.controller.FileController` / `domain.file.service.FileService`
  - 관련 entity/context: domain.file.entity.File, domain.file.repository.FileRepository, domain.file.external.ObjectStoragePort
  - 메모: 파일 접근 정책은 FileAccessPolicy와 함께 검토한다.
  - source: checklist
  - source: class mapping
  - source: ADR-015
  - source: ERD `tb_file`

### DELETE /api/file/{id}
- 목적: 파일 soft delete와 후처리 정리를 시작한다.
- 상태: `Documented`
- 권한/접근 주체: 업로드 사용자, 업무 소유자, 상위 관리자 범위에서 삭제를 수행한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 삭제/soft delete 결과 [추론]
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
    "isDeleted": true,
    "message": "파일이 삭제 처리되었습니다."
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `FILE_NOT_FOUND` [추론]
  - 대표 오류: `FILE_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_file.is_deleted`
- 근거
  - class mapping ownership: `domain.file.controller.FileController` / `domain.file.service.FileService`
  - 관련 entity/context: domain.file.entity.File, domain.file.repository.FileRepository
  - 메모: 실제 저장소 정리는 비동기 후속 처리로 분리할 수 있다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_file`

### PUT /api/file/{id}/summary
- 목적: AI가 생성한 파일 요약을 파일 메타데이터에 반영한다.
- 상태: `Inferred-required`
- 권한/접근 주체: 내부 AI 후처리 또는 관리자 보정 경로가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `aiSummary`
- 응답 (`data` 기준)
  - 반영된 `aiSummary`, `updatedAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 7001
  },
  "body": {
    "aiSummary": "재고 계획 문서의 핵심 일정과 리스크 요약"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "fileId": 7001,
    "aiSummary": "재고 계획 문서의 핵심 일정과 리스크 요약",
    "updatedAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `FILE_NOT_FOUND` [추론]
  - 대표 오류: `FILE_AI_UPDATE_FORBIDDEN` [추론]
- ERD 연관
  - `tb_file.ai_summary`
- 근거
  - class mapping ownership: `domain.file.controller.InternalFileAiCallbackController` / `domain.file.service.InternalFileAiCallbackService`
  - 관련 entity/context: domain.file.entity.File, domain.file.repository.FileRepository
  - 메모: 현행 코드 기준 internal callback controller base path는 /api/internal/file 이지만, endpoint/status 표기는 api-design 상세 본문 계약(/api/{domain}/{id}/{action})을 우선 따른다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_file`

### PATCH /api/file/{id}/ai-status
- 목적: 파일 AI 처리 진행 상태를 반영한다.
- 상태: `Inferred-required`
- 권한/접근 주체: 내부 AI 처리 상태 반영용 API로 본다. [추론]
- 요청
  - Path: `id`
  - Body: `aiProcessingStatus`, `message` [추론]
- 응답 (`data` 기준)
  - 변경된 `aiProcessingStatus`, `updatedAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 7001
  },
  "body": {
    "aiProcessingStatus": "COMPLETED",
    "message": "문서 요약 및 임베딩 완료"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "fileId": 7001,
    "aiProcessingStatus": "COMPLETED",
    "updatedAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `FILE_NOT_FOUND` [추론]
  - 대표 오류: `FILE_AI_STATUS_INVALID` [추론]
- ERD 연관
  - `tb_file.ai_processing_status`
- 근거
  - class mapping ownership: `domain.file.controller.InternalFileAiCallbackController` / `domain.file.service.InternalFileAiCallbackService`
  - 관련 entity/context: domain.file.entity.File, domain.file.repository.FileRepository
  - 메모: 현행 코드 기준 internal callback controller base path는 /api/internal/file 이지만, endpoint/status 표기는 api-design 상세 본문 계약(/api/{domain}/{id}/{action})을 우선 따른다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_file`

## 6. 추론 메모
- 다운로드는 `Resource` 직접 스트리밍 또는 presigned URL 발급 둘 다 허용하는 계약으로 열어 두었다. [추론]
- `GET /list`, `PUT /summary`, `PATCH /ai-status` 는 class mapping 기준 `Inferred-required` 상태를 유지한다.
