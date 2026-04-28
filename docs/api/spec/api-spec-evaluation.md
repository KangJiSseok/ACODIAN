# Evaluation API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자 평가 이력 조회와 평가 등록 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/users/{id}/evaluations` 를 사용한다.

## 2. 주요 ERD 연관
- `tb_user_evaluation`
- `tb_user`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/users/{id}/evaluations` | `Documented` | 특정 사용자의 평가 이력을 조회한다. |
| `POST` | `/api/users/{id}/evaluations` | `Documented` | 특정 사용자에 대한 평가를 등록한다. |

## 5. 엔드포인트 상세

### GET /api/users/{id}/evaluations
- 목적: 특정 사용자의 평가 이력을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: DIRECTOR, DEPT_HEAD 조회를 허용한다.
- DIRECTOR는 모든 부서 전체 다 볼 수 있다.
- DEPT_HEAD는 같은 부서의 사람들만 볼 수 있다.
- DEPT_HEAD는 같은 부서라도 `DIRECTOR` 권한 사용자의 평가 이력은 볼 수 없다.
- DEPT_HEAD는 자기 자신에 대한 평가 이력은 볼 수 없다.
- 요청
  - Path: `id`
  - Query: `page`, `pageSize`, `sortDirection`
- 응답 (`data` 기준)
  - `PageResponse<EvaluationSummary>`
  - `items[*]`: `evaluationId`, `evaluateeUserId`, `evaluateeUserName`, `evaluatorUserId`, `evaluatorUserName`, `content`, `createdAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  },
  "query": {
    "page": 1,
    "pageSize": 20
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
        "evaluationId": 501,
        "evaluateeUserId": 101,
        "evaluateeUserName": "홍길동",
        "evaluatorUserId": 301,
        "evaluatorUserName": "김본부장",
        "content": "프로젝트 리딩이 안정적입니다.",
        "createdAt": "2026-04-20T09:00:00Z"
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
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `EVALUATION_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_user_evaluation`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007

### POST /api/users/{id}/evaluations
- 목적: 특정 사용자에 대한 평가를 등록한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD`만 호출한다.
- `TEAM_LEAD`, `MEMBER`는 평가를 등록할 수 없다.
- 요청
  - Path: `id`
  - Body: `content`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  },
  "body": {
    "content": "프로세스 정리와 리스크 대응이 우수합니다."
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
  - 성공: `201 Created`
  - 대표 오류: `USER_NOT_FOUND`
  - 대표 오류: `EVALUATION_SELF_WRITE_FORBIDDEN`
  - 대표 오류: `EVALUATION_ACCESS_DENIED`
- ERD 연관
  - `tb_user_evaluation`
- 근거
  - source: checklist
  - source: class mapping

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/users/{id}/evaluations` 를 canonical 로 사용한다.
