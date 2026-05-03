# api-springboot-endpoint-checklist.md

- 기준 문서: [`api-springboot-endpoint-class-mapping.md`](api-springboot-endpoint-class-mapping.md)
- 표기 기준: [`spec/api-spec-index.md`](spec/api-spec-index.md) 의 normalized path (복수형, `/list` 미사용)

---

## 1. `auth` 도메인

- [x] `POST /api/auth/login`
- [x] `POST /api/auth/logout`
- [x] `POST /api/auth/refresh`
- [ ] `POST /api/auth/change-password`

## 2. `organization/department` 도메인

- [x] `GET /api/departments`
- [x] `GET /api/departments/{id}`
- [x] `GET /api/departments/{id}/users`
- [x] `POST /api/departments`
- [x] `PUT /api/departments/{id}`
- [x] `DELETE /api/departments/{id}`

## 3. `organization/team` 도메인

- [x] `GET /api/teams`
- [x] `GET /api/teams/summary`
- [x] `GET /api/teams/{id}`
- [x] `GET /api/teams/{id}/users`
- [ ] `POST /api/teams`
- [ ] `PATCH /api/teams/{id}`
- [ ] `DELETE /api/teams/{id}`

## 4. `organization/user` 도메인

- [x] `GET /api/users/me`
- [ ] `GET /api/users`
- [ ] `GET /api/users/{id}`
- [ ] `POST /api/users/signup`
- [ ] `POST /api/users`
- [ ] `PUT /api/users/{id}`
- [ ] `DELETE /api/users/{id}`

## 5. `organization/skill` 도메인

- [ ] `GET /api/users/{id}/skills`
- [ ] `PUT /api/users/{id}/skills`

## 6. `organization/evaluation` 도메인

- [x] `GET /api/users/{id}/evaluations`
- [ ] `POST /api/users/{id}/evaluations`

## 7. `worklog` 도메인

- [ ] `GET /api/worklogs`
- [ ] `GET /api/worklogs/{id}`
- [ ] `POST /api/worklogs`
- [ ] `PUT /api/worklogs/{id}`
- [ ] `DELETE /api/worklogs/{id}`
- [ ] `PATCH /api/worklogs/{id}/status`
- [ ] `GET /api/worklogs/{id}/history`
- [ ] `PATCH /api/worklogs/{id}/summary`
- [ ] `PUT /api/worklogs/{id}/tags`
- [ ] `PATCH /api/worklogs/{id}/ai-status`

## 8. `file` 도메인

- [ ] `POST /api/files/upload`
- [ ] `GET /api/files`
- [ ] `GET /api/files/{id}`
- [ ] `GET /api/files/{id}/download`
- [ ] `DELETE /api/files/{id}`
- [ ] `PUT /api/files/{id}/summary`
- [ ] `PATCH /api/files/{id}/ai-status`

## 9. `tag` 도메인

- [ ] `GET /api/tags`
- [ ] `POST /api/tags/merge`
- [ ] `DELETE /api/tags/{id}`

## 10. `notification` 도메인

- [ ] `GET /api/notifications`
- [ ] `GET /api/notifications/unread-count`
- [ ] `PATCH /api/notifications/read-all`
- [ ] `PATCH /api/notifications/{id}/read`

## 11. `dashboard` 도메인

- [ ] `GET /api/dashboards/summary`
- [ ] `GET /api/dashboards/overdue`
- [ ] `GET /api/dashboards/workload`
- [ ] `GET /api/dashboards/my`
