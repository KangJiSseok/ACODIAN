# API 명세 인덱스

## 1. 목적
- 이 문서는 checklist 기반 **52개 upstream inventory** 의 coverage 와 상태 분포를 검수하기 위한 인덱스다.
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

## 3. 상태 분포
| Status | Count |
|---|---:|
| `Documented` | 44 |
| `Proposed-risk-closure` | 3 |
| `Inferred-required` | 5 |

- 총 endpoint 수: **52**
- addendum 3건은 위 count 와 matrix row count 에 포함하지 않는다.

## 4. Coverage Matrix (inventory-linked 52 rows)
| Domain | Method | Path | Status | Source priority | Spec doc |
|---|---|---|---|---|---|
| `auth` | `POST` | `/api/auth/login` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `POST` | `/api/auth/logout` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `GET` | `/api/auth/me` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `POST` | `/api/auth/refresh` | `Proposed-risk-closure` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `auth` | `POST` | `/api/auth/change-password` | `Proposed-risk-closure` | checklist → class mapping → ERD/ADR | [api-spec-auth.md](./api-spec-auth.md) |
| `department` | `GET` | `/api/department/list` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `GET` | `/api/department/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `POST` | `/api/department` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `PUT` | `/api/department/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `department` | `DELETE` | `/api/department/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-department.md](./api-spec-department.md) |
| `team` | `GET` | `/api/team/list` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `GET` | `/api/team/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `POST` | `/api/team` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `PUT` | `/api/team/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `team` | `PATCH` | `/api/team/{id}/status` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-team.md](./api-spec-team.md) |
| `user` | `GET` | `/api/user/list` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `user` | `GET` | `/api/user/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `user` | `POST` | `/api/user` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `user` | `PUT` | `/api/user/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `user` | `DELETE` | `/api/user/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-user.md](./api-spec-user.md) |
| `skill` | `GET` | `/api/user/{id}/skills` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-skill.md](./api-spec-skill.md) |
| `skill` | `PUT` | `/api/user/{id}/skills` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-skill.md](./api-spec-skill.md) |
| `evaluation` | `GET` | `/api/user/{id}/evaluations` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-evaluation.md](./api-spec-evaluation.md) |
| `evaluation` | `POST` | `/api/user/{id}/evaluations` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-evaluation.md](./api-spec-evaluation.md) |
| `worklog` | `GET` | `/api/worklog/list` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `GET` | `/api/worklog/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `POST` | `/api/worklog` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PUT` | `/api/worklog/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `DELETE` | `/api/worklog/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PATCH` | `/api/worklog/{id}/status` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `GET` | `/api/worklog/{id}/history` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PATCH` | `/api/worklog/{id}/summary` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PUT` | `/api/worklog/{id}/tags` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `worklog` | `PATCH` | `/api/worklog/{id}/ai-status` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-worklog.md](./api-spec-worklog.md) |
| `file` | `POST` | `/api/file/upload` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `GET` | `/api/file/list` | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `GET` | `/api/file/{id}` | `Proposed-risk-closure` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `GET` | `/api/file/{id}/download` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `DELETE` | `/api/file/{id}` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `PUT` | `/api/file/{id}/summary` | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `file` | `PATCH` | `/api/file/{id}/ai-status` | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-file.md](./api-spec-file.md) |
| `tag` | `GET` | `/api/tag/list` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-tag.md](./api-spec-tag.md) |
| `tag` | `POST` | `/api/tag/merge` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-tag.md](./api-spec-tag.md) |
| `tag` | `DELETE` | `/api/tag/{id}` | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-tag.md](./api-spec-tag.md) |
| `notification` | `GET` | `/api/notification/list` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `notification` | `GET` | `/api/notification/unread-count` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `notification` | `PATCH` | `/api/notification/read-all` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `notification` | `PATCH` | `/api/notification/{id}/read` | `Inferred-required` | checklist → class mapping → ERD/ADR | [api-spec-notification.md](./api-spec-notification.md) |
| `dashboard` | `GET` | `/api/dashboards/summary` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboards/overdue` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboards/workload` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboards/my` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |

## 5. Spec-normalized path note
- 위 matrix 는 **upstream inventory 추적용 canonical row set** 이다.
- row identity 와 row count 는 checklist/class mapping 기준 52개를 유지한다.
- path 표기는 clarified scope 에서 현재 spec 표기를 명시적으로 고정한 경우 normalized path 를 사용할 수 있다.

## 6. Clarified-scope addendum
| Method | Path | Reason | Source | Spec doc |
|---|---|---|---|---|
| `POST` | `/api/users/signup` | user-request clarification | deep-interview spec | [api-spec-user.md](./api-spec-user.md) |
| `GET` | `/api/departments/{id}/users` | user-request clarification | deep-interview spec | [api-spec-department.md](./api-spec-department.md) |
| `POST` | `/api/teams/{id}/members/bulk` | user-request clarification | deep-interview spec | [api-spec-team.md](./api-spec-team.md) |

- addendum 3건은 matrix 52행, 상태 분포 count, checklist completeness count 에 포함하지 않는다.

## 7. 검수 체크리스트
- [ ] matrix row count = 52
- [ ] addendum row count = 3
- [ ] checklist/class mapping 원본 파일 미수정
- [ ] domain spec path 는 Auth 제외 모두 복수형 / no-`/list`
- [ ] dashboard 포함 전역 plural 규칙이 common/index/domain 에 일관되게 반영됨

