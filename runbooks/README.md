# Runbooks — 장애 대응 절차

운영 장애 발생 시 대응 절차 모음. 모듈별 디렉토리로 분리한다.
"에러가 발생하면 어떻게 복구하는가"를 단계별로 기록한다.

## 모듈별 인덱스

| 모듈 | 인덱스 | 상태 |
|---|---|---|
| api | [`api/README.md`](api/README.md) | 인덱스 존재, 개별 runbook 미작성 |
| web | 추후 `runbooks/web/README.md` | 예정 |
| ai | 추후 `runbooks/ai/README.md` | 예정 |
| nginx | 추후 `runbooks/nginx/README.md` | 예정 |

## 새 Runbook 작성 규약

1. **장애가 실제 발생한 후** 파일을 추가한다 — 선제적 추측으로 작성하지 않는다.
2. 파일 위치: `runbooks/{module}/{slug}.md`. slug는 kebab-case로 증상 요약.
   - 예: `runbooks/api/deploy-failure.md`, `runbooks/api/db-connection-lost.md`
3. 새 파일 추가 시 해당 모듈 `runbooks/{module}/README.md`의 인덱스 표에 행을 추가한다.
4. 새 모듈(web/ai/nginx) 첫 runbook이 생기는 순간 해당 디렉토리와 `README.md`를 신설한다.

## 공통 템플릿

각 runbook은 아래 5섹션으로 작성한다.

- **증상** (Symptoms): 사용자/모니터링에 어떻게 보이는가
- **1차 점검** (Triage): 5분 내 확인할 3가지
- **복구 절차** (Recovery): 단계별 명령
- **근본 원인 분류** (Root Cause Categories)
- **후속 조치** (Follow-up): 재발 방지 액션

## 관련 참조

- 예방 정책 문서: [`docs/api/jooq-codegen-policy.md`](../docs/api/jooq-codegen-policy.md)
- 문서 구조 전략: [`docs/docs-strategy.md`](../docs/docs-strategy.md)
