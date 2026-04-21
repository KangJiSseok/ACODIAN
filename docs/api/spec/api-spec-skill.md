# Skill API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자별 보유 스킬 목록 조회와 일괄 교체 계약을 정의한다. 스킬은 독립 마스터 테이블이 아닌 `tb_user_skill` 기준의 사용자 부속 데이터로 본다.

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
| `GET` | `/api/user/{id}/skills` | `Documented` | 특정 사용자의 보유 스킬 목록을 조회한다. |
| `PUT` | `/api/user/{id}/skills` | `Documented` | 특정 사용자의 스킬 세트를 교체/정리한다. |

## 5. 엔드포인트 상세

### GET /api/user/{id}/skills
- 목적: 특정 사용자의 보유 스킬 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 본인 조회 또는 조직 관리자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `userId`
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
  - class mapping ownership: `domain.organization.skill.controller.UserSkillController` / `domain.organization.skill.service.UserSkillService`
  - 관련 entity/context: domain.organization.skill.entity.UserSkill, domain.organization.skill.repository.UserSkillRepository, domain.organization.skill.repository.jooq.UserSkillJooqRepository
  - 메모: 현행 코드 skeleton의 클래스는 존재하며 endpoint/status는 api-design 상세 본문을 우선 따른다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_user_skill`

### PUT /api/user/{id}/skills
- 목적: 특정 사용자의 스킬 세트를 교체/정리한다.
- 상태: `Documented`
- 권한/접근 주체: 본인 수정 또는 조직 관리자 보정 작업으로 본다. [추론]
- 요청
  - Path: `id`
  - Body: `skills[]` (`skillName`, `skillLevel`)
- 응답 (`data` 기준)
  - 교체 후 스킬 세트와 변경 시각 [추론]
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
  "data": {
    "userId": 101,
    "skills": [
      {
        "skillName": "WMS",
        "skillLevel": 5
      },
      {
        "skillName": "Python",
        "skillLevel": 3
      }
    ],
    "updatedAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `SKILL_LEVEL_OUT_OF_RANGE` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_user_skill.skill_name`
  - `tb_user_skill.skill_level`
- 근거
  - class mapping ownership: `domain.organization.skill.controller.UserSkillController` / `domain.organization.skill.service.UserSkillService`
  - 관련 entity/context: domain.organization.skill.entity.UserSkill, domain.organization.skill.repository.UserSkillRepository
  - 메모: 사용자별 스킬 ownership은 organization/skill feature에 둔다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_user_skill`

## 6. 추론 메모
- 스킬 수정은 전체 교체(idempotent) 방식으로 정의했다. [추론]
