# 데이터 사전 (Data Dictionary)

## 이 문서는 무엇인가
ERD 상의 모든 테이블/컬럼에 대한 비즈니스 의미를 정의하는 단일 진실 공급원(SSoT)이다.
컬럼명만으로는 알 수 없는 도메인 의미, 허용 값, 관계, 비고를 기록한다.
jOOQ 코드 생성 및 API 설계 시 이 문서를 기준으로 삼는다.

## 언제 읽어야 하는가 (Read trigger)
- 새 테이블 또는 컬럼을 추가하기 전
- `./gradlew jooqGenerate` 실행 전 스키마 의미 확인이 필요할 때
- 도메인 용어의 비즈니스 의미가 불명확하거나 혼동될 때
- 프론트엔드-백엔드 간 필드 매핑 협의 시

## 언제 추가/확장해야 하는가 (Write trigger)
- `docs/ax-wms-erd-draft-v2.sql` 기준으로 ERD가 확정될 때 초안 작성
- Flyway migration(`db/migration/V*.sql`)으로 스키마 변경이 발생할 때마다 동기화
- 컬럼 의미에 대한 팀 내 이견이 발생하여 합의가 필요할 때

## 관련 자동화 아이디어 (Skill / CI / Hook)
> 현재 구현되지 않음. 도입 시점에 아래 방향 검토.

- **스킬 후보**: `$sync-data-dict` — `docs/ax-wms-erd-draft-v2.sql` 파싱 후 사전에 누락된 컬럼 목록 리포트 생성
- **CI 연동**: Flyway migration 파일 변경 PR에서 data-dictionary.md 미수정 시 경고
- **Pre-commit/Hook**: `db/migration/` 경로 변경 감지 시 data-dictionary 동기화 알림
- **외부 툴**: SchemaSpy 또는 dbdocs.io로 시각화 연동 검토

## 현재 상태
- [x] 스텁 (본 문서)
- [ ] 초안 작성
- [ ] 운영 중

## 템플릿 / 예시 항목

| 테이블 | 컬럼 | 타입 | 비즈니스 의미 | 허용 값 / 비고 |
|---|---|---|---|---|
| `tb_user` | `user_id` | `BIGINT` | 사용자 고유 식별자 (시스템 내부 PK) | auto_increment, 외부 노출 금지 |
| `tb_user` | `login_id` | `VARCHAR(50)` | 사용자 로그인 ID (이메일 또는 사번 형식) | 추후 정의 |
| `tb_warehouse` | `warehouse_code` | `VARCHAR(20)` | 창고 코드 (사업장별 고유) | 추후 정의 — ERD v2 확정 후 보완 |
