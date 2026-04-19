# 문서 구조 전략 (Documentation Structure Strategy)

## 현재 기준

- ADR의 단일 기준 파일은 `docs/api/adr.yaml`
- 컨벤션의 단일 기준 파일은 `docs/api/code-convention.yaml`
- 호환용 컨벤션 미러 파일은 유지하지 않는다

현재 구조:

```text
docs/
├── api/
│   ├── README.md
│   ├── adr.yaml
│   ├── code-convention.yaml
│   └── jooq-codegen-policy.md
├── shared/
├── sql/
└── docs-strategy.md
```

## 왜 단일 기준으로 유지하는가

- `AGENTS.md`와 Codex 스킬은 이미 `code-convention.yaml`을 기준으로 읽는다
- 기준 파일이 둘이면 문서 drift가 반복되고, 리뷰 시 어느 파일이 진짜 기준인지 계속 확인해야 한다
- 단일 source of truth가 있으면 문서 수정, 코드리뷰, 에이전트 로딩 경로를 모두 한 파일 기준으로 설명할 수 있다

## 소비 경로

- Codex / `AGENTS.md` / `.codex` 스킬: `docs/api/code-convention.yaml`
- 온보딩 문서: `docs/shared/onboarding.md`
- 정책 문서: `docs/api/jooq-codegen-policy.md`

README, onboarding, strategy 문서는 모두 `code-convention.yaml`을 직접 가리켜야 한다.

## 유지 규칙

- 컨벤션 수정은 `docs/api/code-convention.yaml` 한 파일만 수정한다
- ADR 변경과 컨벤션 연쇄 수정은 함께 검토한다
- 새 모듈(`web`, `ai`, `nginx`)이 활성화되면 각 모듈도 `code-convention.yaml` 네이밍으로 맞춘다

예시:

```text
docs/web/code-convention.yaml
docs/ai/code-convention.yaml
docs/infra/code-convention.yaml
```

## 분할 트리거

현재는 단일 파일 전략을 유지한다. 다만 아래 상황이면 카테고리 또는 모듈 기준 분할을 검토한다.

- `adr.yaml` 또는 `code-convention.yaml`이 40KB 이상으로 커질 때
- 한 파일이 1000줄 이상으로 커질 때
- `web`, `ai`, `nginx` 중 2개 이상이 활성화되어 독자 규칙이 생길 때

## 점검 체크리스트

- `AGENTS.md`가 실제 기준 파일 경로를 가리키는가
- README / onboarding / strategy 문서가 같은 경로를 가리키는가
- 단일 기준 파일이 아닌 오래된 경로를 참조하는 문서가 남아 있지 않은가

## 실무 원칙

- 기준 파일은 하나만 둔다
- 호환성 때문에 미러를 만들더라도 임시로만 두고, 가능한 빨리 참조를 통일한다
- 문서 구조보다 에이전트와 사람이 같은 기준을 읽는가를 우선한다
