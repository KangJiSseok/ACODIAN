# AGENTS.md — Infra 전용 규칙

이 파일은 `infra/` 디렉토리 아래 전체에 적용된다.

## 1. 수정 전 필수 읽기

`infra/` 아래 파일을 수정하기 전에 반드시 먼저 읽는다.

1. `docs/infra/adr.yaml`
2. `docs/infra/code-convention.yaml`

읽기 전 코드 수정 금지.

## 2. 핵심 규칙

- 운영/배포/compose/GitLab CI/CD 관련 결정은 `docs/infra/adr.yaml` 을 source of truth 로 따른다.
- 인프라 코드/문서 규칙은 `docs/infra/code-convention.yaml` 을 source of truth 로 따른다.
- 민감정보(DB 비밀번호, JWT secret, SSH 키, 토큰 등)를 실제 값으로 커밋하지 않는다.
- 배포 source of truth 는 문서와 compose 규칙을 함께 맞춘다. 관련 파일 하나만 고치고 나머지를 방치하지 않는다.
- 요청 범위를 넘는 운영 구조 변경, 브랜치 정책 변경, 배포 전략 변경은 하지 않는다.
