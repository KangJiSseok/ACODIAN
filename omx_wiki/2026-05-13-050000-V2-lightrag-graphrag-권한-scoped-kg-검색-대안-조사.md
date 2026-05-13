---
title: "V2 LightRAG와 GraphRAG 권한 scoped KG 검색 대안 조사"
tags: ["lightrag", "graphrag", "qdrant", "neo4j", "authorization", "scoped-query", "kg", "worklog"]
created: 2026-05-13T05:00:00.000Z
updated: 2026-05-13T05:00:00.000Z
sources: ["https://github.com/HKUDS/LightRAG", "https://github.com/microsoft/graphrag", "https://qdrant.tech/documentation/search/filtering/"]
links: ["2026-05-13-031018-V2-lightrag-v2-scoped-kg-query-설계-인터뷰.md", "2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지.md", "2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입.md", "2026-05-12-080727-search-lightrag-병행-도입-구현-계획.md"]
category: decision
confidence: high
schemaVersion: 1
---

# V2 LightRAG와 GraphRAG 권한 scoped KG 검색 대안 조사

## 목적

이 문서는 2026-05-13 대화에서 수행한 다음 질문의 탐색/검증 결과를 보존한다.

> LightRAG를 fork하지 않고 PostgreSQL 권한 source of truth를 유지하면서, 사용자가 볼 수 있는 `worklog`만 대상으로 KG 기반 검색 정확도를 높일 수 있는가?

기존 v2 문서의 방향은 `PostgreSQL + LightRAG`에서 `allowedWorklogIds`를 query-time hard filter로 적용하기 위해 LightRAG 내부 query path 또는 `Scoped*Storage proxy`를 고려하는 것이었다. 사용자는 fork/vendor 방식의 upstream 충돌 위험을 우려했고, 이번 대화에서는 다음 대안을 재검토했다.

1. `PostgreSQL + LightRAG + Neo4j`
2. `Qdrant + LightRAG`
3. `Microsoft GraphRAG`

관련 기존 문서:

- [[2026-05-13-031018-V2-lightrag-v2-scoped-kg-query-설계-인터뷰]]
- [[2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지]]
- [[2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입]]
- [[2026-05-12-080727-search-lightrag-병행-도입-구현-계획]]

---

## AX-WMS의 핵심 요구사항

권한 모델은 PostgreSQL이 source of truth다.

```text
userId
→ PostgreSQL 권한 계산
→ allowedWorklogIds
→ retrieval / KG traversal / context / reference / LLM prompt 전체가 allowedWorklogIds 안에서만 동작
```

안전 조건:

```text
권한 없는 worklog의 chunk/entity/relation/description/reference는
검색 결과와 LLM 입력 어디에도 들어가면 안 된다.
```

따라서 단순 후필터링은 비채택이다.

```text
전체 corpus 검색
→ 결과 worklogId만 PostgreSQL로 후필터
```

이 방식은 다음 이유로 부적합하다.

- retrieval/ranking 단계에서 이미 권한 밖 문서가 영향을 준다.
- KG entity/relation/community summary가 권한 밖 문서를 요약한 정보를 포함할 수 있다.
- LLM context 또는 reference에 권한 밖 정보가 간접 반영될 수 있다.

---

## 조사/검증 방식

이번 대화에서는 탐색 전문 에이전트와 검증 에이전트를 분리했다.

- `PostgreSQL + LightRAG + Neo4j` 탐색
- `Qdrant + LightRAG` 탐색
- 로컬 wiki/문서 탐색
- LightRAG 결과 독립 검증
- Microsoft GraphRAG 탐색

검증 기준은 공식 문서, 공식 GitHub 소스, primary source를 우선했다.

확인 기준 주요 버전:

- LightRAG GitHub `main`: `5d738ae98f8644bd7939196527beed45686b98db` — 2026-05-12
- LightRAG latest release: `v1.4.16` — 2026-05-07
- Microsoft GraphRAG GitHub `main`: `0da2a4dd3e1402bd97a7ddd4e6cace8c87a55c8b` — 2026-04-13, release `v3.0.9`

---

## 1. PostgreSQL + LightRAG + Neo4j

### 최종 판정

```text
부분 가능 / stock LightRAG만으로는 비권장
```

가능한 것:

- LightRAG는 storage backend 분리 구조를 제공한다.
- 공식 지원 storage에 다음이 포함된다.
  - `PGKVStorage`
  - `PGVectorStorage`
  - `PGDocStatusStorage`
  - `Neo4JStorage`
- 따라서 PostgreSQL을 KV/vector/doc status로, Neo4j를 graph storage로 붙이는 구성 자체는 가능하다.

