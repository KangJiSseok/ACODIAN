---
title: "Session Log 2026-05-13 LightRAG v2 Scoped KG Query 설계 인터뷰"
tags: ["lightrag", "v2", "scoped-query", "kg", "authorization", "deep-interview", "session-log"]
created: 2026-05-13T03:10:18.314Z
updated: 2026-05-13T03:10:50.816Z
sources: []
links: ["2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지.md", "2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입.md", "2026-05-12-080727-search-lightrag-병행-도입-구현-계획.md"]
category: session-log
confidence: medium
schemaVersion: 1
---

# Session Log 2026-05-13 LightRAG v2 Scoped KG Query 설계 인터뷰

# Session Log: LightRAG v2 Scoped KG Query 설계 인터뷰 및 조사 기록

## Metadata

- Date: 2026-05-13
- Category: session-log
- Related wiki pages:
  - [LightRAG 전환 조사와 구성 선택지](2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지.md)
  - [LightRAG 전환 결정: 기존 pgvector 유지와 병행 도입](2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입.md)
  - [LightRAG 병행 도입 구현 계획](2026-05-12-080727-search-lightrag-병행-도입-구현-계획.md)
- Related deep-interview spec: `.omx/specs/deep-interview-lightrag-scoped-kg-query-v2.md`
- Related transcript: `.omx/interviews/lightrag-scoped-kg-query-v2-20260513T030704Z.md`
- Final ambiguity: 14%
- Final direction: **B′ — LightRAG KG 생성 유지 + Scoped*Storage proxy 기반 권한 scoped query**

## 0. 이 로그의 목적

이 페이지는 현재 세션에서 진행된 LightRAG v2 관련 질문, 조사, 판단, 응답을 순차적으로 보존한다. 특히 사용자가 던진 핵심 질문과 그에 대해 수행한 GitHub/Context7/코드 탐색, 선택 이유, 최종 응답을 함께 기록한다.

---

## 1. Endpoint versioning 및 `/light/vN` 격리 논의

### 사용자 질문/요구

사용자는 `$deep-interview`로 `2026-05-12-080727-search-lightrag-병행-도입-구현-계획.md` 문서를 대상으로 다음을 설명했다.

- `ai/app/router/search.py`가 첫 번째 endpoint다.
- 앞으로 `worklogs-v2`, `v3` 등 endpoint를 여러 개 만들어 endpoint 기반 versioning을 하려고 한다.
- 기존 `/app` 하위 계층이 다양하므로 LightRAG 관련 작업은 `/light` 계층 하나만 추가하고 싶다.
- 폴더는 `/ai/light/v2` 방식으로 만들고, URL은 A방식으로 간다.
- version마다 서로 참조하는 파일을 수정할 수 있으므로, Git revert를 쉽게 하기 위해 version별 폴더가 서로 연관되지 않게 분리하고 싶다.

### 조사/확인

로컬 코드에서 다음을 확인했다.

- `ai/app/router/search.py`
  - `router = APIRouter(prefix="/search", tags=["search"])`
  - POST `/worklogs` → 최종 URL `/ai/search/worklogs`
- `ai/app/main.py`
  - `app.include_router(api_router, prefix=settings.api_prefix)`
  - 기본 `settings.api_prefix = "/ai"`
- `ai/app/model/search.py`
  - 기존 v1 검색 request는 `keyword`, `teamId`, `teamStatus`, `allowedTeamIds`, pagination 등을 포함한다.
- `ai/app/service/search_service.py`
  - Gemini embedding → `EmbeddingStore.search_worklogs`
- `ai/app/store/embedding_store.py`
  - pgvector 기반 cosine search와 PostgreSQL filter를 수행한다.

### 판단/응답

