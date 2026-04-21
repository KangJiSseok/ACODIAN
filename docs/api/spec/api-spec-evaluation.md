# Evaluation API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자 평가 이력 조회와 평가 등록 계약을 정의한다. 평가 엔터티는 피평가자/평가자를 모두 `tb_user` 에 연결한다.

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
| `GET` | `/api/user/{id}/evaluations` | `Documented` | 특정 사용자의 평가 이력을 조회한다. |
| `POST` | `/api/user/{id}/evaluations` | `Documented` | 특정 사용자에 대한 평가를 등록한다. |

## 5. 엔드포인트 상세

### GET /api/user/{id}/evaluations
- 목적: 특정 사용자의 평가 이력을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 본인, 평가 권한이 있는 리더, 상위 조직 관리자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
  - 선택 Query: `page`, `pageSize` [추론]
- 응답 (`data` 기준)
  - `PageResponse<EvaluationSummary>` [추론]
  - `items[*]`: `evaluationId`, `evaluateeUserId`, `evaluatorUserId`, `content`, `createdAt`
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
        "evaluatorUserId": 301,
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
  - class mapping ownership: `domain.organization.evaluation.controller.UserEvaluationController` / `domain.organization.evaluation.service.UserEvaluationService`
  - 관련 entity/context: domain.organization.evaluation.entity.UserEvaluation, domain.organization.evaluation.repository.UserEvaluationRepository, domain.organization.evaluation.repository.jooq.UserEvaluationJooqRepository
  - 메모: 평가 read/write ownership은 organization/evaluation feature가 가진다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_user_evaluation`

### POST /api/user/{id}/evaluations
- 목적: 특정 사용자에 대한 평가를 등록한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상 또는 정해진 평가 권한 사용자만 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `content`
  - 선택적으로 평가 문맥(`worklogId`, `period`) [추론]
- 응답 (`data` 기준)
  - 생성된 `evaluationId`, `createdAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  },
  "body": {
    "content": "프로세스 정리와 리스크 대응이 우수합니다.",
    "period": "2026-Q2"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "evaluationId": 502,
    "evaluateeUserId": 101,
    "evaluatorUserId": 301,
    "content": "프로세스 정리와 리스크 대응이 우수합니다.",
    "createdAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `EVALUATION_SELF_WRITE_FORBIDDEN` [추론]
  - 대표 오류: `EVALUATION_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_user_evaluation.content`
- 근거
  - class mapping ownership: `domain.organization.evaluation.controller.UserEvaluationController` / `domain.organization.evaluation.service.UserEvaluationService`
  - 관련 entity/context: domain.organization.evaluation.entity.UserEvaluation, domain.organization.evaluation.repository.UserEvaluationRepository
  - 메모: 평가 생성 규칙과 권한 검증은 서비스에서 처리한다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ERD `tb_user_evaluation`

## 6. 추론 메모
- 평가 조회 범위는 본인·직속 리더·상위 조직 관리자 중심으로 기술했다. [추론]
