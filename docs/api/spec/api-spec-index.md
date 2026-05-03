# API 명세 인덱스

## 1. 목적
- 이 문서는 checklist 기반 **53개 current inventory** 의 coverage 와 상태 분포를 검수하기 위한 인덱스다.
- 실제 도메인 문서 본문은 normalized path 를 사용할 수 있으며, 그 차이는 의도된 구조다.
- 공통 규칙은 [api-spec-common.md](./api-spec-common.md) 를 따른다.

## 2. 문서 링크
| 구분 | 문서 |
|---|---|
| 공통 가이드 | [api-spec-common.md](./api-spec-common.md) |
| Auth | [api-spec-auth.md](./api-spec-auth.md) |
| Department | [api-spec-department.md](./api-spec-department.md) |
| Team | [api-spec-team.md](./api-spec-team.md) |
| User | [api-spec-user.md](./api-spec-user.md) |
| Skill | [api-spec-skill.md](./api-spec-skill.md) |
| Evaluation | [api-spec-evaluation.md](./api-spec-evaluation.md) |
| Worklog | [api-spec-worklog.md](./api-spec-worklog.md) |
| File | [api-spec-file.md](./api-spec-file.md) |
| Tag | [api-spec-tag.md](./api-spec-tag.md) |
| Notification | [api-spec-notification.md](./api-spec-notification.md) |
| Dashboard | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| Enum Reference | [api-spec-enums.md](./api-spec-enums.md) |

## 3. 상태 분포
| Status | Count |
|---|---:|
| `Documented` | 45 |
| `Proposed-risk-closure` | 3 |
| `Inferred-required` | 5 |

- 총 endpoint 수: **53**
- addendum 2건은 위 count 와 matrix row count 에 포함하지 않는다.
- team canonical row count: **7**

## 4. Coverage Matrix (inventory-linked 53 rows)
| Domain | Method | Path                              | Status | Source priority | Spec doc |
|---|---|-----------------------------------|---|---|---|
| `auth` | `POST` | `/api/auth/login`                 | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `POST` | `/api/auth/signup`                | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `POST` | `/api/auth/logout`                | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `POST` | `/api/auth/refresh`               | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `POST` | `/api/auth/change-password`       | `Proposed-risk-closure` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `department` | `GET` | `/api/departments`                | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `GET` | `/api/departments/{id}`           | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `POST` | `/api/departments`                | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `PUT` | `/api/departments/{id}`           | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `DELETE` | `/api/departments/{id}`           | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `team` | `GET` | `/api/teams`                      | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `GET` | `/api/teams/summary`              | `Proposed-risk-closure` | user clarification → checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `GET` | `/api/teams/{id}`                 | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `GET` | `/api/teams/{id}/users`           | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `POST` | `/api/teams`                      | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `PATCH` | `/api/teams/{id}`                 | `Documented` | user clarification → checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `DELETE` | `/api/teams/{id}`                 | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `user` | `GET` | `/api/users/me`                   | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `user` | `GET` | `/api/users`                      | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `user` | `GET` | `/api/users/{id}`                 | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `user` | `PATCH` | `/api/users/{id}`                | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `skill` | `GET` | `/api/users/{id}/skills`          | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-skill.md](./api-spec-skill.md) |
| `skill` | `PUT` | `/api/users/{id}/skills`          | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-skill.md](./api-spec-skill.md) |
| `evaluation` | `GET` | `/api/users/{id}/evaluations`     | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-evaluation.md](./api-spec-evaluation.md) |
| `evaluation` | `POST` | `/api/users/{id}/evaluations`     | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-evaluation.md](./api-spec-evaluation.md) |
| `worklog` | `GET` | `/api/worklogs`                   | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `GET` | `/api/worklogs/{id}`              | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `POST` | `/api/worklogs`                   | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PUT` | `/api/worklogs/{id}`              | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `DELETE` | `/api/worklogs/{id}`              | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PATCH` | `/api/worklogs/{id}/status`       | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `GET` | `/api/worklogs/{id}/history`      | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PATCH` | `/api/worklogs/{id}/summary`      | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PUT` | `/api/worklogs/{id}/tags`         | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PATCH` | `/api/worklogs/{id}/ai-status`    | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `file` | `POST` | `/api/files/upload`               | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `GET` | `/api/files`                      | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `GET` | `/api/files/{id}`                 | `Proposed-risk-closure` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `GET` | `/api/files/{id}/download`        | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `DELETE` | `/api/files/{id}`                 | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `PUT` | `/api/files/{id}/summary`         | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `PATCH` | `/api/files/{id}/ai-status`       | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `tag` | `GET` | `/api/tags`                       | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-tag.md](./api-spec-tag.md) |
| `tag` | `POST` | `/api/tags/merge`                 | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-tag.md](./api-spec-tag.md) |
| `tag` | `DELETE` | `/api/tags/{id}`                  | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-tag.md](./api-spec-tag.md) |
| `notification` | `GET` | `/api/notifications`              | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `notification` | `GET` | `/api/notifications/unread-count` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `notification` | `PATCH` | `/api/notifications/read-all`     | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `notification` | `PATCH` | `/api/notifications/{id}/read`    | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `dashboard` | `GET` | `/api/dashboards/summary`         | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboards/overdue`         | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboards/workload`        | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboards/my`              | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |

## 5. Spec-normalized path note
- 위 matrix 는 **canonical row set** 이며 checklist 및 class mapping 과 동일한 normalized path (복수형, `/list` 미사용) 표기를 사용한다.
- row identity 와 row count 는 현재 matrix 의 53개를 유지한다.
- `/api/auth/me` 는 `/api/users/me` 로 재분류되어 `user` 도메인이 소유한다.

## 6. Clarified-scope addendum
| Method | Path | Reason | Source | Spec doc |
|---|---|---|---|---|
| `GET` | `/api/departments/{id}/users` | user-request clarification | deep-interview spec | [api-spec-department.md](./api-spec-department.md) |
| `GET` | `/api/users/manager-candidates` | user-request clarification | deep-interview spec | [api-spec-user.md](./api-spec-user.md) |

- addendum 2건은 matrix 53행, 상태 분포 count, checklist completeness count 에 포함하지 않는다.

## 7. 검수 체크리스트
- [x] matrix row count = 53
- [x] addendum row count = 2
- [x] team canonical row count = 7
- [x] matrix / checklist / class mapping path 표기가 normalized (복수형, `/list` 미사용) 으로 정렬됨
- [x] domain spec path 는 Auth 제외 모두 복수형 / no-`/list`
- [x] `/api/auth/me` → `/api/users/me` 이동이 matrix, auth spec, user spec, common spec 에 반영됨
- [x] `POST /api/users/signup` → `POST /api/auth/signup` 이동이 matrix, auth spec, user spec 에 반영됨
- [x] dashboard 포함 전역 plural 규칙이 common/index/domain 에 일관되게 반영됨