- 기존 `/ai/search/worklogs`는 v1 compatibility endpoint로 유지한다.
- LightRAG 계열은 `/ai/light/worklogs-v2`, `/ai/light/worklogs-v3`처럼 endpoint suffix로 versioning한다.
- 파일 구조는 `ai/app/light/v2`, `ai/app/light/v3`처럼 version별로 격리한다.
- v2와 v3는 서로 import/참조하지 않는 것을 원칙으로 한다.

---

## 2. LightRAG 도입 방식 1차 정리: pgvector 미사용, Neo4j 미사용

### 사용자 질문/요구

사용자는 다시 `$deep-interview`로 LightRAG 구현 계획을 세우자고 했다.

첫 번째 방식:

- PostgreSQL에서 권한관리를 한다.
- pgvector를 사용하지 않는다.
- LightRAG에서 제공하는 storage를 사용한다.
- Neo4j도 아직 사용하지 않는다.

두 번째 방식:

- pgvector를 사용하면서 LightRAG storage도 사용한다.

사용자는 일단 첫 번째 방식으로 구현할 계획을 세우자고 했다.

### 판단/응답

초기 선택지는 대략 다음으로 정리했다.

- A안: PostgreSQL에서 권한 후보를 먼저 계산하고, LightRAG 검색을 그 범위 안으로 제한한다.
- B안: LightRAG 전체 검색 후 PostgreSQL이 나중에 필터링한다.
- C안: 사용자별 namespace/storage를 분리한다.

사용자는 C안에 대해 각 유저마다 파일/storage가 생성되면 비용이 너무 크다고 지적했고, C안은 제외했다.

이후 사용자는 A로 하겠다고 했고, 강한 방식과 완화 방식의 차이를 물었다.

### 핵심 설명

- 강한 방식:
  - PostgreSQL이 먼저 `allowedWorklogIds`를 계산한다.
  - LightRAG retrieval 자체가 그 후보 안에서만 수행된다.
  - retrieval 이전 hard constraint다.
- 완화 방식:
  - LightRAG가 전체 검색을 수행한다.
  - 나중에 PostgreSQL이 결과를 자른다.

사용자는 완화 방식이 결국 “LightRAG가 먼저 전체 검색으로 후보 worklogId를 반환하고, PostgreSQL이 나중에 권한/필터로 잘라낸다”와 같지 않느냐고 물었다. 이에 맞다고 답했고, 운영 구현에서는 후필터 방식은 허용하지 않는 방향으로 정리했다.

---

## 3. LightRAG 청킹/KG 생성 관련 질문

### 사용자 질문/요구

사용자는 LightRAG가 LLM을 통해 알아서 만들어준다고 들었다며, 그 방식과 성능이 채택 가능한지 확인해달라고 했다.

### 조사/응답 요약

- LightRAG의 `insert`/`ainsert`는 문서 청킹, entity/relation 추출, graph merge, vector indexing을 수행한다.
- 업무일지 1건을 LightRAG document 1건으로 넣고, `worklogId`를 LightRAG document id로 고정하는 방향이 가능하다고 설명했다.
- 다만 이 질문은 이후 사용자가 “아직 해당 질문으로 넘어갈 차례가 아니다”라고 정정했다.

---

## 4. Critical blocker: `allowedWorklogIds`를 어떻게 hard filter할 것인가

### 사용자 질문/요구

사용자는 ambiguity를 50%로 재설정하며, 가장 중요한 문제가 아직 해결되지 않았다고 했다.

핵심 문제:

```text
allowedWorklogIds를 어떻게 필터링해서 가져올지 정해야 한다.
query-time document id hard filter는 명확히 확인하지 못했다.
GraphRAG나 LightRAG 사용자들이 같은 문제를 어떻게 해결했는지 찾아야 한다.
```

사용자는 다음 서브에이전트 위임을 요청했다.

1. GraphRAG에서 권한/allowed document 문제를 어떻게 해결했는지 탐색.
2. 그 해결방안이 실제 가능한지 검증.
3. LightRAG에서 유사 문제/해결 사례를 탐색.
4. 그 해결방안이 AX-WMS에 적용 가능한지 검증.
5. GraphRAG 방식이 LightRAG에도 이식 가능한지 최종 검증.

