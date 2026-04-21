# API 명세 인덱스

## 1. 목적
- checklist 52개 endpoint 의 coverage, 상태 분포, 문서 링크를 한곳에서 검수하기 위한 인덱스다.
- 공통 규칙은 [api-spec-common.md](./api-spec-common.md) 를 기준으로 한다.
- 문서 경로는 `docs/api/spec/` 디렉토리다.

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
- 기대 분포: `Documented 44 / Proposed-risk-closure 3 / Inferred-required 5`

## 4. Coverage Matrix (커버리지 매트릭스)
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
| `dashboard` | `GET` | `/api/dashboard/summary` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboard/overdue` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboard/workload` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |
| `dashboard` | `GET` | `/api/dashboard/my` | `Documented` | checklist → class mapping → ERD/ADR | [api-spec-dashboard.md](./api-spec-dashboard.md) |

## 5. 검수 체크리스트
- [ ] 공통 문서 2개 + 도메인 문서 11개가 모두 존재한다.
- [ ] Coverage Matrix 의 52개 행이 checklist 와 정확히 일치한다.
- [ ] 상태 분포가 `44 / 3 / 5` 와 일치한다.
- [ ] 각 도메인 문서에 도메인 개요 / 엔드포인트 목록 / 엔드포인트 상세 / 추론 메모가 있다.
- [ ] 모든 목록 API가 PageResponse 표준을 설명한다.
- [ ] `Proposed-risk-closure`, `Inferred-required` endpoint 가 누락 없이 반영되었다.
- [ ] cross-link 가 모두 살아 있다.

## 6. 소스 문서 묶음
- checklist: [api-springboot-endpoint-checklist.md](../api-springboot-endpoint-checklist.md)
- class mapping: [api-springboot-endpoint-class-mapping.md](../api-springboot-endpoint-class-mapping.md)
- ADR: [adr.yaml](../adr.yaml)
- code convention: [code-convention.yaml](../code-convention.yaml)
- architecture guide: [../../AX-WMS_기획서_아키텍처가이드.md](../../AX-WMS_기획서_아키텍처가이드.md)
- ERD: [../../ax-wms-erd-draft.sql](../../ax-wms-erd-draft.sql)