불가능하거나 부족한 것:

- stock LightRAG `QueryParam` / REST `QueryRequest`에는 다음 필드가 없다.
  - `ids`
  - `allowed_doc_ids`
  - `metadata_filter`
  - `where`
  - `file_path_filter`
  - ACL/scope filter
- `BaseVectorStorage.query()` signature도 `query`, `top_k`, `query_embedding`만 받는다.
- `PGVectorStorage.query()`는 `workspace`, cosine threshold, `top_k`, embedding 기준으로만 검색한다.
- `Neo4JStorage`는 workspace label 기반 격리는 제공하지만, `allowedWorklogIds`를 query-time subgraph filter로 받는 공식 경로가 없다.

즉 다음 흐름은 stock LightRAG API만으로는 성립하지 않는다.

```text
PostgreSQL allowedWorklogIds
→ LightRAG query-time KG/vector retrieval hard filter
```

### Neo4j 도입의 의미

Neo4j는 graph backend를 강화하는 선택이다. 그러나 권한 문제를 자동 해결하지 않는다.

```text
Neo4j 도입 = graph storage/운영/분석 성능 개선 가능성
Neo4j 도입 ≠ per-user/per-worklog ACL hard filter 제공
```

### fork 없이 가능한 경로

기존 v2의 B′ 방향과 동일하게, LightRAG 원본은 수정하지 않고 AX-WMS 쪽 adapter/proxy를 둔다.

```text
LightRAG
  - indexing
  - chunking
  - entity extraction
  - relationship extraction
  - KG 생성

AX-WMS v2 adapter
  - PostgreSQL allowedWorklogIds 계산
  - allowed chunk/entity/relation scope 산출
  - Scoped*Storage proxy 제공
  - unsafe mixed-provenance description 제거/재구성
  - LLM context/reference 안전성 보장
```

---

## 2. Qdrant + LightRAG

### 최종 판정

```text
부분 가능 / 동적 ACL 목적에는 비권장
```

가능한 것:

- LightRAG는 `QdrantVectorDBStorage`를 공식 지원한다.
- Qdrant 자체는 payload filter, metadata filter, `has_id` 기반 filtering을 지원한다.
- Qdrant를 직접 사용하면 `team_id`, `department_id`, `worklog_id`, `user_id` 등의 payload 조건으로 vector search를 제한할 수 있다.

불가능하거나 부족한 것:

- LightRAG stock query API가 Qdrant payload filter를 query-time으로 전달하지 않는다.
- LightRAG의 Qdrant implementation은 내부적으로 `workspace_id` filter만 고정 적용한다.
- Qdrant filter는 vector storage 계층에만 적용된다.
- LightRAG KG/hybrid 검색은 다음을 함께 사용한다.
  - `entities_vdb`
  - `relationships_vdb`
  - `chunks_vdb`
  - graph storage
  - KV text chunks
  - context builder
- 따라서 chunk vector 검색만 Qdrant filter로 제한해도 KG traversal, entity/relation lookup, context 구성 전체의 권한 hard filter가 보장되지 않는다.

### 가능한 구성안

#### A. workspace 단위 coarse 격리

```text
workspace = team / department / tenant
QdrantVectorDBStorage = workspace payload partition
```

가능한 경우:

- 권한 경계가 팀/부서처럼 고정되어 있다.
- 사용자가 하나의 workspace 또는 소수 workspace만 접근한다.

부적합한 경우:

- 사용자마다 `팀 A + 팀 B + 본인 작성 worklog + 관리자 grant`처럼 동적 조합이 생긴다.
- 권한 변경이 잦다.
- per-worklog ACL이 필요하다.

#### B. Qdrant 직접 검색 + 별도 RAG

```text
PostgreSQL allowedWorklogIds
→ Qdrant payload filter
→ allowed chunk vector search
→ 별도 LLM 응답
```

장점:

- hard filter 구현은 쉽다.

단점:

- LightRAG KG/hybrid 검색 장점은 거의 포기한다.
- 기존 pgvector 검색을 Qdrant로 바꾸는 것에 가깝다.

#### C. custom adapter / fork / upstream extension

LightRAG query pipeline에 scope filter를 넣어야 한다.

필요 변경 지점:

- `QueryParam`
- API `QueryRequest`
- `BaseVectorStorage.query()` signature
- `QdrantVectorDBStorage.query()`
- `entities_vdb`, `relationships_vdb`, `chunks_vdb` 호출부
- graph traversal 및 context builder
- query cache key에 `scopeDigest` 포함 또는 cache 비활성화

이는 fork 또는 내부 확장에 가깝다.