### 수행한 위임/검증

- GraphRAG 탐색 agent: `Popper`
- LightRAG 탐색 agent: `Hooke`
- GraphRAG 검증 agent: `Darwin`
- LightRAG 검증 agent: `Leibniz`
- 최종 통합 검증 agent: `Maxwell`

### GraphRAG/RAG 일반 원칙 결론

- PostgreSQL이 권한 source of truth가 되어야 한다.
- retrieval 전에 authorization hard constraint가 적용되어야 한다.
- 후필터 방식은 recall 문제뿐 아니라 KG에서는 권한 없는 문서가 entity/community/summary/ranking에 영향을 준 뒤 제거될 수 있어 누수 위험이 있다.
- GraphRAG류에서는 provenance-limited traversal이 필요하다.
- summary/community report도 별도 ACL 객체처럼 취급해야 한다.

### LightRAG 기본 결론

- `QueryParam`, `QueryRequest`, `BaseVectorStorage.query()`에는 query-time doc-id/metadata hard filter public contract가 없었다.
- `insert(ids=...)`, `delete_by_doc_id`, `doc_status.chunks_list`, chunk `full_doc_id`, entity/relation `source_id`, `get_by_ids`, `get_vectors_by_ids`는 존재하지만, restricted semantic search public API는 아니다.
- LightRAG 기본 query를 운영 검색에 그대로 쓰면 권한 hard filter 요구사항을 충족하지 못한다.

---

## 5. KG가 필요한 이유 정리

### 사용자 질문/요구

사용자는 “KG가 뭐야?”라고 물었고, 이후 “KG까지 포함해야 할 것 같다. KG를 사용하지 않으면 일반 임베딩하고 유사도 기반 top-k를 뽑는 것과 똑같을 것 같다”고 했다.

### 응답/판단

KG는 Knowledge Graph다. LightRAG의 강점은 단순 chunk vector search가 아니라 entity/relation 기반 연결 구조를 활용한다는 점이다.

따라서 KG를 빼면:

- LightRAG를 도입하는 의미가 줄어든다.
- 기존 pgvector/embedding top-k와 차별성이 약해진다.

이후 방향은 “KG를 빼자”가 아니라 “LightRAG 기본 KG query는 권한 요구사항을 만족하지 못하므로, 권한 scope가 적용된 KG query 방식을 설계해야 한다”로 정정했다.

---

## 6. A/B/C 안 재정리

### A안 — AX-WMS 전용 authorized scoped KG retriever

- LightRAG가 만든 KG artifact는 사용한다.
- 하지만 LightRAG 기본 `query()`는 직접 쓰지 않는다.
- PostgreSQL `allowedWorklogIds` → allowed chunk/entity/relation scope → allowed context 구성.
- 장점: 보안 hard filter를 작게 구현, LightRAG core 수정 적음.
- 단점: traversal/ranking/context 구성을 많이 직접 설계해야 한다.

### B안 — LightRAG core/fork patch

- LightRAG query path 자체에 `allowedWorklogIds` 필터를 심는다.
- 장점: LightRAG 기본 `local/hybrid/mix` query에 가장 가깝다.
- 단점: 구현 범위가 크고 upstream 충돌 위험이 높다.

### C안 — 외부 filter-capable backend + ACL-aware KG

- Qdrant/Milvus/OpenSearch 등 filter 가능한 vector backend와 provenance-aware graph를 사용한다.
- 장점: 성능/운영 확장성.
- 단점: 사용자가 정한 1차 방식(pgvector/Neo4j 미사용, LightRAG storage 우선)과 멀다.

---

## 7. 사용자가 B안을 선호하고 최신 LightRAG 재조사를 요청

### 사용자 질문/요구

사용자는 B안이 좋을 것 같다고 했다.

이유:

