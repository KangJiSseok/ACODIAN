# AGENTS.md — ai/app/graph GraphRAG 모듈 규칙

## 범위

- 이 디렉토리는 GraphRAG 전용 구현만 소유한다.
- LightRAG 구현은 `ai/app/light/**`에만 둔다.
- GraphRAG 관련 model/router/service/store/prompt/test는 가능하면 이 패키지 경계와 `ai/tests/test_graph_*`에 둔다.

## 모듈 역할

| 경로 | 역할 |
|---|---|
| `v1/model/` | GraphRAG 요청/응답 Pydantic contract. 입력 검증과 alias 정책만 둔다. |
| `v1/router/` | FastAPI HTTP contract. Service dependency 연결과 예외 매핑만 담당한다. |
| `v1/service/` | GraphRAG use case orchestration. source 조회, graph build, index/query 흐름을 조합한다. |
| `v1/store/` | PostgreSQL/Neo4j 접근 세부사항. SQLAlchemy query, Neo4j driver/session, parameterized Cypher를 소유한다. |
| `v1/prompt/` | GraphRAG 답변 생성용 프롬프트 문자열. LLM 호출 로직은 두지 않는다. |

## 반드시 지킬 것

- `app.light.*`를 import하지 않는다. LightRAG DTO, builder, adapter, store를 재사용하지 않는다.
- GraphRAG 전용 store는 `app/store/`에 새로 만들지 않고 `app/graph/v1/store/`에 둔다.
- Router는 DB/Neo4j/Gemini/driver에 직접 접근하지 않는다.
- Service는 raw Cypher 문자열과 DB driver lifecycle을 직접 소유하지 않는다. Cypher는 `store/`에 둔다.
- 비밀정보와 endpoint는 하드코딩하지 않고 `app/config/settings.py` + 환경변수로만 주입한다.
- GraphRAG 설정을 추가하면 `settings.py` 끝 줄 append, `.env.example` 동기화, 테스트용 Settings override를 함께 작성한다.
- Cypher/SQL은 사용자 입력 문자열 보간을 금지하고 parameter binding만 사용한다.
- 업무일지 원장 데이터는 PostgreSQL source of truth로 읽고, GraphRAG graph는 검색/추론용 파생 index로 취급한다.
- 권한/팀 scope 필터는 GraphRAG 결과를 그대로 신뢰하지 않고 PostgreSQL/API 기준 검증 가능성을 남긴다.

## 하면 안 되는 것

- `ai/app/light/**` 파일 수정 또는 LightRAG singleton 공유.
- GraphRAG 실험 코드를 `app/service/`, `app/store/`, `app/router/` 공용 경로에 분산 배치.
- `lightrag-hku` API 또는 `LightRAG` class를 GraphRAG adapter에서 호출.
- GraphRAG index/query route를 기존 `/light/*` prefix 아래에 노출.
- Neo4j 비밀번호, Gemini key, 운영 URL을 코드/테스트 fixture에 실제 값으로 기록.
- 장시간 전체 재색인을 동기 HTTP 요청 하나에서 무제한 수행.
- 테스트 없이 router contract, response_model, Cypher parameter contract를 변경.
