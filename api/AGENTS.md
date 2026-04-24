# AGENTS.md — API 전용 규칙

이 파일은 `api/` 디렉토리 아래 전체에 적용된다.

## 1. 수정 전 필수 읽기

`api/` 아래 파일을 수정하기 전에 반드시 먼저 읽는다.

1. `docs/api/adr.yaml`
2. `docs/api/code-convention.yaml`

읽기 전 코드 수정 금지.

## 2. 핵심 규칙

- Spring Boot 관련 설계 결정은 `docs/api/adr.yaml` 을 source of truth 로 따른다.
- 코드 스타일, 테스트 계층, DTO/Service/Repository 규칙은 `docs/api/code-convention.yaml` 을 source of truth 로 따른다.
- 요청 범위를 넘는 구조 변경, 패키지 이동, 리팩토링을 하지 않는다.
- Flyway 마이그레이션에 시드 데이터를 넣지 않는다.
- 로컬 시드는 `api/src/main/java/com/ibank/axwms/devsupport/bootstrap/LocalSeedRunner.java` 에만 추가한다.