- 직접 설계해야 하는 부분이 너무 크다.
- LightRAG는 KG를 알아서 만들어주는 것이 강점이다.
- 이 기능을 최대한 활용하고 싶다.

사용자는 `https://github.com/hkuds/lightrag`가 최신이라며 GitHub와 Context7를 자세히 조사하라고 했다.

핵심 요청:

```text
정말로 해결 방안은 없는지 확인하라.
query-time document id hard filter가 아니어도 된다.
내 문제를 해결할 수 있는 방안을 가져와라.
```

### Context7 조사

Context7에서 `/hkuds/lightrag`를 resolve했다.

확인 내용:

- `QueryParam` fields:
  - `mode`, `only_need_context`, `only_need_prompt`, `response_type`, `stream`, `top_k`, `chunk_top_k`, token limits, `hl_keywords`, `ll_keywords`, `conversation_history`, `model_func`, `user_prompt`, `enable_rerank`, `include_references` 등.
  - `ids`, `metadata_filter`, `where`, `file_path_filter`, `allowed_doc_ids`는 확인되지 않았다.
- workspace isolation:
  - workspace는 data isolation 용도다.
  - dynamic per-query ACL이 아니다.
- retrieval modes:
  - `mix`: KG + vector retrieval
  - `local`: entity-centric
  - `global`: relationship-pattern
  - `naive`: vector only
- custom KG insertion은 chunks/entities/relationships와 `source_id`를 지원한다.

참조:

- https://github.com/hkuds/lightrag/blob/main/docs/ProgramingWithCore.md
- https://github.com/hkuds/lightrag/blob/main/docs/LightRAG-API-Server.md

### GitHub 최신 조사

확인한 최신 main:

- commit: `5d738ae98f8644bd7939196527beed45686b98db`
- date: 2026-05-12
- PR: README typo fix

참조:

- https://github.com/HKUDS/LightRAG/commit/5d738ae98f8644bd7939196527beed45686b98db

확인한 핵심 파일:

- `lightrag/base.py`
  - `QueryParam`에 doc-id filter 없음.
  - `BaseVectorStorage.query(self, query, top_k, query_embedding=None)`만 있음.
  - `get_by_ids`, `get_vectors_by_ids`는 있음.
- `lightrag/api/routers/query_routes.py`
  - `QueryRequest`에도 `ids`나 filter 없음.
- `lightrag/lightrag.py`
  - storage wiring에서 `full_entities`, `full_relations`, `entity_chunks`, `relation_chunks` 확인.
  - vector meta fields: entity `source_id`, relation `source_id`, chunk `full_doc_id`.
- `lightrag/operate.py`
  - `kg_query`, `_build_query_context`, `_perform_kg_search`, `_get_vector_context` 등 query pipeline 확인.

참조:

- https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/base.py
- https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/lightrag.py
- https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/operate.py
- https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/api/routers/query_routes.py

### Issues/PR 조사

- Issue #2138:
  - OpenAPI 문서에 `ids`가 있는 것처럼 보이나 실제 API는 `QueryParam.__init__() got unexpected keyword argument 'ids'` 오류.
  - 사용자는 특정 document IDs 필터를 기대했다.
- Issue #2555:
  - `QueryParam`에서 제공된 ids 기능이 빠져 있다고 문제 제기.
- Issue #2276:
  - query endpoint에서 `file_path` filter feature request.
- PR #1032:
  - `QueryParam.ids`와 Postgres VDB doc-id filtering을 추가했던 PR.
- PR #2025:
  - doc-id based filtering이 제거됨.
  - 이유: entity/relationship은 여러 문서에서 aggregate되므로 단순 doc-id filter는 부정확하고 flawed.

참조:

- https://github.com/HKUDS/LightRAG/issues/2138
- https://github.com/HKUDS/LightRAG/issues/2555
- https://github.com/HKUDS/LightRAG/issues/2276
- https://github.com/HKUDS/LightRAG/pull/1032
- https://github.com/HKUDS/LightRAG/pull/2025

