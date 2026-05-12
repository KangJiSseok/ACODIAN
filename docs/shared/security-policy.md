# 보안 정책 (Security Policy)

## 이 문서는 무엇인가
AX-WMS의 인증/인가, 환경변수 관리, 위협 모델 운영 기준을 정의한다.
ADR-002(인증 방식 결정)와 ADR-005(환경변수 관리 방식)를 구체적 운영 정책으로 확장한 문서다.
신규 API 개발 또는 배포 환경 변경 시 이 문서를 기준으로 보안 검토를 수행한다.

## 언제 읽어야 하는가 (Read trigger)
- 외부에 노출되는 REST API 엔드포인트를 추가할 때
- 인증 플로우(JWT, 세션, OAuth 등)를 수정하거나 새로 도입할 때
- 환경변수(DB 자격증명, 외부 API 키 등)를 새로 추가하거나 로테이션할 때

## 언제 추가/확장해야 하는가 (Write trigger)
- 첫 외부 배포 전에 위협 모델 섹션 초안 작성
- 보안 침해 사고 발생 후 대응 기준 강화
- 컴플라이언스 요구(개인정보보호법, ISO 27001 등)가 발생할 때

## 관련 자동화 아이디어 (Skill / CI / Hook)
> 현재 구현되지 않음. 도입 시점에 아래 방향 검토.

- **스킬 후보**: `$security-audit` — OWASP 체크리스트 기반 자동 점검 리포트 생성
- **CI 연동**: OWASP ZAP API 스캔을 스테이징 배포 후 CI 파이프라인에서 실행
- **Pre-commit/Hook**: `gitleaks` 로 시크릿/자격증명 하드코딩 감지 및 커밋 차단
- **외부 툴**: Snyk 또는 OWASP Dependency-Check 로 의존성 취약점 스캔

## 현재 상태
- [x] 스텁 (본 문서)
- [ ] 초안 작성
- [ ] 운영 중

## ADR 참조 요약 (추후 정의)

| ADR | 제목 | 요약 | 상세 |
|---|---|---|---|
| ADR-002 | 인증 방식 | 추후 정의 — `docs/api/adr.yaml` 확인 | [`docs/api/adr.yaml`](../api/adr.yaml) |
| ADR-005 | 환경변수 관리 | 추후 정의 — `docs/api/adr.yaml` 확인 | [`docs/api/adr.yaml`](../api/adr.yaml) |

## TODO (초안 작성 시 채워야 할 항목)
- [ ] JWT 토큰 만료 정책 (Access / Refresh 시간)
- [ ] CORS 허용 Origin 목록
- [ ] 환경변수 분류 기준 (Public / Secret / Per-env)
- [ ] 위협 모델 — 주요 공격 벡터 및 대응 전략
