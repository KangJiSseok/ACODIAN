# api-springboot-endpoint-checklist.md

- 기준 문서: [`api-springboot-endpoint-class-mapping.md`](api-springboot-endpoint-class-mapping.md)

---

## 1. `auth` 도메인

- [ ] `POST /api/auth/login`
- [ ] `POST /api/auth/logout`
- [ ] `GET /api/auth/me`
- [ ] `POST /api/auth/refresh`
- [ ] `POST /api/auth/change-password`

## 2. `organization/department` 도메인

- [ ] `GET /api/department/list`
- [ ] `GET /api/department/{id}`
- [ ] `POST /api/department`
- [ ] `PUT /api/department/{id}`
- [ ] `DELETE /api/department/{id}`

## 3. `organization/team` 도메인

- [ ] `GET /api/team/list`
- [ ] `GET /api/team/{id}`
- [ ] `POST /api/team`
- [ ] `PUT /api/team/{id}`
- [ ] `PATCH /api/team/{id}/status`

## 4. `organization/user` 도메인

- [ ] `GET /api/user/list`
- [ ] `GET /api/user/{id}`
- [ ] `POST /api/user`
- [ ] `PUT /api/user/{id}`
- [ ] `DELETE /api/user/{id}`

## 5. `organization/skill` 도메인

- [ ] `GET /api/user/{id}/skills`
- [ ] `PUT /api/user/{id}/skills`

## 6. `organization/evaluation` 도메인

- [ ] `GET /api/user/{id}/evaluations`
- [ ] `POST /api/user/{id}/evaluations`

## 7. `worklog` 도메인

- [ ] `GET /api/worklog/list`
- [ ] `GET /api/worklog/{id}`
- [ ] `POST /api/worklog`
- [ ] `PUT /api/worklog/{id}`
- [ ] `DELETE /api/worklog/{id}`
- [ ] `PATCH /api/worklog/{id}/status`
- [ ] `GET /api/worklog/{id}/history`
- [ ] `PATCH /api/worklog/{id}/summary`
- [ ] `PUT /api/worklog/{id}/tags`
- [ ] `PATCH /api/worklog/{id}/ai-status`

## 8. `file` 도메인

- [ ] `POST /api/file/upload`
- [ ] `GET /api/file/list`
- [ ] `GET /api/file/{id}`
- [ ] `GET /api/file/{id}/download`
- [ ] `DELETE /api/file/{id}`
- [ ] `PUT /api/file/{id}/summary`
- [ ] `PATCH /api/file/{id}/ai-status`

## 9. `tag` 도메인

- [ ] `GET /api/tag/list`
- [ ] `POST /api/tag/merge`
- [ ] `DELETE /api/tag/{id}`

## 10. `notification` 도메인

- [ ] `GET /api/notification/list`
- [ ] `GET /api/notification/unread-count`
- [ ] `PATCH /api/notification/read-all`
- [ ] `PATCH /api/notification/{id}/read`

## 11. `dashboard` 도메인

- [ ] `GET /api/dashboard/summary`
- [ ] `GET /api/dashboard/overdue`
- [ ] `GET /api/dashboard/workload`
- [ ] `GET /api/dashboard/my`