### 재조사 후 판단

단순 `QueryParam.ids` 방식은 해결책이 아니다. 실제로 LightRAG upstream에서 도입했다가 제거한 방식이다.

그러나 최신 LightRAG에는 다음 provenance structure가 있다.

- `full_entities`
- `full_relations`
- `entity_chunks`
- `relation_chunks`
- `doc_status.chunks_list`

이 구조를 활용하면 LightRAG의 KG 생성은 그대로 사용하면서, query 시점에 권한 scope를 강제하는 B′ 방식을 만들 수 있다.

---

## 8. B′ 방향 제안 및 확정

### 제안한 B′

B′는 다음을 의미한다.

```text
LightRAG KG generation은 그대로 사용한다.
LightRAG 기본 rag.query()는 운영 검색에 직접 쓰지 않는다.
AX-WMS /ai/light/v2 adapter/query layer에서 권한 scoped query를 구현한다.
```

핵심 흐름:

```text
PostgreSQL allowedWorklogIds
→ LightRAG doc_status에서 allowedChunkIds
→ full_entities/full_relations에서 후보 entity/relation
→ entity_chunks/relation_chunks로 provenance 검증
→ allowed scope 안에서 KG/vector query
→ allowed chunk만 LLM context/reference에 포함
```

### 왜 B′를 선택했는가

- A안은 보안은 좋지만 LightRAG query 기능을 너무 많이 직접 재구현하게 된다.
- B안의 full fork는 upstream update conflict 위험이 크다.
- C안은 pgvector/Neo4j/external backend 도입으로 현재 제약과 맞지 않는다.
- B′는 LightRAG KG 생성과 query pipeline을 최대한 활용하면서, 권한 hard filter 문제를 adapter/proxy로 해결하려는 절충안이다.

### 사용자 응답

사용자는 B′ 방향을 확정했다.

---

## 9. deep-interview 재활성화 및 ambiguity 산정

### 사용자 요청

사용자는 deep-interview가 종료된 것 같으니 모호도와 함께 다시 활성화하라고 했다.

### 응답/상태

- `deep-interview`를 다시 활성화했다.
- Profile: standard
- Threshold: 20%
- Current ambiguity: 42%
- Context snapshot: `.omx/context/lightrag-scoped-kg-query-20260513T024657Z.md`

남은 모호성:

- LightRAG를 dependency로 둘지 fork/vendor할지.
- source install과 vendor/fork의 차이.
- customQuery 기능 완성도.
- mixed-provenance description 정책.
- v2 구현 범위.
- phase 문서 위치.
- 완료 기준과 non-goals.

---

## 10. Round 1 — LightRAG 수정 경계

### 질문

`/ai/light/v2` 안에서 LightRAG를 외부 dependency로 유지하고 wrapper/query layer만 만들지, 아니면 LightRAG 소스를 v2 내부에 vendor/fork해서 query path를 직접 패치할지 물었다.

### 사용자 응답

사용자는 fork/vendor 방식은 나중에 LightRAG GitHub update를 받을 때 충돌이 나는 것 아니냐고 물었다.

### 응답/판단

맞다고 답했다.

예상 충돌 지점:

- `QueryParam`
- `operate.py`의 query path
- `lightrag.py` storage wiring
- entity/relation/chunk provenance 처리
- cache/query context builder

더 위험한 것은 textual conflict보다 semantic conflict라고 설명했다. upstream에서 query flow가 바뀌었는데 merge가 조용히 되면 권한 필터가 우회될 수 있다.

따라서 전체 fork/vendor는 비추천하고, 다음 구조를 제안했다.

```text
ai/app/light/v2/
  lightrag_adapter.py
  scoped_query_service.py
  scope_resolver.py
  scoped_vector_search.py
  context_builder.py
```

LightRAG는 dependency로 두고, v2 adapter가 내부 storage를 읽어 scoped retrieval을 수행하는 방식이다.