---

## 3. Microsoft GraphRAG

### 최종 판정

```text
Stock GraphRAG만 사용: 불가에 가까운 부분 가능
GraphRAG + AX-WMS 권한 gateway/custom wrapper: 조건부 가능
```

### stock GraphRAG만으로 부족한 이유

Microsoft GraphRAG 공식 query API는 대략 다음 artifact를 `DataFrame`으로 받는다.

```python
global_search(
    entities,
    communities,
    community_reports,
    ...
)

local_search(
    entities,
    communities,
    community_reports,
    text_units,
    relationships,
    covariates,
    ...
)

drift_search(
    entities,
    communities,
    community_reports,
    text_units,
    relationships,
    ...
)

basic_search(
    text_units,
    ...
)
```

그러나 공식 API/CLI에 다음 query-time hard filter 파라미터는 없다.

```text
allowedWorklogIds
allowed_document_ids
metadata_filter
acl_filter
text_unit_filter
where
```

따라서 PostgreSQL에서 계산한 `allowedWorklogIds`를 GraphRAG 기본 query에 그대로 전달하는 공식 경로는 없다.

### GraphRAG가 LightRAG보다 유리한 점

GraphRAG는 artifact가 `parquet`/`pandas.DataFrame` 중심이라, AX-WMS gateway에서 query 전에 scope를 제한하기 쉽다.

주요 artifact:

- `documents`
- `text_units`
- `entities`
- `relationships`
- `communities`
- `community_reports`
- `covariates`

예상 wrapper 흐름:

```text
PostgreSQL allowedWorklogIds
→ allowed documents/text_units
→ allowed entities
→ allowed relationships
→ allowed covariates
→ allowed communities/community_reports
→ GraphRAG local/basic search 호출
```

이 점은 LightRAG보다 유리하다. LightRAG는 내부 storage와 query pipeline에 더 강하게 결합되어 있어 `Scoped*Storage proxy` 같은 adapter가 필요하다.

### GraphRAG의 핵심 위험

단순히 아래처럼 `text_units`만 자르는 것은 안전하지 않다.

```text
text_units.document_id IN allowedWorklogIds
```

이유:

- `entities.description`은 여러 text unit/document에서 추출된 설명이 LLM으로 요약된 값일 수 있다.
- `relationships.description`도 여러 source에서 합쳐진 요약일 수 있다.
- `community_reports.full_content`, `summary`, `findings`는 community 전체를 LLM이 요약한 결과다.
- `covariates.description/source_text`도 권한 밖 text unit에서 온 claim일 수 있다.

따라서 전체 corpus에서 생성된 GraphRAG artifact를 나중에 일부 row만 자르는 것은 hard security boundary가 아니다.

### Search mode별 판단

| Mode | 주 사용 artifact | AX-WMS 적합성 |
|---|---|---|
| `basic` | `text_units`, text unit embeddings | 가장 제어 쉬움. 단, KG 장점은 약함 |
| `local` | `entities`, `relationships`, `community_reports`, `text_units`, `covariates` | 가능성 있음. provenance 정화 필요 |
| `global` | `community_reports`, `communities`, `entities` | 가장 위험. 전체 corpus report 사용 금지 |
| `DRIFT` | `community_reports` + local artifacts | global/local 양쪽 누수 위험 존재 |

### GraphRAG를 안전하게 쓰려면

필수 중 하나가 필요하다.

#### 1. scope별 index/community report 재생성

```text
scope별 allowed worklog set
→ text_units/entities/relationships 재생성
→ communities/community_reports 재생성
→ 해당 scope query에서만 사용
```

장점:

- 가장 명확하고 안전하다.

단점:

- scope가 사용자별로 많으면 비용/저장공간/동기화 부담이 크다.

#### 2. report ACL / provenance-limited artifact

각 artifact에 provenance를 명확히 둔다.

```text
entity.text_unit_ids
relationship.text_unit_ids
community.text_unit_ids
community_report.source_text_unit_ids 또는 source_document_ids
```

사용 조건:

```text
artifact provenance ⊆ allowedWorklogIds
```

이 조건을 만족하지 않는 entity/relationship/community_report는 그대로 쓰지 않는다.

#### 3. mixed-provenance description 재생성 또는 제거

LightRAG v2에서 정한 정책과 동일하다.

```text
If all source chunks are allowed:
  existing description/report may be used.

If mixed allowed/disallowed:
  do not use as-is.
  Rebuild from allowed evidence, or omit description/report.
```

---

## 최종 비교

