# api Runbook — 운영 장애 대응 절차

Spring Boot api 모듈의 장애 유형별 대응 절차 인덱스.
장애가 실제 발생할 때마다 파일을 추가한다. 예방적 정책은 `../../docs/api/jooq-codegen-policy.md` 같은 root-level 정책 문서를 참조.

## 대응 절차 인덱스

| 유형 | 파일 | 상태 |
|---|---|---|
| (스텁) 배포 실패 | deploy-failure.md | 미작성 |
| (스텁) DB 연결 불가 | db-connection-lost.md | 미작성 |
| (스텁) Flyway migration 실패 | flyway-migration-failure.md | 미작성 |

## 새 Runbook 작성 템플릿

각 runbook은 아래 구조로 작성:

- **증상** (Symptoms): 사용자/모니터링에 어떻게 보이는가
- **1차 점검** (Triage): 5분 내 확인할 3가지
- **복구 절차** (Recovery): 단계별 명령
- **근본 원인 분류** (Root Cause Categories)
- **후속 조치** (Follow-up): 재발 방지 액션

## 관련 정책 문서

- [`../../docs/api/jooq-codegen-policy.md`](../../docs/api/jooq-codegen-policy.md) — jOOQ 코드 생성 운영 모델