---

## 11. Round 2 — README의 Installation from Source 의미

### 사용자 질문

사용자는 README의 `Installation from Source` 방식으로 한다는 뜻인지 물었다.

### 조사/응답

LightRAG README에는 server source install과 core source install 흐름이 있다.

- 서버/WebUI/API까지 띄우는 방식:

```bash
git clone https://github.com/HKUDS/LightRAG.git
cd LightRAG
make dev
source .venv/bin/activate
lightrag-server
```

- core source install:

```bash
cd LightRAG
uv sync
pip install -e .
```

응답에서는 “source install에 가깝지만, vendor/fork와 다르다”고 설명했다.

정확한 방향:

```text
LightRAG는 source 기반/pinned dependency로 설치한다.
단, LightRAG 원본 소스는 직접 수정하지 않는다.
AX-WMS 권한 scoped query는 ai/app/light/v2 adapter layer에서 구현한다.
```

---

## 12. Round 3 — 정말 이 방식밖에 없는가?

### 사용자 질문

사용자는 다음처럼 확인했다.

```text
기본적으로 LightRAG에서 내가 해결하려고 하는 ids 기반 탐색이 안 된다.
그래서 소스코드를 수정해서 사용해야 한다.
이게 맞는가?
```

### 응답/판단

정확히는 “반만 맞다”고 답했다.

- 맞는 부분:
  - LightRAG 기본 `rag.query()` / `QueryParam`만으로 `allowedWorklogIds` 기반 권한 scoped KG query는 해결할 수 없다.
- 조정해야 하는 부분:
  - LightRAG 원본 파일을 직접 수정하는 것은 필수는 아니다.
  - source-pinned dependency로 두고, `/ai/light/v2` adapter가 LightRAG 내부 storage를 읽어 scoped query를 구현하는 방식이 가능하다.

최종 표현:

```text
LightRAG는 allowedWorklogIds 기반 권한 scoped KG query를 public API로 제공하지 않는다.
따라서 AX-WMS v2는 LightRAG 기본 query를 그대로 사용할 수 없다.
다만 원본 fork/vendor 대신 pinned source dependency + adapter/query layer를 우선한다.
원본 fork patch는 fallback으로만 둔다.
```

---

## 13. Round 4 — KG 생성은 LightRAG, query는 customQuery인가?

### 사용자 질문

사용자는 다음처럼 정리했다.

```text
KG를 만드는 건 LightRAG가 하는 게 맞고,
query를 받았을 때에는 custom해서 내가 만든 customQuery를 사용하도록 바꿔야 한다는 거지?
```

### 초기 응답

초기에는 다음처럼 답했다.

```text
Insert/Indexing 단계:
  LightRAG 사용
  - 문서 청킹
  - entity 추출
  - relation 추출
  - KG 생성
  - vector/KV/graph/doc_status 저장

Query 단계:
  LightRAG 기본 rag.query() 미사용
  - AX-WMS customQuery 사용
```

### 사용자 정정

사용자는 Round 4가 끝나지 않았다고 했다. 핵심 모호성은 다음이라고 했다.

```text
LightRAG query()를 쓰지 않고 내가 직접 구현했을 때,
기능 완성도가 LightRAG query()만큼 나오느냐?
```

### 추가 코드 확인

LightRAG `operate.py`를 다시 확인했다.

확인한 query pipeline:

- `kg_query`
- `get_keywords_from_query`
- `_build_query_context`
- `_perform_kg_search`
- `_get_vector_context`
- `_build_context_str`
- `naive_query`

LightRAG query는 단순 vector top-k가 아니라 다음을 수행한다.

```text
1. high-level / low-level keyword extraction
2. local/global/hybrid/mix/naive mode 처리
3. entity vector search
4. relationship vector search
5. chunk vector search
6. KG graph 확장
7. token truncation
8. vector chunk + entity-related chunk + relation-related chunk merge
9. context string 구성
10. reference/raw_data 구성
11. LLM prompt/response/cache/stream 처리
```