| 항목 | LightRAG + Neo4j | LightRAG + Qdrant | Microsoft GraphRAG |
|---|---|---|---|
| 구성 자체 | 가능 | 가능 | 가능 |
| stock query-time ACL filter | 없음 | 없음 | 없음 |
| 권한 source of truth로 PostgreSQL 사용 | 가능 | 가능 | 가능 |
| fork 없이 wrapper 가능성 | 가능하나 내부 storage proxy 필요 | 가능하나 KG 전체 보장은 어려움 | DataFrame gateway로 상대적으로 쉬움 |
| KG/global summary 누수 위험 | entity/relation description mixed provenance 위험 | 동일 + vector-only filter 한계 | community_reports 때문에 더 큼 |
| per-user/per-worklog hard ACL | custom adapter 필요 | custom adapter 필요 | custom gateway/provenance artifacts 필요 |
| 가장 안전한 사용 모드 | Scoped*Storage proxy 기반 `kg_query` 재사용 | ACL-aware vector only 또는 custom adapter | `basic/local` with scope-safe artifacts |
| global/community summary 사용 | 주의 필요 | 주의 필요 | scope별 report 없으면 비권장 |

---

## 최종 결정 메모

### 1. Neo4j/Qdrant는 문제의 본질을 해결하지 않는다

Neo4j는 graph storage backend, Qdrant는 vector storage backend다. 둘 다 query-time per-worklog ACL hard filter를 LightRAG query pipeline 전체에 자동 주입하지 않는다.

### 2. GraphRAG는 더 다루기 쉽지만 자동 해결책은 아니다

GraphRAG는 artifact가 DataFrame으로 노출되어 있어 scope gateway를 만들기 쉽다. 그러나 stock GraphRAG query API에도 ACL filter는 없고, 특히 `community_reports`는 권한 누수 위험이 크다.

### 3. AX-WMS의 핵심은 storage 선택이 아니라 provenance-limited retrieval이다

안전한 구조의 핵심은 다음이다.

```text
PostgreSQL allowedWorklogIds
→ allowed source chunks/text_units
→ allowed entities/relationships only
→ mixed-provenance summary 제거/재생성
→ safe context/reference
→ LLM answer
```

### 4. 기존 v2 B′ 방향은 여전히 유효하다

현재 기준으로 가장 현실적인 방향은 다음이다.

```text
LightRAG는 KG 생성/indexing에 사용
AX-WMS는 PostgreSQL 권한 scope와 scoped retrieval/context safety를 책임
LightRAG 원본 fork/vendor는 fallback
기본 전략은 source-pinned dependency + Scoped*Storage proxy/custom adapter
```

GraphRAG를 선택한다면 다음으로 바뀐다.

```text
GraphRAG는 indexing/artifact 생성에 사용
AX-WMS는 PostgreSQL 권한 gateway에서 DataFrame/artifact scope를 제한
Global/DRIFT는 scope-safe community reports가 있을 때만 허용
```

---

## 참고 source links

### LightRAG

- LightRAG storage 지원 목록: https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/docs/ProgramingWithCore.md#L474-L513
- LightRAG `QueryParam`: https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/base.py#L85-L169
- `BaseVectorStorage.query()`: https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/base.py#L261-L272
- Qdrant implementation workspace filter: https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/kg/qdrant_impl.py#L697-L717
- PGVector query implementation: https://github.com/HKUDS/LightRAG/blob/5d738ae98f8644bd7939196527beed45686b98db/lightrag/kg/postgres_impl.py#L3487-L3516
- LightRAG doc-id filter 추가 PR: https://github.com/HKUDS/LightRAG/pull/1032
- LightRAG doc-id filter 제거 PR: https://github.com/HKUDS/LightRAG/pull/2025

### Qdrant

- Qdrant filtering: https://qdrant.tech/documentation/search/filtering/
- Qdrant payload concepts: https://qdrant.tech/documentation/concepts/payload/

### Microsoft GraphRAG

- Query API: https://microsoft-graphrag.mintlify.app/api/query
- Query overview: https://github.com/microsoft/graphrag/blob/main/docs/query/overview.md
- Inputs: https://microsoft.github.io/graphrag/index/inputs/
- Outputs: https://microsoft.github.io/graphrag/index/outputs/
- Dataflow: https://microsoft.github.io/graphrag/index/default_dataflow/
- Bring Your Own Graph: https://microsoft.github.io/graphrag/index/byog/
- GraphRAG API source: https://github.com/microsoft/graphrag/blob/main/packages/graphrag/graphrag/api/query.py
- GraphRAG vector store source: https://github.com/microsoft/graphrag/blob/main/packages/graphrag-vectors/graphrag_vectors/vector_store.py

