# Skill API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자별 보유 스킬 목록 조회와 일괄 교체 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/users/{id}/skills` 를 사용한다.

## 2. 주요 ERD 연관
- `tb_user_skill`
- `tb_user`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/users/{id}/skills` | `Documented` | 특정 사용자의 보유 스킬 목록을 조회한다. |
| `PUT` | `/api/users/{id}/skills` | `Documented` | 특정 사용자의 스킬 세트를 교체한다. |

## 5. 엔드포인트 상세

### GET /api/users/{id}/skills
- 목적: 특정 사용자의 보유 스킬 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 본인 조회 또는 조직 관리자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `userId`, `userName`
  - `skills[*]`: `skillName`, `skillLevel`, `updatedAt`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "userName": "홍길동",
    "skills": [
      {
        "skillName": "WMS",
        "skillLevel": 5,
        "updatedAt": "2026-04-20T09:00:00Z"
      },
      {
        "skillName": "SQL",
        "skillLevel": 4,
        "updatedAt": "2026-04-18T09:00:00Z"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `USER_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_user_skill`
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/users/{id}/skills
- 목적: 특정 사용자의 스킬 세트를 교체한다.
- 상태: `Documented`
- 권한/접근 주체: 본인 수정 또는 조직 관리자 보정 작업으로 본다. [추론]
- 요청
  - Path: `id`
  - Body: `skills[]` (`skillName`, `skillLevel`)
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  },
  "body": {
    "skills": [
      {
        "skillName": "WMS",
        "skillLevel": 5
      },
      {
        "skillName": "Python",
        "skillLevel": 3
      }
    ]
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
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `SKILL_LEVEL_OUT_OF_RANGE` [추론]
- ERD 연관
  - `tb_user_skill`
- 근거
  - source: checklist
  - source: class mapping

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/users/{id}/skills` 를 canonical 로 사용한다.

