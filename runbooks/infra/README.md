# Runbooks — infra

인프라 운영 장애 대응 절차 인덱스. 작성 규약은 최상위 [`../README.md`](../README.md) 를 따른다.

## 인덱스

| Runbook | 증상 | 관련 기준 |
|---|---|---|
| [`ci-auth-troubleshoot.md`](ci-auth-troubleshoot.md) | CI 의 인증 자격증명(`GHCR_USER`/`GHCR_TOKEN`, `SSH_PRIVATE_KEY`) 이 runtime 에 주입되지 않거나 형식이 깨져 `docker login` / `ssh` 가 실패 | ADR-007, ADR-010, OPS-006, OPS-009, OPS-010, OPS-012 |