### 최종 응답/판단

처음부터 customQuery를 직접 만들면 LightRAG `query()`만큼 기능 완성도가 나오기 어렵다고 답했다.

따라서 customQuery의 의미를 바꿨다.

```text
customQuery = from-scratch query가 아니라,
LightRAG query pipeline을 최대한 재사용하되,
storage 접근만 Scoped*Storage proxy로 감싸는 방식.
```

제안한 구조:

```text
Original LightRAG query:
  kg_query(knowledge_graph_inst, entities_vdb, relationships_vdb, text_chunks_db, chunks_vdb)

v2 scoped query:
  kg_query(ScopedGraphStorage, ScopedEntitiesVectorStorage, ScopedRelationshipsVectorStorage, ScopedTextChunksStorage, ScopedChunksVectorStorage)
```

### 사용자 응답

사용자는 이 방향으로 잡자고 확정했다.

---

## 14. Round 5 — mixed-provenance entity/relation description 정책

### 질문

entity/relation이 여러 worklog에서 merge된 경우, v2에서는 보안을 우선해서 merged entity/relation description을 그대로 쓰지 않고, allowed chunk 기준으로 재구성하거나, 재구성이 어렵다면 description 없이 allowed chunk 근거만 쓰는 정책으로 갈지 물었다.

### 사용자 응답

사용자는 그렇게 가자고 했다.

### 확정 정책

```text
If all source chunks of an entity/relation are allowed:
  existing description may be used.

If source chunks are mixed between allowed and disallowed worklogs:
  do not use merged description as-is.
  First try to rebuild scoped description from allowed chunks/extraction cache.
  If safe rebuild is not practical, omit description and rely on allowed chunk evidence.
```

이 정책은 권한 없는 worklog가 entity/relation summary에 간접 반영되어 LLM prompt로 들어가는 것을 막기 위한 것이다.

---

## 15. Round 6 — v2 구현 범위와 phase 폴더

### 초기 질문

v2 첫 구현 범위를 query path에 집중하고 ingestion/update/delete는 skeleton 또는 수동 preloaded 기준으로 둘지 물었다.

### 사용자 정정

사용자는 아니라고 했다.

핵심:

```text
첫 구현은 기능이 완성되는 것까지로 본다.
최대한 phase별로 잘게 쪼갠다.
phase는 ai/v2/phase 같은 폴더를 만들어 각 작업해야 하는 것을 넣어둔다.
```

### 응답/정정

v2 첫 구현은 skeleton/PoC가 아니라 기능 완성까지라고 정정했다.

포함 범위:

```text
1. LightRAG source-pinned dependency 연결
2. worklog insert/indexing
3. LightRAG KG 생성
4. PostgreSQL 기반 allowedWorklogIds 확보
5. allowedWorklogIds → scoped chunks/entities/relations 변환
6. Scoped*Storage proxy 구현
7. LightRAG query pipeline 재사용
8. mixed-provenance description 재구성/제외
9. /ai/light/worklogs-v2 endpoint 완성
10. 권한 누수 방지 테스트
```

이후 phase 경로를 확인했고, 사용자는 다음으로 확정했다.

```text
ai/app/light/v2/phase/
```

---

## 16. Round 7 — v2 완료 기준

### 질문

v2 완료 기준을 다음으로 잡을지 물었다.

```text
실제 /ai/light/worklogs-v2 endpoint가
worklog indexing부터 권한 scoped KG query,
LLM 응답, reference 반환,
권한 누수 방지 테스트까지 통과하는 것.
```

### 사용자 응답

사용자는 맞다고 했다.

### 확정 완료 기준

```text
v2 완료 = /ai/light/worklogs-v2 endpoint가 실제로 동작하고,
worklog indexing → LightRAG KG 생성 → PostgreSQL 권한 scope → scoped KG query
→ LLM 응답 → reference 반환 → 권한 누수 방지 테스트까지 통과하는 것.
```

