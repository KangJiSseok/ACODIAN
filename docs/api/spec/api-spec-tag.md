# Tag API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
관리형 태그 풀 조회, 중복 태그 병합, 미사용 태그 정리 계약을 정의한다. 태그 자체는 `tb_meta_tag`, 업무일지 연결은 `tb_worklog_tag` 가 맡는다.

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
| `GET` | `/api/tag/list` | `Documented` | 태그 풀 목록과 사용량을 조회한다. |
| `POST` | `/api/tag/merge` | `Documented` | 중복/유사 태그를 병합해 태그 품질을 유지한다. |
| `DELETE` | `/api/tag/{id}` | `Inferred-required` | 사용 중단 태그를 정리하는 운영 관리 API다. |

## 5. 엔드포인트 상세

### GET /api/tag/list
- 목적: 태그 풀 목록과 사용량을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증 사용자 조회를 기본으로 한다. 조직 범위 태그만 노출할지 여부는 구현 정책으로 남겨 둔다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `keyword`, `usageCountMin` [추론]
- 응답 (`data` 기준)
  - `PageResponse<TagSummary>` [추론]
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
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_meta_tag`
- 근거
  - class mapping ownership: `domain.tag.controller.TagController` / `domain.tag.service.TagService`
  - 관련 entity/context: domain.tag.entity.MetaTag, domain.tag.repository.TagRepository, domain.tag.repository.jooq.MetaTagJooqRepository
  - 메모: 태그 풀은 worklog에서 재사용하지만 ownership은 tag 도메인에 둔다.
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ERD `tb_meta_tag`

### POST /api/tag/merge
- 목적: 중복/유사 태그를 병합해 태그 품질을 유지한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상 또는 태그 품질 관리자만 호출한다. [추론]
- 요청
  - Body: `targetTagId`, `sourceTagIds[]` [추론]
- 응답 (`data` 기준)
  - 병합 후 대상 태그 정보와 이동된 연결 개수 [추론]
- 요청 JSON 예시
```json
{
  "body": {
    "targetTagId": 301,
    "sourceTagIds": [
      401,
      402
    ]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "targetTagId": 301,
    "mergedSourceTagIds": [
      401,
      402
    ],
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
  - class mapping ownership: `domain.tag.controller.TagController` / `domain.tag.service.TagService`
  - 관련 entity/context: domain.tag.entity.MetaTag, domain.worklog.entity.WorklogTag, domain.tag.repository.TagRepository, domain.worklog.repository.WorklogTagRepository
  - 메모: 태그 merge는 worklog-tag 연결 재배치를 동반한다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_meta_tag`
  - source: ERD `tb_worklog_tag`

### DELETE /api/tag/{id}
- 목적: 사용 중단 태그를 정리하는 운영 관리 API다.
- 상태: `Inferred-required`
- 권한/접근 주체: 태그 품질 관리자만 사용 중단 태그를 정리한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 삭제/비활성 처리 결과 [추론]
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
  "data": {
    "tagId": 301,
    "message": "태그가 삭제 처리되었습니다."
  },
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
  - class mapping ownership: `domain.tag.controller.TagController` / `domain.tag.service.TagService`
  - 관련 entity/context: domain.tag.entity.MetaTag, domain.worklog.entity.WorklogTag, domain.tag.repository.TagRepository, domain.worklog.repository.WorklogTagRepository
  - 메모: 삭제 가능 여부는 태그 사용량과 연결 상태를 함께 본다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_meta_tag`

## 6. 추론 메모
- 태그 병합은 source tag 다건을 target tag 하나로 수렴하는 형태로 정의했다. [추론]
