# Tag API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
관리형 태그 풀 조회, 중복 태그 병합, 미사용 태그 정리 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/tags/*` 를 사용한다.

## 2. 주요 ERD 연관
- `tb_meta_tag`
- `tb_worklog_tag`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/tags` | `Documented` | 태그 풀 목록과 사용량을 조회한다. |
| `POST` | `/api/tags/merge` | `Documented` | 중복/유사 태그를 병합한다. |
| `DELETE` | `/api/tags/{id}` | `Inferred-required` | 사용 중단 태그를 정리한다. |

## 5. 엔드포인트 상세

### GET /api/tags
- 목적: 태그 풀 목록과 사용량을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증 사용자 조회를 기본으로 한다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`, `keyword`, `usageCountMin`
- 응답 (`data` 기준)
  - `PageResponse<TagSummary>`
  - `items[*]`: `tagId`, `tagName`, `usageCount`, `createdAt`
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "keyword": "재고",
    "usageCountMin": 1
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
        "tagId": 301,
        "tagName": "재고",
        "usageCount": 15,
        "createdAt": "2026-04-01T09:00:00Z"
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
  - 대표 오류: `TAG_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_meta_tag`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007

### POST /api/tags/merge
- 목적: 중복/유사 태그를 병합한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상 또는 태그 품질 관리자만 호출한다. [추론]
- 요청
  - Body: `targetTagId`, `sourceTagIds[]`
- 응답 (`data` 기준)
  - `targetTagId`, `targetTagName`, `mergedSourceTagIds`, `movedWorklogCount`
- 요청 JSON 예시
```json
{
  "body": {
    "targetTagId": 301,
    "sourceTagIds": [401, 402]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "targetTagId": 301,
    "targetTagName": "재고",
    "mergedSourceTagIds": [401, 402],
    "movedWorklogCount": 12
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `TAG_NOT_FOUND` [추론]
  - 대표 오류: `TAG_MERGE_CONFLICT` [추론]
- ERD 연관
  - `tb_meta_tag`
  - `tb_worklog_tag`
- 근거
  - source: checklist
  - source: class mapping

### DELETE /api/tags/{id}
- 목적: 사용 중단 태그를 정리한다.
- 상태: `Inferred-required`
- 권한/접근 주체: 태그 품질 관리자만 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 301
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
  - 대표 오류: `TAG_NOT_FOUND` [추론]
  - 대표 오류: `TAG_IN_USE` [추론]
- ERD 연관
  - `tb_meta_tag`
  - `tb_worklog_tag`
- 근거
  - source: checklist
  - source: class mapping

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/tags/*` 를 canonical 로 사용한다.