---

## 17. Round 8 — Non-goals 확정

### 질문

v2 기능 완성 범위에서 명시적으로 제외할 항목을 제시했다.

```text
1. LightRAG 원본 소스 fork/vendor 직접 수정
2. 사용자별 LightRAG storage/workspace 분리
3. LightRAG 기본 rag.query() 운영 검색 사용
4. 전체 검색 후 PostgreSQL post-filter 방식
5. Neo4j 도입
6. pgvector 도입
7. v3 이상의 endpoint 구현
8. API 서버/웹 호출부 전환까지 포함
```

### 사용자 응답

사용자는 맞다고 확인했다.

### 결과

Non-goals가 닫히면서 deep-interview final ambiguity가 14%로 내려갔다.

---

## 18. 생성된 deep-interview 산출물

다음 파일을 생성했다.

- Spec: `.omx/specs/deep-interview-lightrag-scoped-kg-query-v2.md`
- Transcript: `.omx/interviews/lightrag-scoped-kg-query-v2-20260513T030704Z.md`

Spec의 핵심 항목:

- Metadata
- Clarity breakdown
- Intent
- Desired Outcome
- Core Decision
- Architecture Direction
- Query Flow
- Mixed-Provenance Description Policy
- In Scope
- Out of Scope / Non-goals
- Decision Boundaries
- Constraints
- Acceptance Criteria
- Test Plan
- Assumptions Exposed and Resolved
- Evidence vs Inference Notes
- Condensed Transcript
- Recommended Handoff

추천 handoff:

```text
$ralplan .omx/specs/deep-interview-lightrag-scoped-kg-query-v2.md
```

---

## 19. 최종 결정 요약

### 최종 아키텍처 방향

```text
LightRAG:
  - indexing/chunking/entity extraction/relation extraction/KG generation 담당

AX-WMS v2:
  - PostgreSQL 권한 scope 계산
  - allowedWorklogIds → allowedChunkIds/entity/relation scope 변환
  - Scoped*Storage proxy 제공
  - LightRAG kg_query/mix query pipeline 최대 재사용
  - safe context/reference/LLM prompt 구성 보장
```

### 왜 이 선택을 했는가

- LightRAG 기본 `rag.query()`는 `allowedWorklogIds` 기반 hard filter를 제공하지 않는다.
- 단순 `ids` 기반 filter는 upstream에서 시도되었다가 제거된 방식이다.
- KG를 빼면 LightRAG 도입 의미가 약해진다.
- from-scratch customQuery는 LightRAG query 완성도를 따라가기 어렵다.
- full fork/vendor는 upstream update conflict와 semantic conflict 위험이 크다.
- Scoped*Storage proxy는 LightRAG query pipeline을 최대한 보존하면서 권한 scope를 강제할 수 있는 가장 균형 잡힌 방식이다.

### v2 non-goals

- pgvector 도입 없음.
- Neo4j 도입 없음.
- user별 workspace/storage 분리 없음.
- 전체 검색 후 post-filter 없음.
- LightRAG 원본 source 직접 fork/vendor 수정 없음.
- v3 구현 없음.
- API 서버/web 호출부 전환 없음.

---

## 20. 앞으로 이어질 때 기억해야 할 핵심 문장

```text
v2 customQuery는 직접 검색 알고리즘을 새로 만드는 것이 아니다.
LightRAG의 kg_query/mix query pipeline을 최대한 재사용하고,
storage 접근만 Scoped*Storage proxy로 감싸 권한 scope를 강제하는 것이다.
```

```text
LightRAG가 KG를 만들고,
AX-WMS가 PostgreSQL 권한과 scoped retrieval/context safety를 책임진다.
```

```text
권한 없는 worklog의 chunk/entity/relation/description/reference는 검색 결과와 LLM 입력 어디에도 들어가면 안 된다.
```

